package com.example.demo.processing.domain.importbif.config;

import com.example.demo.processing.domain.importbif.SftpProperties;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.DefaultSessionFactoryLocator;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(SftpProperties.class)
@RequiredArgsConstructor
public class SftpSessionFactoryConfiguration {

    private final SftpProperties properties;

    @Bean
    public DelegatingSessionFactory<SftpClient.DirEntry> delegatingSessionFactory() {
        Map<Object, SessionFactory<SftpClient.DirEntry>> factories = new HashMap<>();

        for (Map.Entry<String, SftpProperties.User> member : properties.getUsers().entrySet()) {
            String memberName = member.getKey();
            factories.put(memberName, createSessionFactory(member, properties.isSetAllowUnknownKeys()));
        }

        return new DelegatingSessionFactory<>(new DefaultSessionFactoryLocator<>(factories));
    }

    @Bean
    public RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate(
            DelegatingSessionFactory<SftpClient.DirEntry> delegatingSessionFactory
    ) {
        return new RemoteFileTemplate<>(delegatingSessionFactory);
    }

    private CachingSessionFactory<SftpClient.DirEntry> createSessionFactory(
            Map.Entry<String, SftpProperties.User> userPropertiesEntry,
            boolean allowUnknownKeys
    ) {
        String username = userPropertiesEntry.getKey();
        SftpProperties.User userProperties = userPropertiesEntry.getValue();

        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(userProperties.getHost());
        factory.setPort(userProperties.getPort());
        factory.setUser(username);
        factory.setPrivateKey(userProperties.getPrivateKeyPath());
        factory.setAllowUnknownKeys(allowUnknownKeys);

        CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory = new CachingSessionFactory<>(factory);
        cachingSessionFactory.setPoolSize(properties.getPoolSizePerUser());
        cachingSessionFactory.setSessionWaitTimeout(properties.getSessionsWaitTimeout());
        return cachingSessionFactory;
    }
}
