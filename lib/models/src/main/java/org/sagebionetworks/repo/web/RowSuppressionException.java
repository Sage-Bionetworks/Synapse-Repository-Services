package org.sagebionetworks.repo.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;

/**
 * Thrown when a query against an aggregate-only source requests row results that cannot be returned:
 * either the source defines no quasi-identifier columns (so it never returns rows), or a
 * quasi-identifier column is referenced in a way that would expose it as more than a count. The row
 * results are withheld and this exception is mapped to an HTTP 403 with a typed
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

	private static final String NO_QUASI_IDENTIFIERS_REASON = "The row results of this query were withheld because the aggregate-only source defines no quasi-identifier columns and therefore never returns row-level results.";

	private static final String QID_MISUSE_REASON = "The row results of this query were withheld because a quasi-identifier column was used in a manner that is not permitted for an aggregate-only source.";

	// The reason code is embedded here so it survives the async job round-trip; the message-only
	// constructor parses it back out via CODE_PATTERN.
	private static final String ADVICE_SUFFIX = " Re-run the query without requesting row results to obtain the aggregate-only response. Reason code: %s.";

	private static final Pattern CODE_PATTERN = Pattern.compile("Reason code: (\\w+)\\.");

	private final RowSuppressionReasonCode reasonCode;

	public RowSuppressionException(RowSuppressionReasonCode reasonCode) {
		super(buildMessage(reasonCode));
		this.reasonCode = reasonCode;
	}

	private static String buildMessage(RowSuppressionReasonCode reasonCode) {
		String reason = RowSuppressionReasonCode.NO_QUASI_IDENTIFIERS.equals(reasonCode)
				? NO_QUASI_IDENTIFIERS_REASON
				: QID_MISUSE_REASON;
		return reason + String.format(ADVICE_SUFFIX, reasonCode);
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
