package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.util.ControllerUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 
 * <p>
 * The Evaluation API is designed to support open-access data analysis and modeling challenges in
 * Synapse. This framework provides tools for administrators to collect and analyze data models
 * from Synapse users created for a specific goal or purpose.
 * </p>
 *  
 * <p>
 * The data model of the Evaluation API is built around around three primary objects:
 * <ul>
 * <li> <a href="${org.sagebionetworks.evaluation.model.Evaluation}">Evaluation</a>: The primary
 * object representing a Synapse Evaluation. Access to Evaluations is governed by an <a
 * href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
 * List (ACL)</a>.
 * </li> 
 * <li> <a href="${org.sagebionetworks.evaluation.model.Participant}">Participant</a>: 
 * Given proper permissions, a Synapse user can register as a Participant in an Evaluation. Being a
 * Participant is a prerequisite for accessing Evaluation resources.
 * </li>
 * <li> <a href="${org.sagebionetworks.evaluation.model.Submission}">Submission</a>: A Participant
 * in a Synapse Evaluation can submit a Synapse Entity as Submission to that Evaluation. Submission
 * data is owned by the parent Evaluation, and is immutable.
 * </ul>
 * </p>
 * 
 * <p>
 * The data model includes additional objects to support scoring of Submissions and convenient data 
 * access:
 * <ul>
 * <li> <a href="${org.sagebionetworks.evaluation.model.SubmissionStatus}">SubmissionStatus</a>: An
 * object used to track scoring information for a single Submission. This object is intended to be
 * modified by the users (or test harnesses) managing the Evaluation.
 * </li> 
 * <li> <a href="${org.sagebionetworks.evaluation.model.SubmissionBundle}">SubmissionBundle</a>: 
 * A convenience object to transport a Submission and its accompanying SubmissionStatus in a single
 * web service call.
 * </li>
 * </ul>
 * </p>
 * 
 * The Evaluation API supports data access mechanisms to monitor Evaluation activity for on-demand 
 * scoring and leaderboarding.
 */
