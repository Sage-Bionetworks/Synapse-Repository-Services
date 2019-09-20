package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.statistics.ProjectStatistics;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.repo.web.service.statistics.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Services that expose statistics computed by the backend.
 */
@Controller
@ControllerInfo(displayName = "Statistics Services", path = "repo/v1")
@RequestMapping(UrlHelpers.REPO_PATH)
public class StatisticsController {

	private StatisticsService service;

	@Autowired
	public StatisticsController(ServiceProvider serviceProvider) {
		this.service = serviceProvider.getStatisticsService();
	}

	/**
	 * Returns the statistics about the project with the given id, includes the monthly file downloads and uploads
	 * statistics over the past 12 months (excluding the current month). In order to access the statistics the user should
	 * have <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">READ</a> and
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">VIEW_STATISTICS</a> access for the project with the given id.
	 * 
	 * @param projectId     The synapse id of the project
	 * @param fileDownloads True (default) if the file downloads statistics should be included in the response
	 * @param fileUploads   True (default) if the file uploads statistics should be included in the response
	 * @return The <a href="${org.sagebionetworks.repo.model.statistics.ProjectStatistics}">Project Statistics</a> computed
	 *         for the project with the given id
	 * 
	 * @throws IllegalArgumentException If any of the argument is null, or if the given projectId is malformed
	 * @throws NotFoundException        If the given projectId does not point to an existing project
	 * @throws UnauthorizedException    If the user does not have {@link ACCESS_TYPE#READ} or
	 *                                  {@link ACCESS_TYPE#VIEW_STATISTICS} access for the project with the given id
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STATISTICS_PROJECT, method = RequestMethod.GET)
	public @ResponseBody ProjectStatistics getProjectStatistics(
			// Injected
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String projectId,
			@RequestParam(defaultValue = "true") boolean fileDownloads, @RequestParam(defaultValue = "true") boolean fileUploads)
			throws NotFoundException {
		return service.getProjectStatistics(userId, projectId, fileDownloads, fileUploads);
	}

}
