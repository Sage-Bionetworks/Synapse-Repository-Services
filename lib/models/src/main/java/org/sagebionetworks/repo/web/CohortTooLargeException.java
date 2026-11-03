package org.sagebionetworks.repo.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thrown when a request-scoped cohort captures more distinct values than the configured maximum. It
 * is an {@link IllegalArgumentException}, so it maps to an HTTP 400, with its own error code so
 * callers can distinguish it from other bad requests.
 * <p>
 * Table queries run as asynchronous jobs. When a job fails, the framework persists only the
 * exception's class name and message and later reconstructs the exception from those two values
 * (see {@code AsynchJobUtils#throwExceptionIfFailed}). To carry the maximum across that boundary it
 * is embedded in the message and parsed back out by the message-only constructor. The message never
 * includes the cohort's actual size.
 */
public class CohortTooLargeException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	private static final String MESSAGE_TEMPLATE = "Cohort '%s' exceeds the maximum of %d values.";

	private static final Pattern MAX_PATTERN = Pattern.compile("exceeds the maximum of (\\d+) values");

	private final Long maximumValues;

	public CohortTooLargeException(String cohortName, long maximumValues) {
		super(String.format(MESSAGE_TEMPLATE, cohortName, maximumValues));
		this.maximumValues = maximumValues;
	}

	/**
	 * Message-only constructor used by the async job framework to reconstruct a failed job's
	 * exception. The maximum is recovered from the message so it survives the async round-trip.
	 *
	 * @param message
	 */
	public CohortTooLargeException(String message) {
		super(message);
		this.maximumValues = parseMaximum(message);
	}

	/**
	 * @return The maximum number of distinct values a cohort may have, or null if it could not be
	 *         determined from the message.
	 */
	public Long getMaximumValues() {
		return maximumValues;
	}

	private static Long parseMaximum(String message) {
		if (message == null) {
			return null;
		}
		Matcher matcher = MAX_PATTERN.matcher(message);
		if (matcher.find()) {
			return Long.valueOf(matcher.group(1));
		}
		return null;
	}

}
