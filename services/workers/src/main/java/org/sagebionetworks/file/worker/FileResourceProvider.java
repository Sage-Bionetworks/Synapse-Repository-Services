package org.sagebionetworks.file.worker;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

/**
 * Abstraction for getting file resources.
 * By using an abstraction for file resources, resources cleanup can be tested.
 *
 */
public interface FileResourceProvider {
	
	/**
	 * Create a temporary file on the local machine.
	 * 
	 * @param prefix
	 * @param suffix
	 * @return
	 */
	public File createTempFile(String prefix, String suffix);
	
	/**
	 * Create a ZipOutputStream to write to the given file.
	 * @param outFile
	 * @return
	 */
	ZipOutputStream createZipOutputStream(File outFile);

	/**
	 * Create an InputStream for the passed file.
	 * @param tempFile
	 * @return
	 */
	public InputStream createInputStream(File tempFile);

	/**
	 * Copy all data from the input stream to the output stream.
	 * @param input
	 * @param output
	 */
	public void copy(InputStream input, OutputStream output);

}
