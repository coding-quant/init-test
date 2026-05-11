package com.example.demo.sftp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

@Component
public class TimeProvider {

	public LocalDate currentLocalDate() {
		return LocalDate.now();
	}

	public LocalDateTime currentLocalDateTime() {
		return LocalDateTime.now();
	}

	public ZonedDateTime currentZonedDateTime() {
		return ZonedDateTime.now();
	}
}
