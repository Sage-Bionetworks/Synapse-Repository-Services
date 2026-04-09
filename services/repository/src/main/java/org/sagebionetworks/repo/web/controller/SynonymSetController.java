package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsRequest;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsResponse;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.service.search.SynonymSetService;
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
 * Services for managing synonym sets used in search configurations.
 */
@ControllerInfo(displayName = "Synonym Set Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SynonymSetController {

	@Autowired
	private SynonymSetService synonymSetService;

	/**
	 * Create a new synonym set.
	 *
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET, method = RequestMethod.POST)
	public @ResponseBody SynonymSet create(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SynonymSet request) {
		return synonymSetService.create(userId, request);
	}

	/**
	 * Get a synonym set by its ID.
	 *
	 * @param userId
	 * @param synonymSetId
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET_ID, method = RequestMethod.GET)
	public @ResponseBody SynonymSet get(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String synonymSetId) {
		return synonymSetService.get(userId, synonymSetId);
	}

	/**
	 * Update a synonym set.
	 *
	 * @param userId
	 * @param synonymSetId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET_ID, method = RequestMethod.PUT)
	public @ResponseBody SynonymSet update(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String synonymSetId,
			@RequestBody SynonymSet request) {
		if (!synonymSetId.equals(request.getId())) {
			throw new IllegalArgumentException(
				"The path ID: " + synonymSetId + " does not match the request body's ID: " + request.getId());
		}
		return synonymSetService.update(userId, request);
	}

	/**
	 * Delete a synonym set.
	 *
	 * @param userId
	 * @param synonymSetId
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET_ID, method = RequestMethod.DELETE)
	public void delete(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String synonymSetId) {
		synonymSetService.delete(userId, synonymSetId);
	}

	/**
	 * List synonym sets.
	 *
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET_LIST, method = RequestMethod.POST)
	public @ResponseBody ListSynonymSetsResponse list(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ListSynonymSetsRequest request) {
		return synonymSetService.list(userId, request);
	}
}
