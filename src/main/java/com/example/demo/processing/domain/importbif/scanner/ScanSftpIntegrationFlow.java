package com.example.demo.processing.domain.importbif.scanner;

import com.example.demo.processing.domain.common.model.BifKind;
import com.example.demo.processing.domain.common.model.BifStatus;
import com.example.demo.processing.domain.common.model.BifType;
import com.example.demo.processing.domain.importbif.SftpProperties;
import com.example.demo.processing.domain.importbif.model.BifMetadataEntity;
import com.example.demo.processing.domain.importbif.model.ImportBif;
import com.example.demo.processing.domain.importbif.scanner.filter.OnlyFilesFilter;
import com.example.demo.processing.domain.importbif.scanner.filter.SftpKirFileFilter;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.DefaultSessionFactoryLocator;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.advice.ContextHolderRequestHandlerAdvice;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.dsl.SftpOutboundGatewaySpec;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(SftpProperties.class)
@RequiredArgsConstructor
@Slf4j
public class ScanSftpIntegrationFlow {

    public static final String SFTP_POLLER_ID = "sftpPollerId";

    private final SftpProperties properties;
    private final SeenFileHandler handler;

    private static final String BIF_INFO_CHANNEL = "bifInfoChannel";
    private static final String ERROR_CHANNEL = "errorChannel";

    @Bean
    public IntegrationFlow saveBifInfo() {
        return IntegrationFlow.from(BIF_INFO_CHANNEL).handle(this::saveBifsInfo).get();
    }


    private void saveBifsInfo(Message<?> message) {
        SftpFileInfo fileInfo = (SftpFileInfo) message.getPayload();
        try {
            var memberName  = message.getHeaders().get("user", String.class);
            var path = message.getHeaders().get("path", String.class);
            var kind = properties.getUsers().get(memberName).getKind();

            var acceptedFilesRegexes = properties.getAcceptedFilesPatternsForUser(memberName);
            ImportBif importBif = new ImportBif();
            handler.save(importBif);
        } catch (Exception e) {
            log.error("Error while saving bifs info", e);
        }
    }


    @Bean
    public IntegrationFlow readBifInfo(
            DelegatingSessionFactory<SftpClient.DirEntry> delegatingSessionFactory,
            Advice sftpCleanupAdvice,
            SftpKirFileFilter fileFilter,
            OnlyFilesFilter onlyFilesFilter
    ) {
        return IntegrationFlow.fromSupplier((

                ) -> properties.getUsers().keySet(), poller()
                ).split()
                .enrichHeaders(h -> h.headerExpression("user", "payload"))
                .split()
                .enrichHeaders(h -> h.headerExpression("path", "payload"))
                .handle(sftpOutboundGatewaySpec(delegatingSessionFactory, onlyFilesFilter),
                        e -> e.advice(sftpCleanupAdvice))
                .split()
                .filter(Message.class, fileFilter::accept)
                .channel(BIF_INFO_CHANNEL)
                .get();
    }

    private Consumer<SourcePollingChannelAdapterSpec> poller() {
        return e -> e.poller(
                Pollers.fixedDelay(
                        properties.getPollerInterval()).errorChannel(ERROR_CHANNEL)).id(SFTP_POLLER_ID);
    }

    private static SftpOutboundGatewaySpec sftpOutboundGatewaySpec(
            DelegatingSessionFactory<SftpClient.DirEntry> delegatingSftpSessionFactory,
            OnlyFilesFilter onlyFilesFilter
    ) {
       return Sftp.outboundGateway(delegatingSftpSessionFactory, AbstractRemoteFileOutboundGateway.Command.LS, "headers['path']")
               .filter(onlyFilesFilter)        ;
    }

    @Bean
    public IntegrationFlow readBifInfoErrorChannel(

    ) {
        return IntegrationFlow.from(ERROR_CHANNEL)
                .handle(ErrorMessage.class, (message, headers) -> {
                    Throwable exception = message.getPayload();
                    return null;
                }).get();
    }

    @Bean
    public ContextHolderRequestHandlerAdvice sftpCleanupAdvice(
            DelegatingSessionFactory<SftpClient.DirEntry> delegatingSftpSessionFactory) {
        return new ContextHolderRequestHandlerAdvice(
                message -> Objects.requireNonNull(message.getHeaders().get("user", String.class)),
                        delegatingSftpSessionFactory::setThreadKey,
                        delegatingSftpSessionFactory::clearThreadKey);
    }

    @Bean
    public DelegatingSessionFactory<SftpClient.DirEntry> delegatingSessionFactory() {
        Map<Object, SessionFactory<SftpClient.DirEntry>> factories = new HashMap<>();
        var members = properties.getUsers().entrySet();
        for (Map.Entry<String, SftpProperties.User> member : members) {
            String memberName = member.getKey();

            CachingSessionFactory<SftpClient.DirEntry> factory = createSessionFactory(member, properties.isSetAllowUnknownKeys());

            factories.put(memberName, factory);
        }

        return new DelegatingSessionFactory<>(new DefaultSessionFactoryLocator<>(factories));

    }

    private CachingSessionFactory<SftpClient.DirEntry> createSessionFactory(
            Map.Entry<String, SftpProperties.User> userPropertiesEntry, boolean allowUnknownKeys) {

        var username = userPropertiesEntry.getKey();
        var userProperties = userPropertiesEntry.getValue();

        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(userProperties.getHost());
        factory.setPort(userProperties.getPort());
        factory.setUser(username);
        factory.setPrivateKey(userProperties.getPrivateKeyPath());
        factory.setAllowUnknownKeys(allowUnknownKeys);

        var cachingSessionFactory = new CachingSessionFactory<>(factory);
        cachingSessionFactory.setPoolSize(properties.getPoolSizePerUser());
        cachingSessionFactory.setSessionWaitTimeout(properties.getSessionsWaitTimeout());
        return cachingSessionFactory;
    }


}
