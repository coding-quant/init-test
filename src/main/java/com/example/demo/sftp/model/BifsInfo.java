package com.example.demo.sftp.model;

import java.util.List;

public record BifsInfo(
		String memberId,
		String dirName,
		List<FileSpecificInfo> filesInfo) {

	public record FileSpecificInfo(
			String fileName,
			BifStatus status,
			BifKind kind,
			BifMetadata bifMetadata) {
	}
}
