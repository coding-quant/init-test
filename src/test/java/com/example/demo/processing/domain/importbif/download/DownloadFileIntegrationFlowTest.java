package com.example.demo.processing.domain.importbif.download;

import com.example.demo.processing.domain.common.model.BifKind;
import com.example.demo.processing.domain.common.model.BifStatus;
import com.example.demo.processing.domain.common.model.BlockContent;
import com.example.demo.processing.domain.importbif.ImportBifRepository;
import com.example.demo.processing.domain.importbif.model.ImportBif;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(properties = {
        "sftp.enable=true",
        "sftp.set-allow-unknown-keys=true",
        "sftp.pool-size-per-user=1",
        "sftp.sessions-wait-timeout=5000",
        "sftp.poller-interval=1s",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
class DownloadFileIntegrationFlowTest {

    private static final String MEMBER_ID = "16000003";
    private static final String IMPORT_DIRECTORY = "/IMPORT/EUELIXIR";
    private static final String BACKUP_DIRECTORY = "/BACKUP/EUELIXIR";
    private static final String FILE_NAME = "EEp20260512OUTABCDE.SCTCVN.CV1";
    private static final String FILE_CONTENT = "test file content from atmoz sftp\nsecond line\n";

    @Container
    static GenericContainer<?> sftp = new GenericContainer<>(DockerImageName.parse("atmoz/sftp:latest"))
            .withExposedPorts(22)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("sftp/test_user_ed25519.pub"),
                    "/home/" + MEMBER_ID + "/.ssh/keys/test_user_ed25519.pub")
            .withCommand(MEMBER_ID + ":password:1001:1001:IMPORT/EUELIXIR,BACKUP/EUELIXIR");

    @DynamicPropertySource
    static void registerSftpProperties(DynamicPropertyRegistry registry) {
        registry.add("sftp.users." + MEMBER_ID + ".host", sftp::getHost);
        registry.add("sftp.users." + MEMBER_ID + ".port", () -> sftp.getMappedPort(22));
        registry.add("sftp.users." + MEMBER_ID + ".private-key-path", () -> "classpath:sftp/test_user_ed25519");
        registry.add("sftp.users." + MEMBER_ID + ".kind", () -> BifKind.SEPA_SCT.name());
        registry.add("sftp.users." + MEMBER_ID + ".directories[0]", () -> IMPORT_DIRECTORY);
        registry.add("sftp.users." + MEMBER_ID + ".directories[1]", () -> BACKUP_DIRECTORY);
        registry.add("sftp.users." + MEMBER_ID + ".accept-files[0].type", () -> "OUT_SCT_MESSAGE");
        registry.add("sftp.users." + MEMBER_ID + ".accept-files[0].regexp", () -> ".*");
    }

    @Autowired
    private ImportBifRepository importBifRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() throws Exception {
        importBifRepository.deleteAll();
        cleanRemoteDirectories();
    }

    @Test
    void downloadsRemoteFileAndSavesItsContentInBlockContent() throws Exception {
        putRemoteFile(IMPORT_DIRECTORY, FILE_NAME, FILE_CONTENT);

        ImportBif importBif = saveNewImportBif();

        ImportBif downloaded = waitForReceivedImportBif(importBif.getId());

        assertThat(downloaded.getStatus()).isEqualTo(BifStatus.RECEIVED);
        assertThat(downloaded.getBlockContents()).hasSize(1);

        BlockContent blockContent = downloaded.getBlockContents().get(0);
        assertThat(blockContent.getKind()).isEqualTo(BifKind.SEPA_SCT);
        assertThat(blockContent.getContent()).isEqualTo(FILE_CONTENT);
    }

    @Test
    void downloadsRemoteFileFromBackupDirectoryWhenImportDirectoryDoesNotContainFile() throws Exception {
        putRemoteFile(BACKUP_DIRECTORY, FILE_NAME, FILE_CONTENT);

        ImportBif importBif = saveNewImportBif();

        ImportBif downloaded = waitForReceivedImportBif(importBif.getId());

        assertThat(downloaded.getStatus()).isEqualTo(BifStatus.RECEIVED);
        assertThat(downloaded.getBlockContents())
                .extracting(BlockContent::getContent)
                .containsExactly(FILE_CONTENT);
    }

    private ImportBif saveNewImportBif() {
        ImportBif importBif = importBifRepository.saveAndFlush(ImportBif.builder()
                .status(BifStatus.NEW)
                .kind(BifKind.SEPA_SCT)
                .memberId(MEMBER_ID)
                .dirName(IMPORT_DIRECTORY)
                .fileName(FILE_NAME)
                .build());

        return importBif;
    }

    private void cleanRemoteDirectories() throws Exception {
        String importPath = "/home/" + MEMBER_ID + IMPORT_DIRECTORY;
        String backupPath = "/home/" + MEMBER_ID + BACKUP_DIRECTORY;
        sftp.execInContainer(
                "sh",
                "-c",
                "mkdir -p " + importPath + " " + backupPath
                        + " && rm -f " + importPath + "/" + FILE_NAME
                        + " " + backupPath + "/" + FILE_NAME
        );
    }

    private void putRemoteFile(String directory, String fileName, String content) throws Exception {
        String absoluteDirectory = "/home/" + MEMBER_ID + directory;
        sftp.execInContainer("mkdir", "-p", absoluteDirectory);
        sftp.copyFileToContainer(
                Transferable.of(content.getBytes(StandardCharsets.UTF_8), 0644),
                absoluteDirectory + "/" + fileName
        );
    }

    private ImportBif waitForReceivedImportBif(UUID id) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        AssertionError lastAssertionError = null;

        while (System.nanoTime() < deadline) {
            entityManager.clear();
            Optional<ImportBif> importBif = findImportBifWithBlockContents(id);
            try {
                assertThat(importBif).isPresent();
                assertThat(importBif.get().getStatus()).isEqualTo(BifStatus.RECEIVED);
                assertThat(importBif.get().getBlockContents())
                        .extracting(BlockContent::getContent)
                        .containsExactly(FILE_CONTENT);
                return importBif.get();
            } catch (AssertionError e) {
                lastAssertionError = e;
                Thread.sleep(250);
            }
        }

        fail("ImportBif was not downloaded and saved to BLOCK_CONTENT in time", lastAssertionError);
        throw new IllegalStateException("Unreachable");
    }

    private Optional<ImportBif> findImportBifWithBlockContents(UUID id) {
        List<ImportBif> result = entityManager.createQuery("""
                        select distinct importBif
                        from IMPORT_BIF importBif
                        left join fetch importBif.blockContents
                        where importBif.id = :id
                        """, ImportBif.class)
                .setParameter("id", id)
                .getResultList();
        return result.stream().findFirst();
    }
}
