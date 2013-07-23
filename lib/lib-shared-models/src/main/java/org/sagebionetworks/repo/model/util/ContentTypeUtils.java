package org.sagebionetworks.repo.model.util;

import java.util.Arrays;
import java.util.HashSet;

public class ContentTypeUtils {
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	public static final String PLAIN_TEXT = "text/plain";
	
	private static final String[] CODE_EXTENSIONS = new String[] {".awk",".bat",".btm",".c",".cmd",".cpp",".cxx",".def",".dlg",".dpc",".dpj",".dtd",".h",".hdl",".hpp",".hrc",".html",".hxx",".inc",".ini",".inl",".ins",".ipy",".java",".js",".jsp",".l",".lgt",".ll",".par",".pl",".py",".r",".rc",".rdb",".res",".s",".sbl",".scp",".sh",".sql",".src",".srs",".xml",".xrb",".y",".yxx"};
	private static final HashSet<String> CODE_EXTENSIONS_SET = new HashSet<String>(Arrays.asList(CODE_EXTENSIONS));
	/**
	 * Return the content type that should be used for this file.
	 * @param contentType
	 * @param filename
	 * @return
	 */
	public static String getContentType(String contentType, String filename) {
		String newContentType = contentType;
		if (contentType == null || APPLICATION_OCTET_STREAM.equals(contentType.toLowerCase())) {
			//we don't trust the assigned type
			//check to see if it looks like a code file
			if (isRecognizedCodeFileName(filename)) {
				//it does look like code! set the content type to text/plain
				newContentType = PLAIN_TEXT;
			}
		}
		return newContentType;
	}
	
	public static boolean isRecognizedCodeFileName(String fileName) {
		boolean isCodeFile = false;
		if (fileName != null) {
			int lastDot = fileName.lastIndexOf(".");
			if (lastDot > -1) {
				String extension = fileName.substring(lastDot).toLowerCase();
				isCodeFile = CODE_EXTENSIONS_SET.contains(extension);
			}
		}
		return isCodeFile;
	}
}
