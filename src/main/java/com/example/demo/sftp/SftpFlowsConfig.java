package com.example.demo.sftp;

import static com.example.demo.sftp.model.BifStatus.NEW;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aopalliance.aop.Advice;
import org.apache.sshd.sftp.client.SftpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.DefaultSessionFactoryLocator;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.advice.ContextHolderRequestHandlerAdvice;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.messaging.Message;

import com.example.demo.sftp.model.BifFileCandidate;
import com.example.demo.sftp.model.BifFileContent;
import com.example.demo.sftp.model.BifKind;
import com.example.demo.sftp.model.BifMetadata;
import com.example.demo.sftp.model.BifType;

@Configuration
@EnableConfigurationProperties(SftpProperties.class)
public class SftpFlowsConfig {

	public static final String SFTP_POLLER_ID = "sftpPollerId";
	private static final String USER_HEADER = "user";
	private static final String PATH_HEADER = "path";
	private static final int POOL_SIZE_PER_USER = 1;
	private static final long SESSIONS_WAIT_TIMEOUT = 5_000;

	private static final Logger log = LoggerFactory.getLogger(SftpFlowsConfig.class);

	private final SftpProperties properties;
	private final BifService service;
	private final TimeProvider timeProvider;

	public SftpFlowsConfig(SftpProperties properties, BifService service, TimeProvider timeProvider) {
		this.properties = properties;
		this.service = service;
		this.timeProvider = timeProvider;
	}

	@Bean
	@ConditionalOnProperty(name = "sftp.enable", havingValue = "true")
	public IntegrationFlow readSftpFiles(
			DelegatingSessionFactory<SftpClient.DirEntry> delegatingSftpSessionFactory,
			Advice sftpCleanupAdvice) {

		return IntegrationFlow.fromSupplier(
						() -> properties.getUsers().keySet(),
						e -> e.poller(Pollers.fixedDelay(properties.getPollerInterval())))
				.split()
				.enrichHeaders(h -> h.headerExpression(USER_HEADER, "payload"))
				.<String, List<String>>transform(user -> properties.getUsers().get(user).getDirectories())
				.split()
				.enrichHeaders(h -> h.headerExpression(PATH_HEADER, "payload"))
				.handle(
						Sftp.outboundGateway(
										delegatingSftpSessionFactory,
										AbstractRemoteFileOutboundGateway.Command.LS,
										"headers['" + PATH_HEADER + "']")
								.filter(new AcceptAllFileListFilter<>()),
						e -> e.advice(sftpCleanupAdvice))
				.handle(this::registerListedFiles)
				.get();
	}

	@Bean
	@ConditionalOnProperty(name = "sftp.enable", havingValue = "true")
	public IntegrationFlow downloadPendingBifFiles(
			DelegatingSessionFactory<SftpClient.DirEntry> delegatingSftpSessionFactory,
			Advice sftpCleanupAdvice) {

		return IntegrationFlow.fromSupplier(
						// Tu flow pobierania startuje od bazy: bierzemy zapisane nazwy BIF ze statusem NEW.
						service::findNewBifsToDownload,
						e -> e.poller(Pollers.fixedDelay(properties.getPollerInterval())))
				.split()
				.handle(
						Sftp.outboundGateway(delegatingSftpSessionFactory, this::downloadBifFile),
						e -> e.advice(sftpCleanupAdvice))
				.handle(this::saveDownloadedBifFile)
				.get();
	}

	@Bean
	@ConditionalOnProperty(name = "sftp.enable", havingValue = "true")
	public ContextHolderRequestHandlerAdvice sftpCleanupAdvice(
			DelegatingSessionFactory<SftpClient.DirEntry> delegatingSftpSessionFactory) {
		return new ContextHolderRequestHandlerAdvice(
				this::sessionFactoryKey,
				delegatingSftpSessionFactory::setThreadKey,
				delegatingSftpSessionFactory::clearThreadKey);
	}

