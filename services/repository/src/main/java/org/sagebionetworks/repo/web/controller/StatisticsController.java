package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.repo.web.service.statistics.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
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
	 * Generic endpoint to retrieve statistics about objects. The user should have
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">READ</a> access on the object
	 * referenced by the objectId in the request.
	 * 
	 * Currently supported requests:
	 * 
	 * <ul>
	 * <li><a href=
	 * "${org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsRequest}">ProjectFilesStatisticsRequest</a>:
	 * Used to retrieve the statistics about project files, response type: <a href=
	 * "${org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsResponse}">ProjectFilesStatisticsResponse</a>
	 * </li>
	 * </ul>
	 * 
	 * @param userId  The id of the user requesting the statistics
	 * @param request The request specifying which type of statistics that should be retrieved
	 * @return An <a href=
	 *         "${org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse}">ObjectStatisticsResponse</a>
	 *         containing the statistics according to the original <a href=
	 *         "${org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest}">request</a>
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.STATISTICS, method = RequestMethod.POST)
	public @ResponseBody ObjectStatisticsResponse getObjectStatistics(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, 
			@RequestBody ObjectStatisticsRequest request) {
		return service.getStatistics(userId, request);
	}

}
