package com.example.demo.processing.domain.importbif;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@ConfigurationProperties(prefix = "sftp")
@Data
public class SftpProperties {

    private boolean enable;
    private boolean setAllowUnknownKeys;
    private int poolSizePerUser;
    private long seesionsWaitTimeout;
    private long pollerInterval;
    private Map<String, User> users;

    @Data
    public static class User {}
}
