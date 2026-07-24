package org.sagebionetworks.repo.model.config;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Default implementation that reads DDL files from the classpath.
 */
public class ClasspathDdlFileReader implements DdlFileReader {

	@Override
	public String readDdl(String ddlFileName) {
		if (ddlFileName == null) {
			return null;
		}
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(ddlFileName);
				Scanner scanner = in != null ? new Scanner(in, "UTF-8").useDelimiter("\\A") : null) {
			return scanner != null && scanner.hasNext() ? scanner.next() : null;
		} catch (Exception e) {
			// DDL file not found or can't be read - table has no dependencies
			return null;
		}
	}
}
