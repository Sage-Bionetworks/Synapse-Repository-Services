package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.web.UrlHelpers.ID_PATH_VARIABLE;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.questionnaire.PassingRecord;
import org.sagebionetworks.repo.model.questionnaire.Questionnaire;
import org.sagebionetworks.repo.model.questionnaire.QuestionnaireResponse;
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
 * To become a Synapse Certified User you must pass a test.  The Synapse APIs include
 * a service to provide the test and a service to submit a test result.  There are also
 * administrative services to retrieve the history of test submissions.
 * 
 * @author brucehoff
 *
 */
@ControllerInfo(displayName="Team Services", path="repo/v1")
@Controller
public class CertifiedUserController {
	@Autowired
	ServiceProvider serviceProvider;
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_TEST, method = RequestMethod.GET)
	public @ResponseBody
	Questionnaire getCertificationQuestionnaire()  {
		return serviceProvider.getCertifiedUserService().getCertificationQuestionnaire();
	}


	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_TEST_RESPONSE, method = RequestMethod.POST)
	public @ResponseBody
	QuestionnaireResponse submitCertificationQuestionnaireResponse(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody QuestionnaireResponse response
			) throws NotFoundException {
		return serviceProvider.getCertifiedUserService().
				submitCertificationQuestionnaireResponse(userId, response);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_TEST_RESPONSE, method = RequestMethod.GET)
	public @ResponseBody 
	PaginatedResults<QuestionnaireResponse> getQuestionnaireResponses(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PRINCIPAL_ID, required = false) Long principalId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset
			) throws NotFoundException {
		return serviceProvider.getCertifiedUserService().getQuestionnaireResponses(userId, principalId, limit, offset);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_TEST_RESPONSE_WITH_ID, method = RequestMethod.DELETE)
	public void deleteQuestionnaireResponse(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = ID_PATH_VARIABLE) Long responseId
			) throws NotFoundException {
		serviceProvider.getCertifiedUserService().deleteQuestionnaireResponse(userId, responseId);	
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CERTIFIED_USER_PASSING_RECORD_WITH_ID, method = RequestMethod.GET)
	public PassingRecord getPassingRecord(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = ID_PATH_VARIABLE) Long principalId
			) throws NotFoundException {
		return serviceProvider.getCertifiedUserService().getPassingRecord(userId, principalId);
	}
}
