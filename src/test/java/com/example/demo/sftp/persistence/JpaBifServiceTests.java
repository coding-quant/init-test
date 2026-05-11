package com.example.demo.sftp.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.sftp.BifService;
import com.example.demo.sftp.model.BifFileCandidate;
import com.example.demo.sftp.model.BifFileContent;
import com.example.demo.sftp.model.BifKind;
import com.example.demo.sftp.model.BifMetadata;
import com.example.demo.sftp.model.BifStatus;
import com.example.demo.sftp.model.BifType;
import com.example.demo.sftp.model.BlockContentType;

@SpringBootTest
class JpaBifServiceTests {

	@Autowired
	private BifService service;

	@Autowired
	private BifFileRepository repository;

	@Autowired
	private BlockContentRepository blockContentRepository;

	@BeforeEach
	void setUp() {
		blockContentRepository.deleteAll();
		repository.deleteAll();
	}

	@Test
	void savesDownloadedFileContent() {
		BifFileContent fileContent = new BifFileContent(
				new BifFileCandidate(
						"16000003",
						"/IMPORT/EUELIXIR",
						"EEpT20260512OUTABCDE.CTCVN.CV1",
						"/IMPORT/EUELIXIR/EEpT20260512OUTABCDE.CTCVN.CV1",
						BifStatus.NEW,
						BifKind.SEPA_SCT,
						new BifMetadata(
								"ABCDE",
								BifType.CT,
								new BifMetadata.SessionSlot(LocalDate.of(2026, 5, 12), 20260512),
								LocalDateTime.of(2026, 5, 12, 10, 0),
								new BifMetadata.Summary(null, null, null, null, null, null, null, null, null, null))),
				"file-body");

		service.saveBifFile(fileContent);

		assertThat(service.exists("16000003", "/IMPORT/EUELIXIR", "EEpT20260512OUTABCDE.CTCVN.CV1")).isTrue();
		assertThat(repository.findAll()).singleElement()
				.satisfies(entity -> {
					assertThat(entity.getStatus()).isEqualTo(BifStatus.RECEIVED);
				});
		assertThat(blockContentRepository.findAll()).singleElement()
				.satisfies(entity -> {
					assertThat(entity.getType()).isEqualTo(BlockContentType.BIF);
					assertThat(entity.getContent()).isEqualTo("file-body");
				});
	}
}
