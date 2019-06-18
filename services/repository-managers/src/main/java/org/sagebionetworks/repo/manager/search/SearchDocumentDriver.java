package org.sagebionetworks.repo.manager.search;

import java.io.IOException;
import java.util.Optional;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * @author deflaux
 *
 */
public interface SearchDocumentDriver {
	
	/**
	 * Get the etag of the entity if the entity exits.
	 * @param entityId
	 * @return Returns Optional.empty() if the entity does not exist, else the entity's etag.
	 */
	public Optional<String> getEntityEtag(String entityId);
	
	/**
	 * Create a search document for a given NodeId.
	 * @param nodeId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException TODO
	 */
	public Document formulateSearchDocument(String nodeId) throws DatastoreException, NotFoundException;
	/**
	 * Create a search document and return it.
	 *
	 * @param node
	 * @param annos
	 * @param acl
	 * @param wikiPagesText
	 * @return the search document for the node
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Document formulateSearchDocument(Node node, NamedAnnotations annos,
											AccessControlList acl, String wikiPagesText) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 */
	public EntityPath getEntityPath(String nodeId) throws NotFoundException;
	
	
	public String getAllWikiPageText(String nodeId) throws DatastoreException;
	

}