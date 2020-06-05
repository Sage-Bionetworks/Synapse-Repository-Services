package org.sagebionetworks.util;

import java.util.regex.Pattern;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;

public class ContentDispositionUtils {
	//only the dot character is unescaped
	private static final String SAFE_CHARACTERS = ".";
	private static final Escaper ESCAPER = new PercentEscaper(SAFE_CHARACTERS, false);

	//Amazon S3 does not expect characters that are not ISO-8859-1. Unicode characters must be URL encoded.
	//matches characters that are not compliant with  ISO-8859-1 (https://en.wikipedia.org/wiki/ISO/IEC_8859-1). We use this to replace ISO-8859-1 characters with _
	private static final Pattern NON_ISO_8859_1_REGEX =
			Pattern.compile("[^\\u0020-\\u007E\\u00A0-\\u00ff]");

	/**
	 * The format string for the 'Content-Disposition' header property.
	 */
	public static final String CONTENT_DISPOSITION_FORMAT_STRING = "attachment; filename=\"%s\"; filename*=utf-8''%s";

	/**
	 * The Content Disposition Value contains the file name.  For example if the file name is: 'foo.bar'
	 * then the content disposition value will be: 'attachment; filename="foo.bar"'
	 * @param fileName
	 * @return
	 */
	public static String getContentDispositionValue(String fileName){
			return String.format(CONTENT_DISPOSITION_FORMAT_STRING, NON_ISO_8859_1_REGEX.matcher(fileName).replaceAll("_"), ESCAPER.escape(fileName));
	}
}
