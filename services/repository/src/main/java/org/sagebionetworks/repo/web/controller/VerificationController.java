package org.sagebionetworks.repo.web.controller;

import java.util.Collections;
import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
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
 * Identity verification is a service offered by the Synapse Access and Compliance Team
 * to add an additional layer of legitimacy to a user account, beyond the basic
 * requirements for creating an account in Synapse.  After completing their user
 * profile a user may submit a verification request, including supporting documentation.
 * The ACT reviews the information then approves or rejects it.  After approval, the
 * ACT retains the authority to suspend verification of an account previously verified.
 * Once rejected or suspended a user may create a new verification request.
 *
 */
@ControllerInfo(displayName="Verification Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class VerificationController {
	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a VerficationSubmission.  The content must match that of the user's
	 * profile.  Notification is the request is sent to the Synapse Access and 
	 * Compliance Team.
	 * 
	 * @param userId
	 * @param verificationSubmission the object holding the submitted verification content
	 * @param notificationUnsubscribeEndpointthe portal prefix for one-click email unsubscription
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.VERIFICATION_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody
	VerificationSubmission createVerificationSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody VerificationSubmission verificationSubmission,
			@RequestParam(value = AuthorizationConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM, required = false) String notificationUnsubscribeEndpoint
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getVerificationService().createVerificationSubmission(userId, verificationSubmission, notificationUnsubscribeEndpoint);
	}

	/**
	 * Update the state of a verification submission.  The allowed transitions are:
	 * <ul>
	 * <li>	Submitted -> Approved </li>
	 * <li>	Submitted -> Rejected </li>
	 * <li>	Approved  -> Suspended </li>
	 * </ul>
	 * Notification is sent to the user who requested verification.
	 * 
	 * @param userId
	 * @param id the ID of the verification submission
	 * @param newState the state to which the verification submission is to be set
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.VERIFICATION_SUBMISSION_ID_STATE, method = RequestMethod.POST)
	public
	void changeVerificationSubmissionState(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id,
			@RequestBody VerificationState newState,
			@RequestParam(value = AuthorizationConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM, required = false) String notificationUnsubscribeEndpoint
			) throws DatastoreException, NotFoundException {
		serviceProvider.getVerificationService().changeSubmissionState(userId, id, newState, notificationUnsubscribeEndpoint);
	}
	
	/**
	 * Delete a verification submission by its ID.
	 * Note:  This service may be called only by the original verification requestor.
	 * @param userId
	 * @param id
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.VERIFICATION_SUBMISSION_ID, method=RequestMethod.DELETE)
	public void deleteVerificationSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id
	) {
		serviceProvider.getVerificationService().deleteVerificationSubmission(userId, id);
	}

	/**
	 * List the verification submissions in the system.  This service is available only
	 * to the Synapse Access and Compliance Team.  Submissions may be filtered by the
	 * requesting user and/or the submission state.
	 * 
	 * @param userId
	 * @param verifiedUserId filter on the user requesting verification (optional)
	 * @param currentVerificationState filter on the state of the verification submission (optional)
	 * @param limit page size pagination parameter (optional)
	 * @param offset page start pagination parameter (zero offset, optional)
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.VERIFICATION_SUBMISSION, method = RequestMethod.GET)
	public @ResponseBody VerificationPagedResults listVerificationSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = false) Long verifiedUserId,
			@RequestParam(required = false) VerificationStateEnum currentVerificationState,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset
			) throws DatastoreException, NotFoundException {
		List<VerificationStateEnum> currentVerificationStateList = null;
		if (currentVerificationState!=null) {
			currentVerificationStateList = 
				Collections.singletonList(currentVerificationState);
		}
		return serviceProvider.getVerificationService().
				listVerificationSubmissions(userId, currentVerificationStateList, verifiedUserId, limit, offset);
	}

}
