package com.example.demo.sftp.events;

import java.time.LocalDateTime;

import com.example.demo.sftp.model.BifKind;
import com.example.demo.sftp.model.BifStatus;

public record BifStatusEvent(
		String memberId,
		String dirName,
		String fileName,
		BifKind kind,
		BifStatus status,
		LocalDateTime occurredAt) {
}
