package com.example.demo.sftp.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingBifStatusEventPublisher implements BifStatusEventPublisher {

	private static final String TOPIC = "kir-bif-status-event";
	private static final Logger log = LoggerFactory.getLogger(LoggingBifStatusEventPublisher.class);

	@Override
	public void publish(BifStatusEvent event) {
		log.info("Publishing BIF status event to topic {}: {}", TOPIC, event);
	}
}
