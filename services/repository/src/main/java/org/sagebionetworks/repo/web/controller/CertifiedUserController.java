package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;
import static org.sagebionetworks.repo.web.UrlHelpers.ID_PATH_VARIABLE;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
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
 * To become a Synapse Certified User you must pass a test.  The Synapse APIs include
 * a service to provide the test and a service to submit a test result.  There are also
 * administrative services to retrieve the history of test submissions.
 *
 */
@ControllerInfo(displayName="Certified User Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class CertifiedUserController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Get the test to become a Certified User.
	 * @param userId
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_TEST, method = RequestMethod.GET)
	public @ResponseBody
	Quiz getCertificationQuiz(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) {
		return serviceProvider.getCertifiedUserService().getCertificationQuiz(userId);
	}

	/**
	 * Submit a response to the Certified User test.
	 * 
	 * @param userId
	 * @param response
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_TEST_RESPONSE, method = RequestMethod.POST)
	public @ResponseBody
	PassingRecord submitCertificationQuizResponse(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody QuizResponse response
			) throws NotFoundException {
		return serviceProvider.getCertifiedUserService().
				submitCertificationQuizResponse(userId, response);
	}

	@RequiredScope({view})
	@Deprecated
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_TEST_RESPONSE, method = RequestMethod.GET)
	public @ResponseBody String getQuizResponses() throws NotFoundException {
		return "This endpoint has been removed. The service has been moved to " + UrlHelpers.ADMIN + UrlHelpers.CERTIFIED_USER_TEST_RESPONSE + " and is only accessible to Synapse administrators";
	}

	/**
	 * Get the Certified User test responses in the system, optionally filtered by the one who took the test.
	 * Note:  This service is available to Synapse administrators only.
	 * @param userId
	 * @param principalId If specified, only retrieve the quiz for this user, if it exists.
	 * @param limit Limits the size of the page returned. For example, a page size of 10 requires limit = 10.
	 * @param offset The index of the pagination offset. For a page size of 10, the first page would be at offset = 0, and the second page would be at offset = 10.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN + UrlHelpers.CERTIFIED_USER_TEST_RESPONSE, method = RequestMethod.GET)
	public @ResponseBody 
	PaginatedResults<QuizResponse> getQuizResponses(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PRINCIPAL_ID, required = false) Long principalId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset
			) throws NotFoundException {
		return serviceProvider.getCertifiedUserService().getQuizResponses(userId, principalId, limit, offset);
	}

	@Deprecated
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_TEST_RESPONSE_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody String deleteQuizResponse() throws NotFoundException {
		return "This endpoint has been removed. The service has been moved to " + UrlHelpers.ADMIN + UrlHelpers.CERTIFIED_USER_TEST_RESPONSE_WITH_ID + " and is only accessible to Synapse administrators";
	}

	/**
	 * Delete a test response.  Note:  This service is available to Synapse administrators only.
	 * @param userId
	 * @param responseId
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN + UrlHelpers.CERTIFIED_USER_TEST_RESPONSE_WITH_ID, method = RequestMethod.DELETE)
	public void deleteQuizResponse(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = ID_PATH_VARIABLE) Long responseId
			) throws NotFoundException {
		serviceProvider.getCertifiedUserService().deleteQuizResponse(userId, responseId);	
	}
	
	/**
	 * Retrieve the Passing Record on the User Certification test for the given user.
	 * @param userId
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_PASSING_RECORD_WITH_ID, method = RequestMethod.GET)
	public  @ResponseBody PassingRecord getPassingRecord(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = ID_PATH_VARIABLE) Long id
			) throws NotFoundException {
		return serviceProvider.getCertifiedUserService().getPassingRecord(userId, id);
	}

	@RequiredScope({view})
	@Deprecated
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_PASSING_RECORDS_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody String getPassingRecords() throws NotFoundException {
		return "This endpoint has been removed. The service has been moved to " + UrlHelpers.ADMIN + UrlHelpers.CERTIFIED_USER_PASSING_RECORDS_WITH_ID + " and is only accessible to Synapse administrators";
	}

	/**
	 * Retrieve all the Passing Record on the User Certification test for the given user.
	 * Note:  This service is available to Synapse administrators only.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ADMIN + UrlHelpers.CERTIFIED_USER_PASSING_RECORDS_WITH_ID, method = RequestMethod.GET)
	public  @ResponseBody PaginatedResults<PassingRecord> getPassingRecords(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = ID_PATH_VARIABLE) Long id,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset
			) throws NotFoundException {
		return serviceProvider.getCertifiedUserService().getPassingRecords(userId, id, limit, offset);
	}

	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_STATUS, method = RequestMethod.PUT)
	public @ResponseBody String setUserCertificationStatus()  {
		return "This endpoint has been removed. The service has been moved to " + UrlHelpers.ADMIN + UrlHelpers.CERTIFIED_USER_STATUS + " and is only accessible to Synapse administrators";
	}

	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.ADMIN + UrlHelpers.CERTIFIED_USER_STATUS, method = RequestMethod.PUT)
	public void setUserCertificationStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = ID_PATH_VARIABLE) Long principalId,
			@RequestParam(value = AuthorizationConstants.IS_CERTIFIED) Boolean isCertified
	) throws NotFoundException {
		serviceProvider.getCertifiedUserService().setUserCertificationStatus(userId, principalId, isCertified);
	}

}
