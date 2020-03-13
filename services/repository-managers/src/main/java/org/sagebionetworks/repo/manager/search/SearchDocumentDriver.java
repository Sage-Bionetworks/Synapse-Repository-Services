package org.sagebionetworks.repo.manager.search;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.IdAndAlias;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * @author deflaux
 *
 */
public interface SearchDocumentDriver {

	/**
	 * Returns true if the entity exists in the repository. false otherwise.
	 * @param entityId id of the entity
	 * @return true if the entity exists in the repository. false otherwise.
	 */
	boolean doesEntityExistInRepository(String entityId);

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
	public Document formulateSearchDocument(Node node, Annotations annos,
											AccessControlList acl, String wikiPagesText) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 */
	public EntityPath getEntityPath(String nodeId) throws NotFoundException;
	
	/**
	 * 
	 * @param nodeIds
	 * @return
	 */
	public List<IdAndAlias> getAliases(List<String> nodeIds);
	
	
	public String getAllWikiPageText(String nodeId) throws DatastoreException;
	

}