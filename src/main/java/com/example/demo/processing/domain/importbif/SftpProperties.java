package com.example.demo.processing.domain.importbif;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "sftp")
@Data
public class SftpProperties {

    private boolean enable;
    private boolean setAllowUnknownKeys;
    private int poolSizePerUser;
    private long sessionsWaitTimeout;
    private long pollerInterval;
    private Map<String, User> users;

    @Data
    public static class User {
        private String host;
        private int port;
        private Resource privateKeyPath;
        private Kind kind;
        private List<AcceptedFile> acceptFiles;
        private List<String> directories;

        public enum Kind {
            SEPA_SCT,
            ELIXIR
        }
    }

    @Data
    public static class AcceptedFile {
        private Type type;
        private String regexp;

        public enum Type {
            OUT_SCT_MESSAGE,
            REJ_BIF_MESSAGE
        }
    }

    public List<String> getAcceptedFilesPatternsForUser(String username) {
        return users.get(username).acceptFiles.stream().map(SftpProperties.AcceptedFile::getRegexp).toList();
    }
}
