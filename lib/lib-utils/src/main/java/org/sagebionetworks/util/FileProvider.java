package org.sagebionetworks.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

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
	OutputStream createFileOutputStream(File file) throws FileNotFoundException;
	
	/**
	 * Create a Writer for the given file.
	 * @param file
	 * @param charSet
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException 
	 */
	Writer createFileWriter(File file, Charset charSet) throws FileNotFoundException, UnsupportedEncodingException;

	/**
	 * Create a FileInputStream for the given file.
	 * @param file
	 * @return
	 * @throws FileNotFoundException 
	 */
	InputStream createFileInputStream(File file) throws FileNotFoundException;
}
