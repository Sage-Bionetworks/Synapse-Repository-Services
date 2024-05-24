package org.sagebionetworks.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for normalizing request paths
 */
public class PathNormalizer {
	private static final String PREFIXES_TO_REMOVE_DELIMITER = "/v1/";
	private static final Pattern NUMERIC_PARAM_PATTERN = Pattern.compile("/(syn\\d+|\\d+)");
	private static final String GET_MD5_URL_PART = "/entity/md5";
	private static final String GET_EVALUATION_NAME_URL_PART = "/evaluation/name";
	private static final String GET_ENTITY_ALIAS_URL_PART = "/entity/alias";
	private static final String NUMBER_REPLACEMENT = "/#";
	
	/**
	 * Normalize the access record's request path
	 * 
	 * @param requestPath
	 * @return
	 */
	public static String normalizeMethodSignature(String requestPath) {
		requestPath = requestPath.toLowerCase();
		int prefixIndex = requestPath.indexOf(PREFIXES_TO_REMOVE_DELIMITER);
		if (prefixIndex == -1) {
			throw new IllegalArgumentException("requestPath: " + requestPath + " was not correctly formatted. It must start with {optional WAR name}/{any string}/v1/");
		}
		requestPath = requestPath.substring(prefixIndex + PREFIXES_TO_REMOVE_DELIMITER.length() - 1);

		if (requestPath.startsWith(GET_MD5_URL_PART)) {
			return GET_MD5_URL_PART + NUMBER_REPLACEMENT;
		}
		if (requestPath.startsWith(GET_EVALUATION_NAME_URL_PART)) {
			return GET_EVALUATION_NAME_URL_PART + NUMBER_REPLACEMENT;
		}
		if (requestPath.startsWith(GET_ENTITY_ALIAS_URL_PART)) {
			return GET_ENTITY_ALIAS_URL_PART + NUMBER_REPLACEMENT;
		}
		Matcher matcher = NUMERIC_PARAM_PATTERN.matcher(requestPath);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(buffer, NUMBER_REPLACEMENT);
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}
}
