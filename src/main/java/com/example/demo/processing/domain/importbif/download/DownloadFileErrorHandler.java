package com.example.demo.processing.domain.importbif.download;

import com.example.demo.processing.domain.importbif.SftpIntegrationHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DownloadFileErrorHandler {

    public void handle(ErrorMessage message) {
        Message<?> failedMessage = failedMessage(message.getPayload());
        if (failedMessage == null) {
            log.error("Error while processing SFTP flow", message.getPayload());
            return;
        }

        String errorSource = failedMessage.getHeaders().get(SftpIntegrationHeaders.ERROR_SOURCE, String.class);
        if (SftpIntegrationHeaders.ERROR_SOURCE_SCAN.equals(errorSource)) {
            log.error("Error while scanning SFTP directories", message.getPayload());
            return;
        }
        if (SftpIntegrationHeaders.ERROR_SOURCE_DOWNLOAD_CONTENT.equals(errorSource)) {
            log.error("Error while downloading or saving SFTP file content", message.getPayload());
            return;
        }

        log.error("Error while processing SFTP flow, source={}", errorSource, message.getPayload());
    }

    private Message<?> failedMessage(Throwable throwable) {
        if (throwable instanceof MessagingException messagingException) {
            return messagingException.getFailedMessage();
        }
        return null;
    }
}
