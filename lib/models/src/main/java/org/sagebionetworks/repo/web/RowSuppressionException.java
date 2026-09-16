package org.sagebionetworks.repo.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;

/**
 * Thrown when a query against an aggregate-only source requests row results but references a
 * quasi-identifier column in a way that would expose it as more than a count. The row results are
 * withheld and this exception is mapped to an HTTP 403 with a typed
 * {@code RowSuppressionErrorResponse} carrying the {@link RowSuppressionReasonCode} so callers can
 * distinguish it from a plain authorization failure and can re-run the query without requesting row
 * results to obtain the aggregate-only response.
 * <p>
 * Table queries run as asynchronous jobs. When a job fails, the framework persists only the
 * exception's class name and message and later reconstructs the exception from those two values (see
 * {@code AsynchJobUtils#throwExceptionIfFailed}). To carry the reason code across that boundary it is
 * embedded in the message and parsed back out by the message-only constructor.
 */
public class RowSuppressionException extends RuntimeException {

	private static final String MESSAGE_TEMPLATE = "The row results of this query were withheld because a quasi-identifier column was used in a manner that is not permitted for an aggregate-only source. Re-run the query without requesting row results to obtain the aggregate-only response. Reason code: %s.";

	private static final Pattern CODE_PATTERN = Pattern.compile("Reason code: (\\w+)\\.");

	private final RowSuppressionReasonCode reasonCode;

	public RowSuppressionException(RowSuppressionReasonCode reasonCode) {
		super(String.format(MESSAGE_TEMPLATE, reasonCode));
		this.reasonCode = reasonCode;
	}

	/**
	 * Message-only constructor used by the async job framework to reconstruct a failed job's
	 * exception. The reason code is recovered from the message so it survives the async round-trip.
	 *
	 * @param message
	 */
	public RowSuppressionException(String message) {
		super(message);
		this.reasonCode = parseReasonCode(message);
	}

	/**
	 * @return The code identifying why the row results were withheld, or null if it could not be
	 *         determined from the message.
	 */
	public RowSuppressionReasonCode getReasonCode() {
		return reasonCode;
	}

	private static RowSuppressionReasonCode parseReasonCode(String message) {
		if (message == null) {
			return null;
		}
		Matcher matcher = CODE_PATTERN.matcher(message);
		if (matcher.find()) {
			try {
				return RowSuppressionReasonCode.valueOf(matcher.group(1));
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		return null;
	}

}
