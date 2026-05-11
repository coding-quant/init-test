package com.example.demo.sftp.persistence;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.sftp.BifService;
import com.example.demo.sftp.TimeProvider;
import com.example.demo.sftp.events.BifStatusEvent;
import com.example.demo.sftp.events.BifStatusEventPublisher;
import com.example.demo.sftp.model.BifFileCandidate;
import com.example.demo.sftp.model.BifFileContent;
import com.example.demo.sftp.model.BifMetadata;
import com.example.demo.sftp.model.BifStatus;
import com.example.demo.sftp.model.BifsInfo;
import com.example.demo.sftp.model.BlockContentType;

@Service
public class JpaBifService implements BifService {

	private static final Logger log = LoggerFactory.getLogger(JpaBifService.class);

	private final BifFileRepository bifRepository;
	private final BlockContentRepository blockContentRepository;
	private final BifStatusEventPublisher eventPublisher;
	private final TimeProvider timeProvider;

	public JpaBifService(
			BifFileRepository bifRepository,
			BlockContentRepository blockContentRepository,
			BifStatusEventPublisher eventPublisher,
			TimeProvider timeProvider) {
		this.bifRepository = bifRepository;
		this.blockContentRepository = blockContentRepository;
		this.eventPublisher = eventPublisher;
		this.timeProvider = timeProvider;
	}

	@Override
	public boolean exists(String memberId, String dirName, String fileName) {
		// Tu sprawdzamy w bazie, czy dana nazwa BIF dla uczestnika została już zarejestrowana.
		return bifRepository.existsByMemberIdAndFileName(memberId, fileName);
	}

	@Override
	@Transactional
	public void saveBifs(BifsInfo bifsInfo) {
		for (var fileInfo : bifsInfo.filesInfo()) {
			registerNewBif(new BifFileCandidate(
					bifsInfo.memberId(),
					bifsInfo.dirName(),
					fileInfo.fileName(),
					remotePath(bifsInfo.dirName(), fileInfo.fileName()),
					fileInfo.status(),
					fileInfo.kind(),
					fileInfo.bifMetadata()));
		}
	}

	@Override
	@Transactional
	public void registerNewBif(BifFileCandidate candidate) {
		if (exists(candidate.memberId(), candidate.dirName(), candidate.fileName())) {
			return;
		}

		try {
			BifFileEntity entity = toEntity(candidate, BifStatus.NEW);
			bifRepository.save(entity);
			publishStatus(entity);
		}
		catch (DataIntegrityViolationException duplicate) {
			log.info("Skipping concurrently registered BIF file: {}/{}", candidate.memberId(), candidate.fileName());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<BifFileCandidate> findNewBifsToDownload() {
		// Tu bierzemy z bazy nazwy BIF ze statusem NEW, które mają zostać pobrane z SFTP.
		return bifRepository.findAllByStatusOrderByCreatedAtAsc(BifStatus.NEW)
				.stream()
				.map(BifFileEntity::toCandidate)
				.toList();
	}

	@Override
	@Transactional
	public void saveBifFile(BifFileContent bifFileContent) {
		BifFileCandidate candidate = bifFileContent.candidate();
		BifFileEntity importBif = bifRepository.findByMemberIdAndFileName(candidate.memberId(), candidate.fileName())
				.orElseGet(() -> bifRepository.save(toEntity(candidate, BifStatus.NEW)));

		if (!blockContentRepository.existsByImportBif(importBif)) {
			BlockContentEntity blockContent = new BlockContentEntity();
			blockContent.setType(BlockContentType.BIF);
			blockContent.setContent(bifFileContent.content());
			blockContent.setImportBif(importBif);
			blockContentRepository.save(blockContent);
		}

		importBif.setStatus(BifStatus.RECEIVED);
		publishStatus(importBif);
	}

	@Override
	@Transactional
	public void markBifAsError(BifFileCandidate candidate) {
		BifFileEntity importBif = bifRepository.findByMemberIdAndFileName(candidate.memberId(), candidate.fileName())
				.orElseGet(() -> bifRepository.save(toEntity(candidate, BifStatus.NEW)));
		importBif.setStatus(BifStatus.ERROR);
		publishStatus(importBif);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BifsInfo> findAll() {
		return bifRepository.findAllByOrderByCreatedAtAsc()
				.stream()
				.map(entity -> new BifsInfo(
						entity.getMemberId(),
						entity.getDirName(),
						List.of(new BifsInfo.FileSpecificInfo(
								entity.getFileName(),
								entity.getStatus(),
								entity.getKind(),
								entity.toMetadata()))))
				.toList();
	}

	private BifFileEntity toEntity(BifFileCandidate candidate, BifStatus status) {
		BifMetadata metadata = candidate.bifMetadata();
		BifMetadata.Summary summary = metadata.summary();

		BifFileEntity entity = new BifFileEntity();
		entity.setMemberId(candidate.memberId());
		entity.setDirName(candidate.dirName());
		entity.setFileName(candidate.fileName());
		entity.setRemotePath(candidate.remotePath());
		entity.setStatus(status);
		entity.setKind(candidate.kind());
		entity.setKirBifId(metadata.kirBifId());
		entity.setBifType(metadata.type());
		entity.setSessionDate(metadata.sessionSlot().date());
		entity.setSessionNumber(metadata.sessionSlot().number());
		entity.setBifTimestamp(metadata.bifTimestamp());
		entity.setNumCtBlk(summary.numCtBlk());
		entity.setNumRfrBlk(summary.numRfrBlk());
		entity.setNumRejBlk(summary.numRejBlk());
		entity.setNumPrcBlk(summary.numPrcBlk());
		entity.setNumRoiBlk(summary.numRoiBlk());
		entity.setNumCnrBlk(summary.numCnrBlk());
		entity.setNumRmpBlk(summary.numRmpBlk());
		entity.setNumRogBlk(summary.numRogBlk());
		entity.setNumSrBlk(summary.numSrBlk());
		entity.setNumRepBlk(summary.numRepBlk());
		return entity;
	}

	private void publishStatus(BifFileEntity importBif) {
		eventPublisher.publish(new BifStatusEvent(
				importBif.getMemberId(),
				importBif.getDirName(),
				importBif.getFileName(),
				importBif.getKind(),
				importBif.getStatus(),
				timeProvider.currentLocalDateTime()));
	}

	private String remotePath(String dirName, String fileName) {
		return dirName.endsWith("/") ? dirName + fileName : dirName + "/" + fileName;
	}
}
