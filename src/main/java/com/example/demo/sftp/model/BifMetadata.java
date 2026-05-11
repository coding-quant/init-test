package com.example.demo.sftp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BifMetadata(
		String kirBifId,
		BifType type,
		SessionSlot sessionSlot,
		LocalDateTime bifTimestamp,
		Summary summary) {

	public record SessionSlot(LocalDate date, Integer number) {
	}

	public record Summary(
			Integer numCtBlk,
			Integer numRfrBlk,
			Integer numRejBlk,
			Integer numPrcBlk,
			Integer numRoiBlk,
			Integer numCnrBlk,
			Integer numRmpBlk,
			Integer numRogBlk,
			Integer numSrBlk,
			Integer numRepBlk) {
	}
}
