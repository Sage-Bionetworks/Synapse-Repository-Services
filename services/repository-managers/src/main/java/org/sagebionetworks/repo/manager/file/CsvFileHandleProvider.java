package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Abstraction for getting a access to the content of a CSV file from a file hanle
 */
public interface CsvFileHandleProvider {
	
	/**
	 * @return A CSV reader that streams from the file handle with the given id. Only supports file handles that implement {@link CloudProviderFileHandleInterface}.
	 */
	CSVReader getCsvReader(UserInfo user, String fileHandleId, CsvTableDescriptor csvDescriptor);

	/**
	 * @return A CSV reader that streams from the the given file handle. Only supports file handles that implement {@link CloudProviderFileHandleInterface}.
	 */
	CSVReader getCsvReader(FileHandle fileHandle, CsvTableDescriptor csvDescriptor);
}
