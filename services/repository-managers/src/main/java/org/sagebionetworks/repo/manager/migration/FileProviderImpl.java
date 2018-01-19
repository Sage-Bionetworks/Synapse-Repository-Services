package org.sagebionetworks.repo.manager.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Simple wrapper for static file calls.
 *
 */
public class FileProviderImpl implements FileProvider {

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.FileProvider#createTempFile(java.lang.String, java.lang.String)
	 */
	@Override
	public File createTempFile(String prefix, String suffix) throws IOException {
		return File.createTempFile(prefix, suffix);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.FileProvider#createFileOutputStream(java.io.File)
	 */
	@Override
	public FileOutputStream createFileOutputStream(File file) throws FileNotFoundException {
		return new FileOutputStream(file);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.FileProvider#createFileInputStream(java.io.File)
	 */
	@Override
	public FileInputStream createFileInputStream(File file) throws FileNotFoundException {
		return new FileInputStream(file);
	}

}
