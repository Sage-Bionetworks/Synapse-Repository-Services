package org.sagebionetworks.simpleHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public interface StreamProvider {

	public FileOutputStream getFileOutputStream(File file) throws FileNotFoundException;
}
