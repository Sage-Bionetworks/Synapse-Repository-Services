package org.sagebionetworks.repo.web.service;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.LogEntry;

/**
 * Basic implementation.
 * 
 * @author John
 *
 */
public class LogServiceImpl implements LogService {

	private static final Logger log = LogManager.getLogger(LogService.class);

	@Override
	public void log(final LogEntry logEntry) {
		Level level = Level.ERROR;
		switch (logEntry.getLevel()) {
		case INFO:
			level = Level.INFO;
			break;
		case WARN:
			level = Level.WARN;
			break;
		case ERROR:
			level = Level.ERROR;
			break;
		}
		Throwable t = new Throwable(logEntry.getMessage()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void printStackTrace() {
				printStackTrace(System.err);
			}

			@Override
			public void printStackTrace(PrintStream s) {
				s.print(logEntry.getStacktrace());
			}

			@Override
			public void printStackTrace(PrintWriter s) {
				s.print(logEntry.getStacktrace());
			}
		};
		log.log(level, logEntry.getMessage(), t);
	}
}
