package org.sagebionetworks.logging;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.reflect.MethodSignature;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SynapseLoggingUtils {

	private static final String DATE_PATTERN = "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})";
	private static final String LEVEL_PATTERN = " \\[\\w+\\] - ";
	private static final String CONTROLLER_METHOD_PATTERN = "(\\w+)/(\\w+)";
	private static final String PROPERTIES_PATTERN = "\\?((?:\\w+=[\\w%.\\-*_+]+&?)+)$";

	public static final Pattern LOGFILE_REGEX = Pattern.compile(DATE_PATTERN+LEVEL_PATTERN+CONTROLLER_METHOD_PATTERN+PROPERTIES_PATTERN);
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss,SSS").withZone(DateTimeZone.UTC);

	public static SynapseEvent parseSynapseEvent(String line) throws UnsupportedEncodingException {
		Matcher matcher = LOGFILE_REGEX.matcher(line);

		DateTime timeStamp;
		String controller, methodName;
		int latency;
		HashMap<String, String> properties;

		if (matcher.matches()) {
			timeStamp = DATE_FORMATTER.parseDateTime(matcher.group(1));
			controller = matcher.group(2);
			methodName = matcher.group(3);

			String[] propertiesArray = matcher.group(4).split("&");
			properties = new HashMap<String, String>();

			for (String property : propertiesArray) {
				String[] keyAndVal = property.split("=", 2);
				properties.put(keyAndVal[0], URLDecoder.decode(keyAndVal[1], "UTF-8"));
			}
			if (properties.containsKey("latency")) {
				latency = Integer.parseInt(properties.get("latency"));
				properties.remove("latency");
			} else {
				latency = -1;
			}
		} else {
			throw new IllegalArgumentException("Line does not represent a SynapseEVent.");
		}
		return new SynapseEvent(timeStamp, controller, methodName, latency, properties);
	}

	/**
	 * Method for returning a coherent arg string from the relevant information.
	 * @param sig method signature from the join point
	 * @param args list of actual arguments to be passed to the join point
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String makeArgString(MethodSignature sig, Object[] args) throws UnsupportedEncodingException {
		Method method = sig.getMethod();

		if (method == null) {
			return Arrays.toString(args);
		}
		String[] parameterNames = sig.getParameterNames();

		String encoding = "UTF-8";

		String argSep = "";

		StringBuilder argString = new StringBuilder();

		for (int i = 0; i < args.length; i++) {
			argString.append(argSep);
			argString.append(parameterNames[i]);

			argString.append("=");
			if (args[i] != null)
				argString.append(URLEncoder.encode(args[i].toString(), encoding));
			else
				argString.append(URLEncoder.encode("null", encoding));

			// Set the argSep after the first time through
			argSep = "&";
		}

		return argString.toString();
	}

	public static String makeLogString(String simpleClassName, String methodName, long latencyMS, String args) {
		return String.format("%s/%s?latency=%d&%s",
				simpleClassName, methodName, latencyMS, args);
	}
}
