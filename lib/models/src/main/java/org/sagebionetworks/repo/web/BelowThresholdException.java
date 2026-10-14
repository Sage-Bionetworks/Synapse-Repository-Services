package org.sagebionetworks.repo.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thrown when an aggregate-only query matches a non-zero number of rows that is
 * below the source's suppression threshold. The count of matched rows is
 * suppressed and this exception is mapped to an HTTP 403 with a typed
 * {@code BelowThresholdErrorResponse} so callers can distinguish it from a plain
 * authorization failure.
 * <p>
 * Table queries run as asynchronous jobs. When a job fails, the framework
 * persists only the exception's class name and message and later reconstructs the
 * exception from those two values (see
 * {@code AsynchJobUtils#throwExceptionIfFailed}). To carry the threshold across
 * that boundary it is embedded in the message and parsed back out by the
 * message-only constructor.
 */
public class BelowThresholdException extends RuntimeException {

	private static final String MESSAGE_TEMPLATE = "The number of records matched by this query is below the minimum threshold of %d required to return a result.";

	private static final Pattern THRESHOLD_PATTERN = Pattern.compile("minimum threshold of (\\d+) required");

	private final Long suppressionThreshold;

	public BelowThresholdException(long suppressionThreshold) {
		super(String.format(MESSAGE_TEMPLATE, suppressionThreshold));
		this.suppressionThreshold = suppressionThreshold;
	}

	/**
	 * Message-only constructor used by the async job framework to reconstruct a
	 * failed job's exception. The threshold is recovered from the message so it
	 * survives the async round-trip.
	 *
	 * @param message
	 */
	public BelowThresholdException(String message) {
		super(message);
		this.suppressionThreshold = parseThreshold(message);
	}

	/**
	 * @return The suppression threshold the matched row count fell below, or null if
	 *         it could not be determined from the message.
	 */
	public Long getSuppressionThreshold() {
		return suppressionThreshold;
	}

	private static Long parseThreshold(String message) {
		if (message == null) {
			return null;
		}
		Matcher matcher = THRESHOLD_PATTERN.matcher(message);
		if (matcher.find()) {
			return Long.valueOf(matcher.group(1));
		}
		return null;
	}

}
