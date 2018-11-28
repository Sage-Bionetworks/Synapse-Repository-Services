package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
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
 * 
 * These services relate to the 'commits' to Docker repositories.
 * Note that create, update and delete of the Docker repositories themselves are done using
 * the <a href="#org.sagebionetworks.repo.web.controller.EntityController">Entity Services</a>,
 * for external/unmanaged repositories, or by direct integration with the Docker registry, for managed
 * Docker repositories.  Tagged commits for both managed and external/unmanaged repositories may be
 * retrieved using the 'listDockerTags' API included in this service.
 *
 */
@Controller
@ControllerInfo(displayName="Docker Commit Services", path="repo/v1")
@RequestMapping(UrlHelpers.REPO_PATH)
public class DockerCommitController {
	@Autowired
	private ServiceProvider serviceProvider;
	
	/**
	 * Add a commit (tag and digest) for an external/unmanaged Docker repository.
	 * (Commits for managed repositories are added via direct integration with the 
	 * Synapse Docker registry.)

	 * @param userId 
	 * @param entityId the ID of the Docker repository entity
	 * @param dockerCommit the new tag/digest pair for the repository
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.ENITY_ID_DOCKER_COMMIT, method = RequestMethod.POST)
	public @ResponseBody
	void addDockerCommit(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String entityId,
			@RequestBody DockerCommit dockerCommit) {
		serviceProvider.getDockerService().addDockerCommit(userId, entityId, dockerCommit);
	}

	/**
	 * List the tagged commits (tag/digest pairs) for the given Docker repository.  Only the most recent
	 * digest for each tag is returned since, following Docker's convention, a tag may be reassigned
	 * to a newer commit. The list may be sorted by date or tag.  The default is to sort by
	 * date, descending (newest first).
	 *
	 * @param userId
	 * @param entityId the ID of the Docker repository entity
	 * @param limit pagination parameter, optional (default is 20)
	 * @param offset pagination parameter, optional (default is 0)
	 * @param sortBy TAG or CREATED_ON, optional (default is CREATED_ON)
	 * @param ascending, optional (default is false)
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_DOCKER_TAG, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<DockerCommit> listDockerTags(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.ID_PATH_VARIABLE) String entityId,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false) String sortByParam,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = "false") Boolean ascending,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Long offset
			) {
		DockerCommitSortBy sortBy = sortByParam==null ?
				DockerCommitSortBy.CREATED_ON : DockerCommitSortBy.valueOf(sortByParam);
		return serviceProvider.getDockerService().listDockerTags(userId, entityId,
				sortBy, ascending, limit, offset);
	}
}
