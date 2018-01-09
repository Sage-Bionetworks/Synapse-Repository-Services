package org.sagebionetworks.repo.manager.migration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Simple abstraction for creating files.
 *
 */
public interface FileProvider {

	/**
	 * Simple abstraction for creating temporary files.
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException
	 */
	File createTempFile(String prefix, String suffix) throws IOException;
	
	/**
	 * Create a FileOutputStream for the given file.
	 * @param file
	 * @return
	 * @throws FileNotFoundException 
	 */
	FileOutputStream createFileOutputStream(File file) throws FileNotFoundException;
}