	@Bean
	@ConditionalOnProperty(name = "sftp.enable", havingValue = "true")
	public DelegatingSessionFactory<SftpClient.DirEntry> delegatingSftpSessionFactory() {
		Map<Object, SessionFactory<SftpClient.DirEntry>> factories = new HashMap<>();
		for (var userEntry : properties.getUsers().entrySet()) {
			factories.put(userEntry.getKey(), createSessionFactory(userEntry));
		}
		return new DelegatingSessionFactory<>(new DefaultSessionFactoryLocator<>(factories));
	}

	private CachingSessionFactory<SftpClient.DirEntry> createSessionFactory(
			Map.Entry<String, SftpProperties.User> userEntry) {

		String userName = userEntry.getKey();
		SftpProperties.User user = userEntry.getValue();
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
		factory.setHost(required(user.getHost(), "Missing SFTP host for user " + userName));
		factory.setPort(user.getPort());
		factory.setUser(userName);
		if (user.getPrivateKeyPath() != null) {
			Resource privateKey = user.getPrivateKeyPath();
			factory.setPrivateKey(privateKey);
		}
		if (user.getPassword() != null && !user.getPassword().isBlank()) {
			factory.setPassword(user.getPassword());
		}
		factory.setAllowUnknownKeys(properties.isSetAllowUnknownKeys());

		CachingSessionFactory<SftpClient.DirEntry> cachingFactory = new CachingSessionFactory<>(factory);
		cachingFactory.setPoolSize(POOL_SIZE_PER_USER);
		cachingFactory.setSessionWaitTimeout(SESSIONS_WAIT_TIMEOUT);
		return cachingFactory;
	}

	@SuppressWarnings("unchecked")
	private Object registerListedFiles(Message<?> message) {
		List<SftpFileInfo> entries = (List<SftpFileInfo>) message.getPayload();
		String userName = (String) message.getHeaders().get(USER_HEADER);
		String path = (String) message.getHeaders().get(PATH_HEADER);
		SftpProperties.User user = properties.getUsers().get(userName);

		List<String> acceptedFilesRegexes = user.getAcceptFiles()
				.stream()
				.map(SftpProperties.AcceptedFile::getRegexp)
				.toList();

		// Tu decydujemy, które nazwy plików pobrać: ostatni filter pyta bazę przez BifService.exists(...).
		List<SftpFileInfo> filteredResult = entries.stream()
				.filter(entry -> !entry.isDirectory())
				.filter(entry -> acceptedFilesRegexes.stream()
						.anyMatch(regex -> entry.getFilename().matches(regex)))
				.filter(entry -> !service.exists(userName, path, entry.getFilename()))
				.toList();

		log.info(
				"New files found for user: {} at path: {}, filenames: {}",
				userName,
				path,
				filteredResult.stream().map(SftpFileInfo::getFilename).toList());

		List<BifFileCandidate> candidates = new BifsInfoMapper()
				.getBifFileCandidates(userName, path, user.getKind(), filteredResult, acceptedFilesRegexes);
		candidates.forEach(service::registerNewBif);
		return null;
	}

	private BifFileContent downloadBifFile(Session<SftpClient.DirEntry> session, Message<?> message) throws IOException {
		BifFileCandidate candidate = (BifFileCandidate) message.getPayload();
		for (String remotePath : lookupPaths(candidate)) {
			if (session.exists(remotePath)) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				// To jest właściwe pobranie całego pliku z SFTP; próbujemy IMPORT, a potem BACKUP.
				session.read(remotePath, outputStream);
				log.info("Downloaded BIF file: {}, size: {} bytes", remotePath, outputStream.size());
				return new BifFileContent(candidate, outputStream.toString(StandardCharsets.UTF_8));
			}
		}

