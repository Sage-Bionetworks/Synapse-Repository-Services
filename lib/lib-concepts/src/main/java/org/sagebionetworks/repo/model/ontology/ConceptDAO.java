package org.sagebionetworks.repo.model.ontology;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * 
 * @author jmhill
 *
 */
public interface ConceptDAO {

	/**
	 * Get the ConceptSummary for all children
	 * @param parentConceptURI
	 * @param startFilter
	 * @param language
	 * @return
	 * @throws DatastoreException 
	 */
	public List<ConceptSummary> getAllConcepts(String parentConceptURI) throws DatastoreException;
	
	/**
	 * Get a concept for a given URI.
	 * @param conceptUri
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public Concept getConceptForUri(String conceptUri) throws DatastoreException, NotFoundException;
}
