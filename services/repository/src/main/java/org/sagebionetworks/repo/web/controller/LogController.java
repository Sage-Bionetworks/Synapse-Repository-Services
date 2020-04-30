package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.LogEntry;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * https://sagebionetworks.jira.com/wiki/display/PLFM/Repository+Service+API#RepositoryServiceAPI-QueryAPI
 * 
 * 
 */
@ControllerInfo(displayName = "Log Service", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class LogController {

	@Autowired
	private ServiceProvider serviceProvider;

	/**
	 * Logs the entry in the Synapse service logs. Clients can use this to log errors that the service should know
	 * about.
	 * 
	 * @param userId The user's id.
	 * @param log The log entry to log.
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.LOG, method = RequestMethod.POST)
	public void log(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @RequestBody LogEntry logEntry,
			HttpServletRequest request, @RequestHeader("User-Agent") String userAgent) {
		serviceProvider.getLogService().log(logEntry, userAgent);
	}
}
