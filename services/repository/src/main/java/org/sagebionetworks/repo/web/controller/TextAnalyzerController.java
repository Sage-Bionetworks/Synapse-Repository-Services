package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.table.search.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.table.search.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.table.search.TextAnalyzer;
import org.sagebionetworks.repo.service.search.TextAnalyzerService;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Services for managing text analyzer configurations used in search indexes.
 */
@ControllerInfo(displayName = "Text Analyzer Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class TextAnalyzerController {

	@Autowired
	private TextAnalyzerService textAnalyzerService;

	/**
	 * Create a new text analyzer.
	 *
	 * @param userId The user creating the analyzer
	 * @param request The text analyzer to create
	 * @return The created text analyzer
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER, method = RequestMethod.POST)
	public @ResponseBody TextAnalyzer create(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody TextAnalyzer request) {
		return textAnalyzerService.create(userId, request);
	}

	/**
	 * Get a text analyzer by its ID.
	 *
	 * @param userId The user making the request
	 * @param id The text analyzer ID
	 * @return The text analyzer
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER_ID, method = RequestMethod.GET)
	public @ResponseBody TextAnalyzer get(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id) {
		return textAnalyzerService.get(userId, id);
	}

	/**
	 * Update a text analyzer.
	 *
	 * @param userId The user updating the analyzer
	 * @param id The text analyzer ID
	 * @param request The updated text analyzer
	 * @return The updated text analyzer
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER_ID, method = RequestMethod.PUT)
	public @ResponseBody TextAnalyzer update(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id,
			@RequestBody TextAnalyzer request) {
		String idString = String.valueOf(id);
		if (!idString.equals(request.getId())) {
			throw new IllegalArgumentException(
				"The path ID: " + idString + " does not match the request body's ID: " + request.getId());
		}
		return textAnalyzerService.update(userId, request);
	}

	/**
	 * Delete a text analyzer.
	 *
	 * @param userId The user deleting the analyzer
	 * @param id The text analyzer ID
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER_ID, method = RequestMethod.DELETE)
	public void delete(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id) {
		textAnalyzerService.delete(userId, id);
	}

	/**
	 * List text analyzers. Returns all analyzers if no organizationId is provided.
	 *
	 * @param userId The user making the request
	 * @param request The list request
	 * @return The list response with results and optional pagination token
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER_LIST, method = RequestMethod.POST)
	public @ResponseBody ListTextAnalyzersResponse list(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ListTextAnalyzersRequest request) {
		return textAnalyzerService.list(userId, request);
	}
}
