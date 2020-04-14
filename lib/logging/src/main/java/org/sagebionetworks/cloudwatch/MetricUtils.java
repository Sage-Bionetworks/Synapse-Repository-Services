package org.sagebionetworks.cloudwatch;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class MetricUtils {

	/**
	 * Turn the stack trace in a string that can be used as a metric dimension
	 * 
	 * @param cause
	 * @return
	 */
	public static String stackTracetoString(Throwable cause) {
		String stackTraceAsString = "";
		if (cause != null) {
			stackTraceAsString = ExceptionUtils.getStackTrace(cause);
			String message = cause.getMessage();
			if (message != null && message.length() > 0) {
				int i = stackTraceAsString.indexOf(message);
				if (i >= 0) {
					stackTraceAsString = stackTraceAsString.substring(0, i)
							+ stackTraceAsString.substring(i + message.length());
				}
			}
		}
		return stackTraceAsString;
	}

}
