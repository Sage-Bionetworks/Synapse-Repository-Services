package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
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
 * <p>Discussions in Synapse are captured in the Project's Forum. Each 
 * Project has a Forum. Each Forum has a set of Moderators. The Moderators manage 
 * the content of the Forum.</p>
 * <br>
 * <p>A Forum has multiple Threads. A Thread is created by an authorized user. 
 * Other authorized users can view and reply to an existing Thread.</p>
 * <br>
 * <p>These services provide the APIs for Moderators and authorized users to 
 * create, edit, and manage the conversations that happen in Synapse.</p>
 * <br>
 */
@ControllerInfo(displayName = "Discussion Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class DiscussionController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Given the Project's ID, get the Forum's metadata.
	 * 
	 * @param userId
	 * @param projectId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.FORUM_PROJECT_ID, method = RequestMethod.GET)
	public @ResponseBody Forum getForumMetadata(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String projectId) throws DatastoreException, NotFoundException {
		return serviceProvider.getDiscussionService().getForumMetadata(userId, projectId);
	}
}
