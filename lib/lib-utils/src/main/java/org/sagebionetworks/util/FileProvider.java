package org.sagebionetworks.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
	 * Create a gzip output stream.
	 * @param out
	 * @return
	 * @throws IOException 
	 */
	GZIPOutputStream createGZIPOutputStream(OutputStream out) throws IOException;
	
	/**
	 * Create a Writer for the given output stream.
	 * @param out
	 * @param charSet
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException 
	 */
	Writer createWriter(OutputStream out, Charset charSet) throws FileNotFoundException, UnsupportedEncodingException;

	/**
	 * Create a FileInputStream for the given file.
	 * @param file
	 * @return
	 * @throws FileNotFoundException 
	 */
	InputStream createFileInputStream(File file) throws FileNotFoundException;
	
	/**
	 * Create a GZIPInputStream from the provided input stream.
	 * 
	 * @param in
	 * @return
	 * @throws IOException 
	 */
	GZIPInputStream createGZIPInputStream(InputStream in) throws IOException;
	

	/**
	 * Create a reader for the given input.
	 * @param in
	 * @param charset
	 * @return
	 */
	Reader createReader(InputStream in, Charset charset);
}
