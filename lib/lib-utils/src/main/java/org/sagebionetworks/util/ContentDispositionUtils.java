package org.sagebionetworks.util;

public class ContentDispositionUtils {

	/**
	 * The format string for the 'Content-Disposition' header property.
	 */
	public static final String CONTENT_DISPOSITION_FORMAT_STRING = "attachment; filename=\"%s\"";

	/**
	 * The Content Disposition Value contains the file name.  For example if the file name is: 'foo.bar'
	 * then the content disposition value will be: 'attachment; filename="foo.bar"'
	 * @param fileName
	 * @return
	 */
	public static String getContentDispositionValue(String fileName){
		return String.format(CONTENT_DISPOSITION_FORMAT_STRING, fileName);
	}
}
