package com.example.demo.processing.domain.importbif.scanner;

import com.example.demo.processing.domain.importbif.SftpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties(SftpProperties.class)
@RequiredArgsConstructor
Slf4j
public class ScanSftpIntegrationFlow {

    public static final String SFTP_POLLER_ID = "sftpPollerId";

    private final SftpProperties properties;
}
