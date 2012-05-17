package org.sagebionetworks.repo.manager.backup;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * @author deflaux
 *
 */
public interface SearchDocumentDriver {
	
	/**
	 * Create a search document, writing the results to the passed destination file.
	 * 
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

	
	/**
	 * Create a search document and return it.
	 * 
	 * @param node
	 * @param rev
	 * @param acl
	 * @return the search document for the node
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Document formulateSearchDocument(Node node, NodeRevisionBackup rev,
			AccessControlList acl, EntityPath entityPath) throws DatastoreException, NotFoundException;
	

}