package com.example.demo.processing.domain.importbif.scanner.filter;

import com.example.demo.processing.domain.importbif.SftpProperties;
import com.example.demo.processing.domain.importbif.scanner.SeenFileHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SftpKirFileFilter implements GenericSelector<Message<SftpFileInfo>> {

    private final SftpProperties properties;
    private final SeenFileHandler handler;

    @Override
    public boolean accept(Message<SftpFileInfo> message) {
        SftpFileInfo fileInfo = message.getPayload();
        String member = message.getHeaders().get("user", String.class);

         var acceptedFilesPatternsForUser = properties.getAcceptedFilesPatternsForUser(member);

         return handler.matchesAnyOfAcceptedRegexes(acceptedFilesPatternsForUser, fileInfo) && handler.doesNotExist(member, fileInfo.getFilename());
    }
}
