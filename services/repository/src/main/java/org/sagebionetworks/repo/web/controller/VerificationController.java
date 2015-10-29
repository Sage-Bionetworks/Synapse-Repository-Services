package org.sagebionetworks.repo.web.controller;

import java.util.Collections;

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
 * TODO Document the purpose of verification
 *
 */
@ControllerInfo(displayName="Verification Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class VerificationController extends BaseController {
	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a VerficationSubmission.
	 * 
	 * @param userId
	 * @param verificationSubmission
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.VERIFICATION_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody
	VerificationSubmission createVerificationSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody VerificationSubmission verificationSubmission
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getVerificationService().createVerificationSubmission(userId, verificationSubmission);
	}

	/**
	 * 
	 * @param userId
	 * @param id
	 * @param newState
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.VERIFICATION_SUBMISSION_ID_STATE, method = RequestMethod.POST)
	public
	void changeVerificationSubmissionState(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id,
			@RequestBody VerificationState newState
			) throws DatastoreException, NotFoundException {
		serviceProvider.getVerificationService().changeSubmissionState(userId, id, newState);
	}

	/**
	 * 
	 * @param userId
	 * @param verifiedUserId
	 * @param currentVerificationState
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.VERIFICATION_SUBMISSION, method = RequestMethod.GET)
	public @ResponseBody VerificationPagedResults listChallengesForParticipant(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam Long verifiedUserId,
			@RequestParam VerificationStateEnum currentVerificationState,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getVerificationService().
				listVerificationSubmissions(userId, Collections.singletonList(currentVerificationState), verifiedUserId, limit, offset);
	}

}
