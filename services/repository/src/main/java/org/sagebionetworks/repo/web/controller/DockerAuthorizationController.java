package org.sagebionetworks.repo.web.controller;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * These services allow Synapse to act as an authorization service for a Docker Registry.
 * For more details see: https://github.com/docker/distribution/blob/master/docs/spec/auth/token.md
 * 
 *
 */
@ControllerInfo(displayName="Docker Authorization Services", path="docker/v1")
@Controller
@RequestMapping(UrlHelpers.DOCKER_PATH)
public class DockerAuthorizationController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Authorize Docker operation.  This service is called by the Docker client only and is not for general use.
	 * @param userId
	 * @param service
	 * @param scope
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOCKER_AUTHORIZATION, method = RequestMethod.GET)
	public @ResponseBody
	DockerAuthorizationToken authorizeDockerAccess(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.DOCKER_SERVICE_PARAM, required=true) String service,
			@RequestParam(value = AuthorizationConstants.DOCKER_SCOPE_PARAM, required=false) List<String> scopes
			) throws NotFoundException {
		return serviceProvider.getDockerService().authorizeDockerAccess(userId, service, scopes);
	}
}
