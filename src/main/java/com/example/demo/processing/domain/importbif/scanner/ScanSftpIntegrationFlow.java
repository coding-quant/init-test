package com.example.demo.processing.domain.importbif.scanner;

import com.example.demo.processing.domain.importbif.SftpIntegrationHeaders;
import com.example.demo.processing.domain.importbif.SftpProperties;
import com.example.demo.processing.domain.importbif.model.ImportBif;
import com.example.demo.processing.domain.importbif.scanner.filter.OnlyFilesFilter;
import com.example.demo.processing.domain.importbif.scanner.filter.SftpKirFileFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.handler.advice.ContextHolderRequestHandlerAdvice;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.dsl.SftpOutboundGatewaySpec;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.messaging.Message;

import java.util.Objects;
import java.util.function.Consumer;

@Configuration
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
                .enrichHeaders(h -> h
                        .headerExpression(SftpIntegrationHeaders.USER, "payload")
                        .header(SftpIntegrationHeaders.ERROR_SOURCE, SftpIntegrationHeaders.ERROR_SOURCE_SCAN))
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
    public ContextHolderRequestHandlerAdvice sftpCleanupAdvice(
            DelegatingSessionFactory<SftpClient.DirEntry> delegatingSftpSessionFactory) {
        return new ContextHolderRequestHandlerAdvice(
                message -> Objects.requireNonNull(message.getHeaders().get(SftpIntegrationHeaders.USER, String.class)),
                        delegatingSftpSessionFactory::setThreadKey,
                        delegatingSftpSessionFactory::clearThreadKey);
    }

}
