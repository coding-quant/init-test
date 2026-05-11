package com.example.demo.sftp;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import com.example.demo.sftp.model.BifFileCandidate;
import com.example.demo.sftp.model.BifFileContent;
import com.example.demo.sftp.model.BifStatus;
import com.example.demo.sftp.model.BifsInfo;

public class InMemoryBifService implements BifService {

	private final Set<String> knownFiles = ConcurrentHashMap.newKeySet();
	private final List<BifsInfo> savedBifs = new CopyOnWriteArrayList<>();
	private final List<BifFileCandidate> pendingFiles = new CopyOnWriteArrayList<>();

	@Override
	public boolean exists(String memberId, String dirName, String fileName) {
		return knownFiles.contains(key(memberId, fileName));
	}

	@Override
	public void saveBifs(BifsInfo bifsInfo) {
		if (bifsInfo.filesInfo().isEmpty()) {
			return;
		}
		savedBifs.add(bifsInfo);
		bifsInfo.filesInfo().forEach(fileInfo ->
				knownFiles.add(key(bifsInfo.memberId(), fileInfo.fileName())));
	}

	@Override
	public void registerNewBif(BifFileCandidate candidate) {
		if (knownFiles.add(key(candidate.memberId(), candidate.fileName()))) {
			pendingFiles.add(candidate);
			savedBifs.add(new BifsInfo(
					candidate.memberId(),
					candidate.dirName(),
					List.of(candidate.toFileSpecificInfo())));
		}
	}

	@Override
	public List<BifFileCandidate> findNewBifsToDownload() {
		return List.copyOf(pendingFiles);
	}

	@Override
	public void saveBifFile(BifFileContent bifFileContent) {
		var candidate = bifFileContent.candidate();
		knownFiles.add(key(candidate.memberId(), candidate.fileName()));
		pendingFiles.remove(candidate);
		savedBifs.add(new BifsInfo(
				candidate.memberId(),
				candidate.dirName(),
				List.of(new BifsInfo.FileSpecificInfo(
						candidate.fileName(),
						BifStatus.RECEIVED,
						candidate.kind(),
						candidate.bifMetadata()))));
	}

	@Override
	public void markBifAsError(BifFileCandidate candidate) {
		pendingFiles.remove(candidate);
	}

	@Override
	public List<BifsInfo> findAll() {
		return List.copyOf(savedBifs);
	}

	private String key(String memberId, String fileName) {
		return memberId + "|" + fileName;
	}
}
