package org.sagebionetworks.upload;

public class UploadUtils {

	/**
	 * The prefix for the 'Content-Disposition' header property.
	 */
	public static final String CONTENT_DISPOSITION_PREFIX = "attachment; filename=";
	
	public static String getContentDispositionValue(String fileName){
		return CONTENT_DISPOSITION_PREFIX+fileName;
	}
}
