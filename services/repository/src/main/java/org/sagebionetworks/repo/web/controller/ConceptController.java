package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.ontology.ConceptManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;
import org.sagebionetworks.repo.model.ontology.ConceptSummaryResponse;
import org.sagebionetworks.repo.model.ontology.SummaryRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The web controller for the concept services.
 * 
 * @author John
 *
 */
@Controller
public class ConceptController {
	
	@Autowired
	ConceptManager conceptManager;
	
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;
	/**
	 * Get the children concepts for a parent concept.
	 * @param summaryRequest
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.CONCEPT_SUMMARY
			}, method = RequestMethod.GET)
	public @ResponseBody
	ConceptSummaryResponse getConceptsForParent(
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, NotFoundException, IOException {
		// Get the request
		SummaryRequest summaryRequest =  (SummaryRequest) objectTypeSerializer.deserialize(request.getInputStream(), header, SummaryRequest.class, header.getContentType());
		// Get the results from the manager
		List<ConceptSummary> results = conceptManager.getAllConcepts(summaryRequest.getParentConceptUri(), summaryRequest.getPrefixFilter());
		ConceptSummaryResponse response = new ConceptSummaryResponse();
		response.setChildren(results);
		response.setParentConceptUri(summaryRequest.getParentConceptUri());
		response.setPrefixFilter(summaryRequest.getPrefixFilter());
		return response;
	}

}
