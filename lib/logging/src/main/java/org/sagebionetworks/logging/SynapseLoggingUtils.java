package org.sagebionetworks.logging;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.reflect.MethodSignature;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SynapseLoggingUtils {

	private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})");
	private static final Pattern LEVEL_PATTERN = Pattern.compile(" \\[\\w+\\] - ");
	private static final Pattern CONTROLLER_METHOD_PATTERN = Pattern.compile("(\\w+)/(\\w+)");
	private static final Pattern PROPERTIES_PATTERN = Pattern.compile("\\?((?:\\w+=[\\w%.\\-*_+]+&?)+)$");

	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss,SSS").withZone(DateTimeZone.UTC);

	public static SynapseEvent parseSynapseEvent(String line) throws UnsupportedEncodingException {
		int lastEnd = 0;

		DateTime timeStamp;
		Matcher dateMatcher = DATE_PATTERN.matcher(line);

		if (dateMatcher.find(lastEnd)) {
			timeStamp = DATE_FORMATTER.parseDateTime(dateMatcher.group(1));
			lastEnd = dateMatcher.end();
		} else {
			throw new IllegalArgumentException(
					"Pattern incorrect: failed to find a date:\n\t"
							+ line.substring(lastEnd));
		}

		Matcher levelMatcher = LEVEL_PATTERN.matcher(line);
		if (!levelMatcher.find(lastEnd)) {
			throw new IllegalArgumentException(
					"Pattern incorrect: failed to find a log-level:\n\t"
							+ line.substring(lastEnd));
		}

		Matcher controllerMatcher = CONTROLLER_METHOD_PATTERN.matcher(line);
		String controller, methodName;
		if (controllerMatcher.find(lastEnd)) {
			controller = controllerMatcher.group(1);
			methodName = controllerMatcher.group(2);
			lastEnd = controllerMatcher.end();
		} else {
			throw new IllegalArgumentException(
					"Pattern incorrect: failed to find controller/method-name:\n\t"
							+ line.substring(lastEnd));
		}

		Matcher propMatcher = PROPERTIES_PATTERN.matcher(line);
		int latency;
		HashMap<String, String> properties;
		if (propMatcher.find()) {
			String[] propertiesArray = propMatcher.group(1).split("&");
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
			throw new IllegalArgumentException(
					"Pattern incorrect: failed to find a property string:\n\t"
							+ line.substring(lastEnd));
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

		// Using LinkedHashMap makes the testing easier because we don't have to account for
		// the normal unreliable ordering of hashmaps
		Map<String, String> properties = new LinkedHashMap<String, String>();

		for (int i = 0; i < args.length; i++) {
			properties.put(parameterNames[i],
					(args[i]!=null?args[i].toString():"null"));
		}

		return makeArgString(properties);
	}

	public static String makeArgString(Map<String, String> properties) throws UnsupportedEncodingException {
		String encoding = "UTF-8";
		String argSep = "";

		StringBuilder argString = new StringBuilder();
		for (Entry<String, String> entry : properties.entrySet()) {
			argString.append(argSep);
			argString.append(entry.getKey());
			argString.append("=");
			argString.append(URLEncoder.encode(entry.getValue().toString(), encoding));

			// Set the argSep after the first time through so that we
			// separate all the pairs with it, but don't have a leading
			// or trailing "&"
			argSep = "&";
		}

		return argString.toString();
	}

	public static String makeLogString(String simpleClassName, String methodName, long latencyMS, String args) {
		return String.format("%s/%s?latency=%d&%s",
				simpleClassName, methodName, latencyMS, args);
	}

	public static String makeLogString(SynapseEvent event) throws UnsupportedEncodingException {
		return makeLogString(event.getController(), event.getMethodName(),
				event.getLatency(), makeArgString(event.getProperties()));
	}
}
