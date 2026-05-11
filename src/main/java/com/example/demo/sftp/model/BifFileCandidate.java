package com.example.demo.sftp.model;

public record BifFileCandidate(
		String memberId,
		String dirName,
		String fileName,
		String remotePath,
		BifStatus status,
		BifKind kind,
		BifMetadata bifMetadata) {

	public BifsInfo.FileSpecificInfo toFileSpecificInfo() {
		return new BifsInfo.FileSpecificInfo(fileName, status, kind, bifMetadata);
	}
}
