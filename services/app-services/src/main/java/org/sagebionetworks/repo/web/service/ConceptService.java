package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptResponsePage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpHeaders;

public interface ConceptService {

	/**
	 * Get the children concepts for a parent concept.
	 * @param summaryRequest
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	public ConceptResponsePage getConceptsForParent(String id,
			String prefixFilter, Integer offset, Integer limit,
			HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, NotFoundException, IOException;

	/**
	 * Get a concept using its id.
	 * @param summaryRequest
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	public Concept getConcept(String id, HttpHeaders header,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, IOException;

}