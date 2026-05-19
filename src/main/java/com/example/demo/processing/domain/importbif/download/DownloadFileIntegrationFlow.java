package com.example.demo.processing.domain.importbif.download;

import com.example.demo.processing.domain.importbif.SftpProperties;
import com.example.demo.processing.domain.importbif.model.ImportBif;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.messaging.support.ErrorMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Configuration
@ConditionalOnProperty(prefix = "sftp", name = "enable", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DownloadFileIntegrationFlow {

    private static final String ERROR_CHANNEL = "errorChannel";
    private static final String DOWNLOAD_REQUEST_CHANNEL = "bifFileContentDownloadRequestChannel";

    private final SftpProperties properties;
    private final DownloadFileHandler downloadFileHandler;

    @Bean
    public IntegrationFlow prepareBifFileContentDownload() {
        return IntegrationFlow
                .fromSupplier(downloadFileHandler::findNewFilesToDownload,
                        endpoint -> endpoint.poller(Pollers.fixedDelay(properties.getPollerInterval())
                                .errorChannel(ERROR_CHANNEL)))
                .split()
                .channel(DOWNLOAD_REQUEST_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow downloadBifFileContentFromSftp(
            DelegatingSessionFactory<SftpClient.DirEntry> delegatingSessionFactory,
            RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate
    ) {
        return IntegrationFlow.from(DOWNLOAD_REQUEST_CHANNEL)
                .handle(ImportBif.class, (importBif, headers) -> {
                    downloadAndSave(importBif, delegatingSessionFactory, remoteFileTemplate);
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow sftpErrorChannel(DownloadFileErrorHandler downloadFileErrorHandler) {
        return IntegrationFlow.from(ERROR_CHANNEL)
                .handle(ErrorMessage.class, (message, headers) -> {
                    downloadFileErrorHandler.handle(message);
                    return null;
                })
                .get();
    }

    private void downloadAndSave(
            ImportBif importBif,
            DelegatingSessionFactory<SftpClient.DirEntry> delegatingSessionFactory,
            RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate
    ) {
        List<String> directories = configuredDirectoriesFor(importBif);
        if (directories.isEmpty()) {
            log.error("No SFTP directories configured for memberId {}", importBif.getMemberId());
            return;
        }

        RuntimeException lastFailure = null;

        for (String directory : directories) {
            try {
                Optional<String> content = download(
                        importBif,
                        directory,
                        delegatingSessionFactory,
                        remoteFileTemplate
                );
                if (content.isPresent()) {
                    downloadFileHandler.saveDownloadedContent(importBif, content.get());
                    return;
                }
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn(
                        "Cannot download file {} for importBif {} from {}",
                        importBif.getFileName(),
                        importBif.getId(),
                        directory,
                        e
                );
            }
        }

        log.error(
                "Cannot download file {} for importBif {} from configured directories {}",
                importBif.getFileName(),
                importBif.getId(),
                directories,
                lastFailure
        );
    }

    private List<String> configuredDirectoriesFor(ImportBif importBif) {
        SftpProperties.User user = properties.getUsers().get(importBif.getMemberId());
        if (user == null || user.getDirectories() == null) {
            return List.of();
        }

        LinkedHashSet<String> directories = new LinkedHashSet<>();
        user.getDirectories().stream()
                .filter(directory -> directory != null && !directory.isBlank())
                .forEach(directories::add);
        return List.copyOf(directories);
    }

    private Optional<String> download(
            ImportBif importBif,
            String directory,
            DelegatingSessionFactory<SftpClient.DirEntry> delegatingSessionFactory,
            RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate
    ) {
        delegatingSessionFactory.setThreadKey(importBif.getMemberId());
        try {
            String[] content = new String[1];
            boolean downloaded = remoteFileTemplate.get(
                    remotePath(directory, importBif.getFileName()),
                    remoteStream -> content[0] = readContent(remoteStream)
            );
            return downloaded ? Optional.ofNullable(content[0]) : Optional.empty();
        } finally {
            delegatingSessionFactory.clearThreadKey();
        }
    }

    private String remotePath(String directory, String fileName) {
        if (directory.endsWith("/")) {
            return directory + fileName;
        }
        return directory + "/" + fileName;
    }

    private String readContent(InputStream remoteStream) throws IOException {
        try (Reader reader = new InputStreamReader(remoteStream, StandardCharsets.UTF_8)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
            return content.toString();
        }
    }
}