		service.markBifAsError(candidate);
		log.warn("BIF file not found in IMPORT/BACKUP for user: {}, filename: {}", candidate.memberId(), candidate.fileName());
		return null;
	}

	private Object saveDownloadedBifFile(Message<?> message) {
		BifFileContent bifFileContent = (BifFileContent) message.getPayload();
		service.saveBifFile(bifFileContent);
		return null;
	}

	private Object sessionFactoryKey(Message<?> message) {
		Object user = message.getHeaders().get(USER_HEADER);
		if (user != null) {
			return user;
		}
		if (message.getPayload() instanceof BifFileCandidate candidate) {
			return candidate.memberId();
		}
		if (message.getPayload() instanceof BifFileContent fileContent) {
			return fileContent.candidate().memberId();
		}
		throw new IllegalStateException("Cannot resolve SFTP user for message: " + message);
	}

	private static String required(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(message);
		}
		return value;
	}

	private class BifsInfoMapper {

		private List<BifFileCandidate> getBifFileCandidates(
				String userName,
				String path,
				SftpProperties.User.Kind kind,
				List<SftpFileInfo> filteredResult,
				List<String> acceptedFilesRegexes) {

			return filteredResult.stream()
					.map(fileInfo -> new BifFileCandidate(
							userName,
							path,
							fileInfo.getFilename(),
							remotePath(path, fileInfo.getFilename()),
							NEW,
							BifKind.valueOf(kind.name()),
							bifMetadata(fileInfo.getFilename(), acceptedFilesRegexes)))
					.toList();
		}

		private BifMetadata bifMetadata(String filename, List<String> acceptedFilesRegexes) {
			for (var regex : acceptedFilesRegexes) {
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(filename);
				if (matcher.matches()) {
					String setType = getGroupOrNull("setType", pattern, matcher);
					String bifId = getGroupOrNull("bifId", pattern, matcher);
					String settlementNumber = getGroupOrNull("settlementNumber", pattern, matcher);

					return new BifMetadata(
							bifId,
							BifType.from(setType),
							new BifMetadata.SessionSlot(timeProvider.currentLocalDate(), parseInteger(settlementNumber)),
							timeProvider.currentLocalDateTime(),
							new BifMetadata.Summary(
									getIntegerGroupOrNull("numCtBlk", pattern, matcher),
									getIntegerGroupOrNull("numRfrBlk", pattern, matcher),
									getIntegerGroupOrNull("numRejBlk", pattern, matcher),
									getIntegerGroupOrNull("numPrcBlk", pattern, matcher),
									getIntegerGroupOrNull("numRoiBlk", pattern, matcher),
									getIntegerGroupOrNull("numCnrBlk", pattern, matcher),
									getIntegerGroupOrNull("numRmpBlk", pattern, matcher),
									getIntegerGroupOrNull("numRogBlk", pattern, matcher),
									getIntegerGroupOrNull("numSrBlk", pattern, matcher),
									getIntegerGroupOrNull("numRepBlk", pattern, matcher)));
				}
			}
			throw new IllegalArgumentException(
					"Filename %s doesn't match with any of regexes %s".formatted(filename, acceptedFilesRegexes));
		}

		private Integer getIntegerGroupOrNull(String group, Pattern pattern, Matcher matcher) {
			return parseInteger(getGroupOrNull(group, pattern, matcher));
		}

		private Integer parseInteger(String value) {
			if (value == null || value.isBlank() || !value.matches("\\d+")) {
				return null;
			}
			return Integer.valueOf(value);
		}

		private String getGroupOrNull(String group, Pattern pattern, Matcher matcher) {
			if (!pattern.pattern().contains("?<" + group + ">")) {
				return null;
			}
			return matcher.group(group);
		}
	}

	private static String remotePath(String dirName, String fileName) {
		return dirName.endsWith("/") ? dirName + fileName : dirName + "/" + fileName;
	}

	private Set<String> lookupPaths(BifFileCandidate candidate) {
		Set<String> paths = new LinkedHashSet<>();
		paths.add(candidate.remotePath());

		SftpProperties.User user = properties.getUsers().get(candidate.memberId());
		if (user != null) {
			user.getBackupDirectories()
					.stream()
					.map(directory -> remotePath(directory, candidate.fileName()))
					.forEach(paths::add);
		}
		if (candidate.dirName().contains("/IMPORT/")) {
			paths.add(candidate.remotePath().replace("/IMPORT/", "/BACKUP/"));
		}
		return paths;
	}
}
