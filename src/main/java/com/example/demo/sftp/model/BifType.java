package com.example.demo.sftp.model;

public enum BifType {
	CT,
	RFR,
	REJ,
	PRC,
	ROI,
	CNR,
	RMP,
	ROG,
	SR,
	REP,
	IX,
	SCT,
	PDC,
	UNKNOWN;

	public static BifType from(String value) {
		if (value == null || value.isBlank()) {
			return UNKNOWN;
		}
		try {
			return BifType.valueOf(value);
		}
		catch (IllegalArgumentException ignored) {
			return UNKNOWN;
		}
	}
}
