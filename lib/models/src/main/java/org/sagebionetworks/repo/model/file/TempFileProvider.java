package org.sagebionetworks.repo.model.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A simple abstraction for creating temporary files.
 * 
 * Why bother with such an abstraction?
 * 
 * Creating temporary files in java is simple (java.io.File.createTempFile()) however,
 * it is non-trivial to test that all temporary files that get created also get deleted.
 * If the temporary files do not get deleted in an enterprise system, then server hard drives
 * will fill up over time and eventually crash the server.
 * By abstracting way temp file creation from the code where the files are used we can 
 * easily test that files get deleted by mocking the temporary file provider.
 * 
 * @author John
 *
 */
public interface TempFileProvider {

	/**
	 * Create a temp file for a given prefix and suffix.
	 * 
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException 
	 */
	File createTempFile(String prefix, String suffix) throws IOException;
	
	/**
	 * Create a file input stream.
	 * @param file
	 * @return
	 * @throws FileNotFoundException 
	 */
	FileInputStream createFileInputStream(File file) throws FileNotFoundException;
	
	/**
	 * Create a file output stream
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	FileOutputStream createFileOutputStream(File file) throws FileNotFoundException;
}
