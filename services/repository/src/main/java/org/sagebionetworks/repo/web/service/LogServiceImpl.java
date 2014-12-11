package org.sagebionetworks.repo.web.service;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.LogEntry;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Basic implementation.
 * 
 * @author John
 * 
 */
public class LogServiceImpl implements LogService {

	private static final Logger log = LogManager.getLogger(LogService.class);

	@Autowired
	Consumer consumer;

	@Override
	public void log(final LogEntry logEntry, String userAgent) {
		log.error(logEntry.getLabel() + " - " + userAgent + ": " + logEntry.getMessage()
				+ (logEntry.getStacktrace() == null ? "" : ("\n" + logEntry.getStacktrace())));

		Date now = new Date();

		// log twice, once with just the label
		ProfileData logEvent = new ProfileData();
		logEvent.setNamespace(LogService.class.getName());
		logEvent.setName(logEntry.getLabel());
		logEvent.setValue(1.0);
		logEvent.setUnit("Count");
		logEvent.setTimestamp(now);
		consumer.addProfileData(logEvent);

		// once with the label and the user agent
		logEvent = new ProfileData();
		logEvent.setNamespace(LogService.class.getName());
		logEvent.setName(logEntry.getLabel());
		logEvent.setValue(1.0);
		logEvent.setUnit("Count");
		logEvent.setTimestamp(now);
		logEvent.setDimension(Collections.singletonMap("UserAgent", userAgent));
		consumer.addProfileData(logEvent);
	}
}
