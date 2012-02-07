package org.sagebionetworks.repo.manager.ontology;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction of the concept manager.
 * @author jmhill
 *
 */
public interface ConceptManager {
	
	/**
	 * Get all of the concepts for given parent concept.
	 * @param parentConceptURI - The URI of the parent concept.
	 * @param prefixFilter - (optional) When provided only concepts that have a preferred name or synonym that start with the given prefix will
	 * be returned.
	 * the preferred name or alias
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public List<ConceptSummary> getAllConcepts(String parentConceptURI, String prefixFilter) throws DatastoreException, NotFoundException;
	
	/**
	 * Get a concept for a given uri.
	 * @param URI
	 * @return
	 */
	public Concept getConcept(String uri) throws DatastoreException, NotFoundException;
	

}
