package org.sagebionetworks.search.service;

import java.util.List;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * @author deflaux
 *
 */
public interface SearchDocumentDriver {
	
	/**
	 * Does the given document exist.
	 * @param nodeId
	 * @param etag
	 * @return
	 */
	public boolean doesDocumentExist(String nodeId, String etag);
	
	/**
	 * Create a search document for a given NodeId.
	 * @param nodeId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Document formulateSearchDocument(String nodeId) throws DatastoreException, NotFoundException;
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
	
	/**
	 * Add any extra return data to a a hit.
	 * @param hits
	 */
	public void addReturnDataToHits(List<Hit> hits);
	

}