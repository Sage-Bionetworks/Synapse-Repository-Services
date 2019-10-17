package org.sagebionetworks.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Simple wrapper for static file calls.
 *
 */
public class FileProviderImpl implements FileProvider {

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.util.FileProvider#createTempFile(java.lang.String, java.lang.String)
	 */
	@Override
	public File createTempFile(String prefix, String suffix) throws IOException {
		return File.createTempFile(prefix, suffix);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.util.FileProvider#createFileOutputStream(java.io.File)
	 */
	@Override
	public FileOutputStream createFileOutputStream(File file) throws FileNotFoundException {
		return new FileOutputStream(file);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.util.FileProvider#createFileInputStream(java.io.File)
	 */
	@Override
	public FileInputStream createFileInputStream(File file) throws FileNotFoundException {
		return new FileInputStream(file);
	}

	@Override
	public Writer createWriter(OutputStream out, Charset charSet) throws FileNotFoundException {
		return new OutputStreamWriter(out, charSet);
	}

	@Override
	public GZIPOutputStream createGZIPOutputStream(OutputStream out) throws IOException {
		return new GZIPOutputStream(out);
	}

	@Override
	public GZIPInputStream createGZIPInputStream(InputStream in) throws IOException {
		return new GZIPInputStream(in);
	}

	@Override
	public Reader createReader(InputStream in, Charset charset) {
		return new InputStreamReader(in, charset);
	}

}
