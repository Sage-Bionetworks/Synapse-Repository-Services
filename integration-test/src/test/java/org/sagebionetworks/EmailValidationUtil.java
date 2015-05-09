package org.sagebionetworks;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/*
 * The methods in this class help read and validate emails (written as files when testing).
 */
public class EmailValidationUtil {
	
	public static String readFile(File file) throws IOException {
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		InputStream fis = new FileInputStream(file);
		try {
			IOUtils.copy(fis, content);
			return content.toString();
		} finally {
			content.close();
			fis.close();
		}
	}
	
	public static File getFileForEmail(String email) {
		String tempDir = System.getProperty("java.io.tmpdir");
		assertNotNull(tempDir);
		return new File(tempDir, email+".json");
	}
	
	public static String getTokenFromFile(File file, String startString, String endString) throws IOException {
		// the email is written to a local file.  Read it and extract the link
		String body = EmailValidationUtil.readFile(file);
		int endpointIndex = body.indexOf(startString);
		int tokenStart = endpointIndex+startString.length();
		assertTrue(tokenStart>=0);
		int tokenEnd = body.indexOf(endString, tokenStart);
		assertTrue(tokenEnd>=0);
		String token = body.substring(tokenStart, tokenEnd);
		return token;
	}
	
	

}
