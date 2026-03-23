package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.table.search.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.table.search.ListColumnAnalyzerOverridesRequest;
import org.sagebionetworks.repo.model.table.search.ListColumnAnalyzerOverridesResponse;
import org.sagebionetworks.repo.service.search.ColumnAnalyzerOverrideService;
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
 * Services for managing column analyzer overrides used in search configurations.
 */
@ControllerInfo(displayName = "Column Analyzer Override Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class ColumnAnalyzerOverrideController {

	@Autowired
	private ColumnAnalyzerOverrideService columnAnalyzerOverrideService;

	/**
	 * Create a new column analyzer override.
	 *
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE, method = RequestMethod.POST)
	public @ResponseBody ColumnAnalyzerOverride create(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ColumnAnalyzerOverride request) {
		return columnAnalyzerOverrideService.create(userId, request);
	}

	/**
	 * Get a column analyzer override by its ID.
	 *
	 * @param userId
	 * @param columnAnalyzerOverrideId
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE_ID, method = RequestMethod.GET)
	public @ResponseBody ColumnAnalyzerOverride get(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String columnAnalyzerOverrideId) {
		return columnAnalyzerOverrideService.get(userId, columnAnalyzerOverrideId);
	}

	/**
	 * Update a column analyzer override.
	 *
	 * @param userId
	 * @param columnAnalyzerOverrideId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE_ID, method = RequestMethod.PUT)
	public @ResponseBody ColumnAnalyzerOverride update(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String columnAnalyzerOverrideId,
			@RequestBody ColumnAnalyzerOverride request) {
		if (!columnAnalyzerOverrideId.equals(request.getId())) {
			throw new IllegalArgumentException(
				"The path ID: " + columnAnalyzerOverrideId + " does not match the request body's ID: " + request.getId());
		}
		return columnAnalyzerOverrideService.update(userId, request);
	}

	/**
	 * Delete a column analyzer override.
	 *
	 * @param userId
	 * @param columnAnalyzerOverrideId
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE_ID, method = RequestMethod.DELETE)
	public void delete(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String columnAnalyzerOverrideId) {
		columnAnalyzerOverrideService.delete(userId, columnAnalyzerOverrideId);
	}

	/**
	 * List column analyzer overrides.
	 *
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE_LIST, method = RequestMethod.POST)
	public @ResponseBody ListColumnAnalyzerOverridesResponse list(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ListColumnAnalyzerOverridesRequest request) {
		return columnAnalyzerOverrideService.list(userId, request);
	}
}
