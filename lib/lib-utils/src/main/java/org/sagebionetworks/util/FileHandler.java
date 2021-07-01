package org.sagebionetworks.util;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface FileHandler <R> {


	/**
	 * Handle the passed file.s
	 * @param file
	 * @return
	 * @throws IOException
	 */
	R apply(File file) throws IOException;
	
}
