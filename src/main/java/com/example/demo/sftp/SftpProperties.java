package com.example.demo.sftp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {

	private boolean enable;
	private boolean setAllowUnknownKeys;
	private Duration pollerInterval = Duration.ofMinutes(1);
	private Map<String, User> users = new LinkedHashMap<>();

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public boolean isSetAllowUnknownKeys() {
		return setAllowUnknownKeys;
	}

	public void setSetAllowUnknownKeys(boolean setAllowUnknownKeys) {
		this.setAllowUnknownKeys = setAllowUnknownKeys;
	}

	public Duration getPollerInterval() {
		return pollerInterval;
	}

	public void setPollerInterval(Duration pollerInterval) {
		this.pollerInterval = pollerInterval;
	}

	public Map<String, User> getUsers() {
		return users;
	}

	public void setUsers(Map<String, User> users) {
		this.users = users;
	}

	public static class User {

		private String host;
		private int port = 22;
		private Resource privateKeyPath;
		private String password;
		private Kind kind;
		private List<AcceptedFile> acceptFiles = new ArrayList<>();
		private List<String> directories = new ArrayList<>();
		private List<String> backupDirectories = new ArrayList<>();

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public Resource getPrivateKeyPath() {
			return privateKeyPath;
		}

		public void setPrivateKeyPath(Resource privateKeyPath) {
			this.privateKeyPath = privateKeyPath;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public Kind getKind() {
			return kind;
		}

		public void setKind(Kind kind) {
			this.kind = kind;
		}

		public List<AcceptedFile> getAcceptFiles() {
			return acceptFiles;
		}

		public void setAcceptFiles(List<AcceptedFile> acceptFiles) {
			this.acceptFiles = acceptFiles;
		}

		public List<String> getDirectories() {
			return directories;
		}

		public void setDirectories(List<String> directories) {
			this.directories = directories;
		}

		public List<String> getBackupDirectories() {
			return backupDirectories;
		}

		public void setBackupDirectories(List<String> backupDirectories) {
			this.backupDirectories = backupDirectories;
		}

		public enum Kind {
			SEPA_SCT,
			ELIXIR
		}
	}

	public static class AcceptedFile {

		private Type type;
		private String regexp;

		public Type getType() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public String getRegexp() {
			return regexp;
		}

		public void setRegexp(String regexp) {
			this.regexp = regexp;
		}

		public enum Type {
			OUT_SCT_MESSAGE,
			REJ_BIF_MESSAGE
		}
	}
}
