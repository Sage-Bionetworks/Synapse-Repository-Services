package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.ontology.ConceptManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptResponsePage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/**
 * The web controller for the concept services.
 * 
 * @author John
 */
public class ConceptServiceImpl implements ConceptService {
	
	@Autowired
	ConceptManager conceptManager;	
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	@Override
	public ConceptResponsePage getConceptsForParent(String id, String prefixFilter,
			Integer offset, Integer limit, HttpHeaders header, HttpServletRequest request) 
					throws DatastoreException, NotFoundException, IOException {
		
		int limitInt = 10;
		if(limit != null){
			limitInt = limit.intValue();
		}
		int offesetInt = 0;
		if(offset != null){
			offesetInt = offset.intValue();
		}
		String conceptUri = conceptManager.getOntologyBaseURI()+id;
//		SummaryRequest summaryRequest =  (SummaryRequest) objectTypeSerializer.deserialize(request.getInputStream(), header, SummaryRequest.class, header.getContentType());
		// Get the results from the manager
		QueryResults<Concept> eqr = conceptManager.getChildConcepts(conceptUri, prefixFilter, limitInt, offesetInt);
		ConceptResponsePage results = new ConceptResponsePage();
		results.setChildren(eqr.getResults());
		results.setParentConceptUri(conceptUri);
		results.setPrefixFilter(prefixFilter);
		results.setTotalNumberOfResults(new Long(eqr.getTotalNumberOfResults()));
		return results;
	}

	@Override
	public Concept getConcept(String id, HttpHeaders header, HttpServletRequest request) 
			throws DatastoreException, NotFoundException, IOException {
		
		String conceptUri = conceptManager.getOntologyBaseURI()+id;
		return conceptManager.getConcept(conceptUri);
	}

}
