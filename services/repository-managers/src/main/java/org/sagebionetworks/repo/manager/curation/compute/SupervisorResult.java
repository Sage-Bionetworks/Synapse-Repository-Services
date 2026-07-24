package org.sagebionetworks.repo.manager.curation.compute;

/**
 * Interprets the final response of a compute-task supervisor against its completion contract. A
 * supervisor ends its final message with a "RESULT: SUCCESS - ..." or "RESULT: ERROR - ..." marker;
 * this helper checks for success and extracts a meaningful failure message otherwise. Shared by the
 * sub-workers so the contract is interpreted identically.
 */
final class SupervisorResult {

	static final String SUCCESS_MARKER = "RESULT: SUCCESS";
	static final String ERROR_MARKER = "RESULT: ERROR";

	private SupervisorResult() {
	}

	/**
	 * Throws {@link IllegalStateException} if the supervisor did not report success. The thrown
	 * message is the given prefix followed by the supervisor's error explanation (or the whole
	 * response if no error marker is present).
	 */
	static void requireSuccess(String supervisorResponse, String failurePrefix) {
		if (supervisorResponse == null || !supervisorResponse.contains(SUCCESS_MARKER)) {
			throw new IllegalStateException(failurePrefix + extractErrorMessage(supervisorResponse));
		}
	}

	private static String extractErrorMessage(String supervisorResponse) {
		if (supervisorResponse == null) {
			return "the supervisor returned no response";
		}
		int idx = supervisorResponse.indexOf(ERROR_MARKER);
		if (idx >= 0) {
			return supervisorResponse.substring(idx);
		}
		return supervisorResponse;
	}
}
