package org.sagebionetworks.repo.manager.statistics;

public class StatisticsProcessingException extends RuntimeException {

	public StatisticsProcessingException() {
		super();
	}

	public StatisticsProcessingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public StatisticsProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public StatisticsProcessingException(String message) {
		super(message);
	}

	public StatisticsProcessingException(Throwable cause) {
		super(cause);
	}

}
