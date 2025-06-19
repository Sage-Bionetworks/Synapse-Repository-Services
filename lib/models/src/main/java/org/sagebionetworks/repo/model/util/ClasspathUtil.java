package org.sagebionetworks.repo.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class ClasspathUtil {

	/**
	 * Load a string from a file on the classpath.
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static String loadFromClasspath(String fileName) throws IOException {
		try (InputStream in = ClasspathUtil.class.getClassLoader().getResourceAsStream(fileName)) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find file " + fileName + " on classpath.");
			}
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		}
	}
}
