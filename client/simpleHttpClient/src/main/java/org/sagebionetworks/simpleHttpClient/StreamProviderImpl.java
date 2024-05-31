package org.sagebionetworks.simpleHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public final class StreamProviderImpl implements StreamProvider{

	@Override
	public FileOutputStream getFileOutputStream(File file) throws FileNotFoundException {
		if (file == null) {
			throw new IllegalArgumentException("file cannot be null");
		}
		return new FileOutputStream(file);
	}

}
