package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptResponsePage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The web controller for the concept services.
 * 
 * @author John
 *
 */
@Controller
public class ConceptController extends BaseController {
	
	@Autowired
	ServiceProvider serviceProvider;
	
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
			UrlHelpers.CONCEPT_ID_CHILDERN_TRANSITIVE
			}, method = RequestMethod.GET)
	public @ResponseBody
	ConceptResponsePage getConceptsForParent(
			@PathVariable String id, 
			@RequestParam(value = UrlHelpers.PREFIX_FILTER, required = false) String prefixFilter,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getConceptService().getConceptsForParent(id, prefixFilter, offset, limit, header, request);
	}
	
	
	/**
	 * Get a concept using its id.
	 * @param summaryRequest
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.CONCEPT_ID
			}, method = RequestMethod.GET)
	public @ResponseBody
	Concept getConcept(
			@PathVariable String id, 
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getConceptService().getConcept(id, header, request);
	}

}
