package org.sagebionetworks.repo.manager.backup;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * @author deflaux
 *
 */
public interface SearchDocumentDriver {
	
	/**
	 * Create a search document, writing the results to the passed destination file.
	 * @param destination
	 * @param progress
	 * @param entitiesToBackup
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 * @throws JSONObjectAdapterException 
	 */
	public void writeSearchDocument(File destination, Progress progress, Set<String> entitiesToBackup) throws IOException, DatastoreException, NotFoundException, InterruptedException, JSONObjectAdapterException;
	

}