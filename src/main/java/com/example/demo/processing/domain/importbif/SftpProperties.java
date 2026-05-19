package com.example.demo.processing.domain.importbif;

import com.example.demo.processing.domain.common.model.BifKind;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "sftp")
@Data
public class SftpProperties {

    private boolean enable;
    private boolean setAllowUnknownKeys;
    private int poolSizePerUser = 1;
    private long sessionsWaitTimeout = 5000;
    private Duration pollerInterval = Duration.ofMinutes(1);
    private Map<String, User> users = new HashMap<>();

    @Data
    public static class User {
        private String host;
        private int port;
        private Resource privateKeyPath;
        private BifKind kind;
        private List<AcceptedFile> acceptFiles = List.of();
        private List<String> directories = List.of();
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
        User user = users.get(username);
        if (user == null) {
            return List.of();
        }
        return user.acceptFiles.stream()
                .map(SftpProperties.AcceptedFile::getRegexp)
                .toList();
    }
}
