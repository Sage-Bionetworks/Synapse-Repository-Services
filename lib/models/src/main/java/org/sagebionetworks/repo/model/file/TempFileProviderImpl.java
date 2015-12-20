package org.sagebionetworks.repo.model.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A very simple implementation of a temporary file provider.
 * 
 * @author John
 *
 */
public class TempFileProviderImpl implements TempFileProvider {

	@Override
	public File createTempFile(String prefix, String suffix) throws IOException {
		// Let java provide the temp file for us.
		return File.createTempFile(prefix, suffix);
	}

	@Override
	public FileInputStream createFileInputStream(File file) throws FileNotFoundException {
		return new FileInputStream(file);
	}

	@Override
	public FileOutputStream createFileOutputStream(File file) throws FileNotFoundException {
		return new FileOutputStream(file);
	}

}