@ControllerInfo(displayName="Evaluation Services", path="repo/v1")
@Controller
public class EvaluationController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a new Evaluation. The passed request body should contain the following fields:
	 * <ul>
	 * <li>name - Give your new Evaluation a name.</li>
	 * <li>contentSource - The ID of the parent Entity, such as a Folder or Project.</li>
	 * <li>status - The initial state of the Evaluation, an 
	 * <a href="${org.sagebionetworks.evaluation.model.EvaluationStatus}">EvaluationStatus</a></li>
	 * </ul>
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CREATE</a> on the contentSource Entity.
	 * </p>
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION, method = RequestMethod.POST)
	public @ResponseBody
	Evaluation createEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Evaluation eval = new Evaluation(new JSONObjectAdapterImpl(requestBody));
		return serviceProvider.getEvaluationService().createEvaluation(userId, eval);
	}
	
	/**
	 * Get an Evaluation.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param userId
	 * @param evalId - the ID of the desired Evaluation
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Evaluation getEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String evalId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getEvaluation(userId, evalId);
	}
	
	/**
	 * Get a collection of Evaluations, within a given range.
	 * 
	 * <p>
	 * <b>Note:</b> The response will contain only those Evaluations on which the caller must is
	 * granted the <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * permission.
	 * </p> 
	 * 
	 * <b>Note:</b> This method is deprecated and should not be used.
	 * </p>
	 * 
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Evaluation> getEvaluationsPaginated(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			HttpServletRequest request
			) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getEvaluationsInRange(userId, limit, offset, request);
	}
	
	/**
	 * Get a collection of Evaluations in which the user may participate, within a given range.
	 * 
	 * <p>
	 * <b>Note:</b> The response will contain only those Evaluations on which the caller must is
	 * granted the <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.PARTICIPATE</a>
	 * permission.
	 * </p>
	 * 
	 * 
	 * <b>Note:</b> This method is deprecated and should not be used.
	 * </p>
	 * 
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param userId
	 * @param statusString
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_AVAILABLE, method = RequestMethod.GET)
	@Deprecated
	public @ResponseBody
	PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString,
			HttpServletRequest request
			) throws DatastoreException, NotFoundException
	{
		EvaluationStatus status = null;
		if (statusString.length() > 0) {
			status = EvaluationStatus.valueOf(statusString.toUpperCase().trim());
		}
		return serviceProvider.getEvaluationService().getAvailableEvaluationsInRange(userId, status, limit, offset, request);
	}	
	
	/**
	 * Get the total number of Evaluations in Synapse.
	 * 
	 * <b>Note:</b> This method is deprecated and should not be used.
	 * </p>
	 * 
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getEvaluationCount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getEvaluationCount(userId);
	}
	
	/**
	 * Find an Evaluation by name.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param name - the name of the desired Evaluation.
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws UnsupportedEncodingException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_NAME, method = RequestMethod.GET)
	public @ResponseBody
	Evaluation findEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String name,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, UnsupportedEncodingException 
	{
		String decodedName = URLDecoder.decode(name, "UTF-8");
		return serviceProvider.getEvaluationService().findEvaluation(userId, decodedName);
	}
	
	/**
	 * Update an Evaluation.
	 *  
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time an Evaluation is updated a new etag will be
	 * issued to the Evaluation. When an update is requested, Synapse will compare the
	 * etag of the passed Evaluation with the current etag of the Evaluation. If the
	 * etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs, the caller should
	 * fetch the latest copy of the Evaluation and re-apply any changes, then re-attempt
	 * the Evaluation update.
	 * </p>
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the Evaluation being updated
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.PUT)
	public @ResponseBody
	Evaluation updateEvaluation(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, InvalidModelException, ConflictingUpdateException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Evaluation eval = new Evaluation(new JSONObjectAdapterImpl(requestBody));
		if (!evalId.equals(eval.getId()))
			throw new IllegalArgumentException("Evaluation ID does not match requested ID: " + evalId);
		return serviceProvider.getEvaluationService().updateEvaluation(userId, eval);
	}
	
	/**
	 * Delete an Evaluation.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the requested Evaluation
	 * @param userId
	 * @param header
	 * @param request
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteEvaluation(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		serviceProvider.getEvaluationService().deleteEvaluation(userId, evalId);
	}
	
	/**
	 * Join as a Participant in a specified Evaluation.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.PARTICIPATE</a> on the specified Evaluation, and must have satisfied all
	 * access requirements on the Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.PARTICIPANT, method = RequestMethod.POST)
	public @ResponseBody
	Participant createParticipant(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException
	{
		return serviceProvider.getEvaluationService().addParticipant(userId, evalId);
	}
	
	/**
	 * Get a Participant. 
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param partId - the ID of the Synapse user whose participation is to be deleted
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Participant getParticipant(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String evalId,
			@PathVariable String partId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getParticipant(userId, partId, evalId);
	}
	
	/**
	 * Delete a Participant.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param partId - the ID of the Synapse user whose participation is to be deleted
	 * @param userId
	 * @param header
	 * @param request
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteParticipantAsAdmin(
			@PathVariable String evalId,
			@PathVariable String partId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException
	{
		serviceProvider.getEvaluationService().removeParticipant(userId, evalId, partId);
	}
	
	/**
	 * Get all Participants for a specified Evaluation. 
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param evalId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Participant> getAllParticipants(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@PathVariable String evalId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getAllParticipants(userId, evalId, limit, offset, request);
	}
	
	/**
	 * Get the number of Participants in a specified Evaluation.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getParticipantCount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String evalId,
			HttpServletRequest request) 
			throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getParticipantCount(userId, evalId);
	}
	
	/**
	 * Create a Submission. The passed request body should contain the following fields:
	 * <ul>
	 * <li>evaluationId - The ID of the Evaluation to which this Submission belongs.</li>
	 * <li>entityId - The ID of the Entity being submitted.</li>
	 * <li>versionNumber - The specific version of the Entity being submitted.</li>
	 * </ul>
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.SUBMIT</a> and must be registered as a Participant on the specified Evaluation.
	 * </p>
	 * 
	 * @param userId
	 * @param entityEtag - the current eTag of the Entity being submitted
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 * @throws ParseException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody
	Submission createSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = AuthorizationConstants.ETAG_PARAM, required = false) String entityEtag,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException, UnauthorizedException, ACLInheritanceException, ParseException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Submission sub = new Submission(new JSONObjectAdapterImpl(requestBody));
		return serviceProvider.getEvaluationService().createSubmission(userId, sub, entityEtag, request);
	}
	
	/**
	 * Get a Submission.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param subId
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Submission getSubmission(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getSubmission(userId, subId);
	}
	
	/**
	 * Get the SubmissionStatus object associated with a specified Submission.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the specified Evaluation. 
	 * Furthermore, the caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> to see all 
	 * data marked as "private" in the SubmissionStatus.	 * 
	 * </p>
	 * 
	 * @param subId - the ID of the requested SubmissionStatus.
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS, method = RequestMethod.GET)
	public @ResponseBody
	SubmissionStatus getSubmissionStatus(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getSubmissionStatus(userId, subId);
	}
	
	/**
	 * Update a SubmissionStatus object.
	 *  
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time an SubmissionStatus is updated a new etag will be
	 * issued to the SubmissionStatus. When an update is requested, Synapse will compare the
	 * etag of the passed SubmissionStatus with the current etag of the SubmissionStatus. If the
	 * etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs, the caller should
	 * fetch the latest copy of the SubmissionStatus and re-apply any changes, then re-attempt
	 * the SubmissionStatus update.
	 * </p>
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param subId - the ID of the SubmissionStatus being updated.
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS, method = RequestMethod.PUT)
	public @ResponseBody
	SubmissionStatus updateSubmissionStatus(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) 
			throws DatastoreException, UnauthorizedException, InvalidModelException, 
			ConflictingUpdateException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		SubmissionStatus status = new SubmissionStatus(new JSONObjectAdapterImpl(requestBody));
		if (!subId.equals(status.getId()))
			throw new IllegalArgumentException("Submission ID does not match requested ID: " + subId);
		return serviceProvider.getEvaluationService().updateSubmissionStatus(userId, status);
	}
	
	/**	
	 * Delete a Submission and its accompanying SubmissionStatus.
	 * Use of this service is discouraged, since Submissions should be immutable.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param subId - the ID of the Submission to be deleted.
	 * @param userId
	 * @param header
	 * @param request
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteSubmission(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		serviceProvider.getEvaluationService().deleteSubmission(userId, subId);
	}
	
	/**
	 * Get a collection of Submissions to a specified Evaluation.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param userId
	 * @param statusString
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_ADMIN, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Submission> getAllSubmissions(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}		
		return serviceProvider.getEvaluationService().getAllSubmissions(userId, evalId, status, limit, offset, request);
	}
	
	/**
	 * Get a collection of SubmissionStatuses to a specified Evaluation.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the specified Evaluation. 
	 * Furthermore, the caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> to see all 
	 * data marked as "private" in the SubmissionStatuses.
	 * </p> 
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param statusString
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS_WITH_EVAL_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}
		return serviceProvider.getEvaluationService().getAllSubmissionStatuses(userId, evalId, status, limit, offset, request);
	}
	
	/**
	 * Get a collection of bundled Submissions and SubmissionStatuses to a given Evaluation. 
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param userId
	 * @param statusString
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_ADMIN_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionBundle> getAllSubmissionBundles(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}		
		return serviceProvider.getEvaluationService().getAllSubmissionBundles(userId, evalId, status, limit, offset, request);
	}
	
	/**
	 * Get the requesting user's Submissions to a specified Evaluation.
	 * 
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Submission> getMySubmissions(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getAllSubmissionsByEvaluationAndUser(evalId, userId, limit, offset, request);
	}
	
	/**
	 * Get the requesting user's bundled Submissions and SubmissionStatuses to a specified
	 * Evaluation.
	 * 
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionBundle> getMySubmissionBundles(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getAllSubmissionBundlesByEvaluationAndUser(evalId, userId, limit, offset, request);
	}
	
	/**
	 * Get a pre-signed URL to access a requested File contained within a specified Submission.
	 * 
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param userInfo
	 * @param submissionId - the ID of the specified Submission.
	 * @param fileHandleId - the ID of the requested FileHandle contained in the Submission.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void redirectURLForFileHandle(
			@PathVariable String subId,
			@PathVariable String fileHandleId,
			@RequestParam (required = false) Boolean redirect,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletResponse response
			) throws DatastoreException, NotFoundException, IOException {
		URL url = serviceProvider.getEvaluationService().getRedirectURLForFileHandle(userId, subId, fileHandleId);
		RedirectUtils.handleRedirect(redirect, url, response);
	}
	
	/**
	 * Get the number of Submissions to a specified Evaluation.
	 * 
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getSubmissionCount(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getSubmissionCount(userId, evalId);
	}
	
	/**
	 * Determine whether a specified Synapse user has a certain 
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE</a> 
	 * on the specified Evaluation.
	 * </p>
	 * 
	 * @param id 
	 * @param userId 
	 * @param accessType 
	 * @param request 
	 * @return the access types that the given user has to the given resource
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value={UrlHelpers.EVALUATION_WITH_ID+UrlHelpers.ACCESS}, method=RequestMethod.GET)
	public @ResponseBody BooleanResult hasAccess(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false) String accessType,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		// pass it along.
		return new BooleanResult(serviceProvider.getEvaluationService().hasAccess(evalId, userId, request, accessType));
	}

	/**
	 * Create a new ACL.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION_ACL, method = RequestMethod.POST)
	public @ResponseBody AccessControlList
	createAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestBody AccessControlList acl,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		return serviceProvider.getEvaluationService().createAcl(userId, acl);
	}

	/**
	 * Update the given ACL.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ACL, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList
	updateAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestBody AccessControlList acl,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		return serviceProvider.getEvaluationService().updateAcl(userId, acl);
	}

	/**
	 * Delete the ACL of the specified evaluation.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_ACL, method = RequestMethod.DELETE)
	public void deleteAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable String evalId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		serviceProvider.getEvaluationService().deleteAcl(userId, evalId);
	}

	/**
	 * Get the access control list (ACL) governing the given evaluation.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_ACL, method = RequestMethod.GET)
	public @ResponseBody AccessControlList
	getAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable String evalId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, ACLInheritanceException {
		return serviceProvider.getEvaluationService().getAcl(userId, evalId);
	}

	/**
	 * Gets the user permissions for the specified evaluation.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_PERMISSIONS, method = RequestMethod.GET)
	public @ResponseBody UserEvaluationPermissions
	getUserPermissionsForEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@PathVariable String evalId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException {
		return serviceProvider.getEvaluationService().getUserPermissionsForEvaluation(userId, evalId);
	}
	
	/**
	 * Execute a user-defined query over the Submissions of a specific Evaluation. Queries may be of
	 * the following form:
	 * 
	 * <p>
	 * SELECT * FROM evaluation_123 WHERE myAnnotation == "bar";
	 * </p>
	 * 
	 * <p>
	 * <b>Note:</b> This service is still under construction. Query syntax and behavior are subject 
	 * to change.
	 * </p>
	 * 
	 * @throws JSONObjectAdapterException
	 * @throws ParseException 
	 * @throws  
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_QUERY, method = RequestMethod.GET)
	public @ResponseBody 
	QueryTableResults query(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(value = ServiceConstants.QUERY_PARAM, required = true) String query,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, ParseException, 
			JSONObjectAdapterException {
		return serviceProvider.getEvaluationService().query(query, userId);
	}
}
