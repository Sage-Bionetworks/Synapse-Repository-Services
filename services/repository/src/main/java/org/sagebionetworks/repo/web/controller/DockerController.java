package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
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
 * 
 * These services allow Synapse to act as an authorization service for a Docker Registry.
 * For more details see: https://github.com/docker/distribution/blob/master/docs/spec/auth/token.md
 * 
 *
 */
@ControllerInfo(displayName="Docker Authorization Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class DockerController extends BaseController {
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
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOCKER_AUTHORIZATION, method = RequestMethod.GET)
	public @ResponseBody
	DockerAuthorizationToken authorizeDockerAccess(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.DOCKER_SERVICE_PARAM, required=true) String service,
			@RequestParam(value = AuthorizationConstants.DOCKER_SCOPE_PARAM, required=true) String scope
			) throws NotFoundException {
		return serviceProvider.getDockerService().authorizeDockerAccess(userId, service, scope);
	}

	/**
	 * Process Docker registry event notifications.  This service is called by the Docker registry 
	 * only and is not for general use.
	 * @param registryEvents
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.DOCKER_NOTIFICATION, method = RequestMethod.POST)
	public void processRegistryNotification(
			@RequestBody DockerRegistryEventList registryEvents
			) throws NotFoundException {
		serviceProvider.getDockerService().dockerRegistryNotification(registryEvents);
	}

	/*
	 * TODO service to add a commit to a repo.
	 * Note:  If the commit includes a tag then the current commit holding that tag must release it.
	 * Note:  This must also change modifiedBy, modifiedOn for the  entity.
	 * Note:  The commit table should have a casc
	 */
	
	/*
	 * TODO service to list the commits for a repo.
	 * Might have a param to return just the commits having tags
	 */
	
	/*
	 * Find the repo for a commit
	 */
	
	/*
	 * Create a Docker password
	 */
	
	/*
	 * Read the Docker password
	 * 
	 */
	
	/*
	 * Delete (invalidate) the Docker password
	 */

}
