package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundListRequest;
import org.sagebionetworks.evaluation.model.EvaluationRoundListResponse;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.DeprecatedServiceException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
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
 * <p>
 * The Evaluation API is designed to support open-access data analysis and modeling challenges in
 * Synapse. This framework provides tools for administrators to collect and analyze data models
 * from Synapse users created for a specific goal or purpose.
 * </p>
 *  
 * <p>
 * The data model of the Evaluation API is built around around two primary objects:
 * <ul>
 * <li> <a href="${org.sagebionetworks.evaluation.model.Evaluation}">Evaluation</a>: The primary
 * object representing a Synapse Evaluation. Access to Evaluations is governed by an <a
 * href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
 * List (ACL)</a>.
 * </li> 
 * <li> <a href="${org.sagebionetworks.evaluation.model.Submission}">Submission</a>: A user
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
 * <li> <a href="${org.sagebionetworks.repo.model.table.SubmissionView}">SubmissionView</a>: 
 * A submission view can be created using the 
 * <a href="${org.sagebionetworks.repo.web.controller.EntityController}">Entity Services</a> providing
 * as scope a list of evaluation ids, in order to query the set of submissions through 
 * the <a href="${POST.entity.id.table.query.async.start}">Table Query Services</a>.
 * <a href="${org.sagebionetworks.repo.model.annotation.v2.Annotations}">Annotations</a> set in 
 * the submissionAnnotations property of a <a href="${org.sagebionetworks.evaluation.model.SubmissionStatus}">SubmissionStatus</a> 
 * can be exposed in the view.
 * </li>
 * </ul>
 * </p>
 * 
 * The Evaluation API supports data access mechanisms to monitor Evaluation activity for on-demand 
 * scoring and leaderboarding.
 */
@ControllerInfo(displayName="Evaluation Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class EvaluationController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Creates a new Evaluation. The passed request body should contain the following fields:
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
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION, method = RequestMethod.POST)
	public @ResponseBody
	Evaluation createEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Evaluation evaluation) throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException
	{
		return serviceProvider.getEvaluationService().createEvaluation(userId, evaluation);
	}
	
	/**
	 * Gets an Evaluation.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param userId
	 * @param evalId - the ID of the desired Evaluation
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Evaluation getEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getEvaluation(userId, evalId);
	}
	
	/**
	 * Gets Evaluations tied to a project. 
	 * 
	 * <b>Note:</b> The response will contain only those Evaluations on which the caller is
	 * granted the <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * permission, unless specified otherwise with the accessType parameter.
	 * 
	 * @param id the ID of the project
	 * @param accessType The type of access for the user to filter for, optional and defaults to <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * @param activeOnly If 'true' then return only those evaluations with rounds defined and for which the current time is in one of the rounds.
	 * @param evaluationIds an optional, comma-delimited list of evaluation IDs to which the response is limited
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 0 is the first entity. When null it will default
	 *            to 0.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_CONTENT_SOURCE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Evaluation> getEvaluationsByContentSourcePaginated(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, 
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false, defaultValue="READ") ACCESS_TYPE accessType,
			@RequestParam(value = ServiceConstants.ACTIVE_ONLY_PARAM, required=false, defaultValue="false") boolean activeOnly,
			@RequestParam(value = ServiceConstants.EVALUATION_IDS_PARAM, required = false) String evaluationIds,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit
			) throws DatastoreException, NotFoundException
	{
		List<Long> ids = stringToEvaluationIds(evaluationIds);
		
		return serviceProvider.getEvaluationService().getEvaluationByContentSource(userId, id, accessType, activeOnly, ids, limit, offset);
	}
	
	/**
	 * Gets a collection of Evaluations, within a given range.
	 * 
	 * <p>
	 * <b>Note:</b> The response will contain only those Evaluations on which the caller is
	 * granted the <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * permission, unless specified otherwise with the accessType parameter.
	 * </p> 
	 * 
	 * @param accessType The type of access for the user to filter for, optional and defaults to <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.READ</a>
	 * @param activeOnly If 'true' then return only those evaluations with rounds defined and for which the current time is in one of the rounds.
	 * @param evaluationIds an optional, comma-delimited list of evaluation IDs to which the response is limited
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 0 is the first entity. When null it will default
	 *            to 0.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Evaluation> getEvaluationsPaginated(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false, defaultValue="READ") ACCESS_TYPE accessType,
			@RequestParam(value = ServiceConstants.ACTIVE_ONLY_PARAM, required=false, defaultValue="false") boolean activeOnly,
			@RequestParam(value = ServiceConstants.EVALUATION_IDS_PARAM, required = false) String evaluationIds,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit
			) throws DatastoreException, NotFoundException {
		
		List<Long> ids = stringToEvaluationIds(evaluationIds);
		
		return serviceProvider.getEvaluationService().getEvaluations(userId, accessType, activeOnly, ids, limit, offset);
	}
	
	/**
	 * Gets a collection of Evaluations in which the user has SUBMIT permission, within a given range.
	 * 
	 * <p>
	 * <b>Note:</b> The response will contain only those Evaluations on which the caller must is
	 * granted the <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.SUBMIT</a>
	 * permission.
	 * </p>
	 * 
	 * </p>
	 * 
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 0 is the first evaluation. When null it will default
	 *            to 0.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param userId
	 * @param evaluationIds an optional, comma-delimited list of evaluation IDs to which the response is limited
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_AVAILABLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Evaluation> getAvailableEvaluationsPaginated(
			@RequestParam(value = ServiceConstants.ACTIVE_ONLY_PARAM, required=false, defaultValue="false") boolean activeOnly,
			@RequestParam(value = ServiceConstants.EVALUATION_IDS_PARAM, required = false) String evaluationIds,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws DatastoreException, NotFoundException
	{
		
		List<Long> ids = stringToEvaluationIds(evaluationIds);
		
		return serviceProvider.getEvaluationService().getAvailableEvaluations(userId, activeOnly, ids, limit, offset);
	}

	/**
	 * Finds an Evaluation by name.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param name - the name of the desired Evaluation.
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws UnsupportedEncodingException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_NAME, method = RequestMethod.GET)
	public @ResponseBody
	Evaluation findEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String name
			) throws DatastoreException, UnauthorizedException, NotFoundException, UnsupportedEncodingException
	{
		String decodedName = URLDecoder.decode(name, "UTF-8");
		return serviceProvider.getEvaluationService().findEvaluation(userId, decodedName);
	}
	
	/**
	 * Updates an Evaluation.
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
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.PUT)
	public @ResponseBody
	Evaluation updateEvaluation(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Evaluation evaluation) throws DatastoreException, UnauthorizedException, InvalidModelException, ConflictingUpdateException, NotFoundException, JSONObjectAdapterException
	{
		if (!evalId.equals(evaluation.getId()))
			throw new IllegalArgumentException("Evaluation ID does not match requested ID: " + evalId);
		return serviceProvider.getEvaluationService().updateEvaluation(userId, evaluation);
	}
	
	/**
	 * Deletes an Evaluation.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the requested Evaluation
	 * @param userId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.EVALUATION_WITH_ID, method = RequestMethod.DELETE)
	public void deleteEvaluation(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws DatastoreException, UnauthorizedException, NotFoundException
	{
		serviceProvider.getEvaluationService().deleteEvaluation(userId, evalId);
	}

	/**
	 * Find out whether a Team and its members are eligible to submit to a given Evaluation queue (at the 
	 * current time).  The request must include an Evaluation ID and a Team ID.   The 'eligibilityStateHash' 
	 * field of the returned object is a required parameter of the subsequent Team Submission request made
	 * for the given Evaluation and Team.  (See: <a href="${POST.evaluation.submission}">POST
	 * /evaluation/submission</a>)
	 * @param userId
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM_SUBMISSION_ELIGIBILITY, method = RequestMethod.GET)
	public @ResponseBody
	TeamSubmissionEligibility getTeamSubmissionEligibility(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId,
			@PathVariable String id) 
			throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getTeamSubmissionEligibility(userId, evalId, id);
	}
	
	/**
	 * Creates a Submission and sends a submission notification email to the submitter's team members.
	 * 
	 * The passed request body should contain the following fields:
	 * <ul>
	 * <li>evaluationId - The ID of the Evaluation to which this Submission belongs.</li>
	 * <li>entityId - The ID of the Entity being submitted.</li>
	 * <li>versionNumber - The specific version of the Entity being submitted.</li>
	 * </ul>
	 * <p>
	 * A Submission must be either a Team or an Individual submission.  A Team submission must 
	 * include a Team ID in the teamId field and the request must include a submissionEligibilityHash
	 * request parameter.  A Team submission may also include a list of submission contributors.
	 * (The submitter is taken to be a contributor and need not be included in the list.)
	 * An individual submission must have a null teamId, a null or empty contributor list, and no
	 * submissionEligibilityHash parameter.
	 * </p>
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.SUBMIT</a>.
	 * </p>
	 * <p>
	 * This call also creates an associated <a href="${org.sagebionetworks.evaluation.model.SubmissionStatus}">SubmissionStatus</a>, 
	 * initialized with a SubmissionStatusEnum value of RECEIVED.
	 * </p>
	 * 
	 * @param userId
	 * @param entityEtag The current eTag of the Entity being submitted
	 * @param submissionEligibilityHash The hash provided by the
	 * <a href="${org.sagebionetworks.evaluation.model.TeamSubmissionEligibility}">TeamSubmissionEligibility</a>
	 * object.
	 * @param challengeEndpoint The portal endpoint prefix to the an entity/challenge page. The entity ID of the
	 * challenge project is appended to create the complete URL. In normal operation, this parameter should be omitted.
	 * @param notificationUnsubscribeEndpoint The portal endpoint prefix for one-click email unsubscription.
	 * A signed, serialized token is appended to create the complete URL:
	 * <a href="${org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken}">NotificationSettingsSignedToken</a>.
	 * In normal operation, this parameter should be omitted.
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 * @throws ParseException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody
	Submission createSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.ETAG_PARAM, required = false) String entityEtag,
			@RequestParam(value = AuthorizationConstants.SUBMISSION_ELIGIBILITY_HASH_PARAM, required = false) String submissionEligibilityHash,
			@RequestParam(value = AuthorizationConstants.CHALLENGE_ENDPOINT_PARAM, defaultValue = ServiceConstants.CHALLENGE_ENDPOINT) String challengeEndpoint,
			@RequestParam(value = AuthorizationConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM, defaultValue = ServiceConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT) String notificationUnsubscribeEndpoint,
			@RequestBody Submission submission
			) throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException, UnauthorizedException, ACLInheritanceException, ParseException
	{
		return serviceProvider.getEvaluationService().createSubmission(
				userId, submission, entityEtag, submissionEligibilityHash, challengeEndpoint, notificationUnsubscribeEndpoint);
	}

	@Deprecated
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = UrlHelpers.SUBMISSION_CONTRIBUTOR, method = RequestMethod.POST)
	public @ResponseBody
	String addSubmissionContributor() {
		return "This endpoint has been removed. The service has been moved to " + UrlHelpers.ADMIN + UrlHelpers.SUBMISSION_CONTRIBUTOR + " and is only accessible to Synapse administrators";
	}

	/**
	 * Add a contributor to an existing Submission.  This service is available to administrators only.
	 *
	 * @param userId
	 * @param subId
	 * @param submissionContributor
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequiredScope({view,modify})
	@RequestMapping(value = UrlHelpers.ADMIN + UrlHelpers.SUBMISSION_CONTRIBUTOR, method = RequestMethod.POST)
	public @ResponseBody
	SubmissionContributor addSubmissionContributor(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String subId,
			@RequestBody SubmissionContributor submissionContributor
			) throws DatastoreException, InvalidModelException, NotFoundException {
		return serviceProvider.getEvaluationService().addSubmissionContributor(userId, subId, submissionContributor);
	}

	/**
	 * Gets a Submission.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param subId
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Submission getSubmission(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws DatastoreException, UnauthorizedException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getSubmission(userId, subId);
	}
	
	/**
	 * Gets the SubmissionStatus object associated with a specified Submission.
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
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum frequency this method can be called</td>
	 * <td>1 calls per second</td>
	 * </tr>
	 * </table>
	 * </p>
	 * @param subId - the ID of the requested SubmissionStatus.
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS, method = RequestMethod.GET)
	public @ResponseBody
	SubmissionStatus getSubmissionStatus(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws DatastoreException, UnauthorizedException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getSubmissionStatus(userId, subId);
	}
	
	/**
	 * Updates a SubmissionStatus object.
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
	 * </p>
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum frequency this method can be called</td>
	 * <td>1 calls per second</td>
	 * </tr>
	 * </table>
	 * </p>
	 * @param subId - the ID of the SubmissionStatus being updated.
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS, method = RequestMethod.PUT)
	public @ResponseBody
	SubmissionStatus updateSubmissionStatus(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionStatus status)
			throws DatastoreException, UnauthorizedException, InvalidModelException, 
			ConflictingUpdateException, NotFoundException, JSONObjectAdapterException
	{
		if (!subId.equals(status.getId()))
			throw new IllegalArgumentException("Submission ID does not match requested ID: " + subId);
		return serviceProvider.getEvaluationService().updateSubmissionStatus(userId, status);
	}
	
	/**
	 * Update multiple SubmissionStatuses. The maximum batch size is 500.  To allow upload
	 * of more than this maximum, the system supports uploading of a <i>series</i> of batches.
	 * Synapse employs optimistic concurrency on the series in the form of a batch token.   
	 * Each request (except the first) must include the 'batch token' returned in the 
	 * response to the previous batch. If another client begins batch upload simultaneously, 
	 * a PRECONDITION_FAILED (412) response will be generated and upload must restart from the
	 * first batch.  After the final batch is uploaded, the data for the Evaluation queue will
	 * be mirrored to the tables which support querying.  Therefore uploaded data will not appear
	 * in Evaluation queries until after the final batch is successfully uploaded.  It is the
	 * client's responsibility to note in each batch request (1) whether it is the first batch
	 * in the series and (2) whether it is the last batch.  (For a single batch both are set to 'true'.)
	 * Failure to use the flags correctly risks corrupted data (due to simultaneous, conflicting
	 * uploads by multiple clients) or data not appearing in query results.
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId the ID of the Evaluation to which the SubmissionSatus objects belong.
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_STATUS_BATCH, method = RequestMethod.PUT)
	public @ResponseBody
	BatchUploadResponse updateSubmissionStatusBatch(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionStatusBatch batch) 
			throws DatastoreException, UnauthorizedException, InvalidModelException, 
			ConflictingUpdateException, NotFoundException
	{
		return serviceProvider.getEvaluationService().updateSubmissionStatusBatch(userId, evalId, batch);
	}
	
	/**	
	 * Deletes a Submission and its accompanying SubmissionStatus.
	 * <b>This service is intended to only be used by ChallengesInfrastructure service account.</b>
	 * 
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param subId - the ID of the Submission to be deleted.
	 * @param userId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_ID, method = RequestMethod.DELETE)
	public void deleteSubmission(
			@PathVariable String subId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws DatastoreException, UnauthorizedException, NotFoundException
	{
		serviceProvider.getEvaluationService().deleteSubmission(userId, subId);
	}
	
	/**
	 * Gets a collection of Submissions to a specified Evaluation.
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
	 *            An index of 0 is the first entity. When null it will default
	 *            to 0.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10, max value 100.
	 * @param userId
	 * @param statusString
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_ADMIN, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Submission> getAllSubmissions(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}		
		return serviceProvider.getEvaluationService().getAllSubmissions(userId, evalId, status, limit, offset);
	}
	
	/**
	 * Gets a collection of SubmissionStatuses to a specified Evaluation.
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
	 *            An index of 0 is the first entity. When null it will default
	 *            to 0.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10, max value 100.
	 * @param statusString
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS_WITH_EVAL_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionStatus> getAllSubmissionStatuses(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}
		return serviceProvider.getEvaluationService().getAllSubmissionStatuses(userId, evalId, status, limit, offset);
	}
	
	/**
	 * Gets a collection of bundled Submissions and SubmissionStatuses to a given Evaluation. 
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
	 *            An index of 0 is the first entity. When null it will default
	 *            to 0.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10, max value 100.
	 * @param userId
	 * @param statusString
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_ADMIN_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionBundle> getAllSubmissionBundles(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.STATUS, defaultValue = "") String statusString
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		SubmissionStatusEnum status = null;
		if (statusString.length() > 0) {
			status = SubmissionStatusEnum.valueOf(statusString.toUpperCase().trim());
		}		
		return serviceProvider.getEvaluationService().getAllSubmissionBundles(userId, evalId, status, limit, offset);
	}
	
	/**
	 * Gets the requesting user's Submissions to a specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 0 is the first entity. When null it will default
	 *            to 0.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Submission> getMySubmissions(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getMyOwnSubmissionsByEvaluation(evalId, userId, limit, offset);
	}
	
	/**
	 * Gets the requesting user's bundled Submissions and SubmissionStatuses to a specified
	 * Evaluation.
	 * 
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 0 is the first entity. When null it will default
	 *            to 0.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_EVAL_ID_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionBundle> getMySubmissionBundles(
			@PathVariable String evalId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getEvaluationService().getMyOwnSubmissionBundlesByEvaluation(evalId, userId, limit, offset);
	}
	
	/**
	 * Gets a pre-signed URL to access a requested File contained within a specified Submission.
	 * 
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param submissionId - the ID of the specified Submission.
	 * @param fileHandleId - the ID of the requested FileHandle contained in the Submission.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@RequiredScope({download})
	@RequestMapping(value = UrlHelpers.SUBMISSION_FILE, method = RequestMethod.GET)
	public void redirectURLForFileHandle(
			@PathVariable String subId,
			@PathVariable String fileHandleId,
			@RequestParam (required = false) Boolean redirect,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletResponse response
			) throws DatastoreException, NotFoundException, IOException {
		String url = serviceProvider.getEvaluationService().getRedirectURLForFileHandle(userId, subId, fileHandleId);
		RedirectUtils.handleRedirect(redirect, url, response);
	}
	
	/**
	 * Gets the number of Submissions to a specified Evaluation.
	 * 
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ_PRIVATE_SUBMISSION</a> on the specified Evaluation.
	 * </p>
	 * 
	 * @param evalId - the ID of the specified Evaluation.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getSubmissionCount(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getEvaluationService().getSubmissionCount(userId, evalId);
	}
	
	/**
	 * Determines whether a specified Synapse user has a certain 
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE</a> 
	 * on the specified Evaluation.
	 * </p>
	 * 
	 * @param id 
	 * @param userId 
	 * @param accessType  
	 * @return the access types that the given user has to the given resource
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException 
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value={UrlHelpers.EVALUATION_WITH_ID+UrlHelpers.ACCESS}, method=RequestMethod.GET)
	public @ResponseBody BooleanResult hasAccess(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = true) String accessType
			) throws DatastoreException, NotFoundException, UnauthorizedException {
		// pass it along.
		return new BooleanResult(serviceProvider.getEvaluationService().hasAccess(evalId, userId, accessType));
	}

	/**
	 * This method is deprecated and should be removed from future versions of the API.
	 * Creates a new access control list (ACL) for an evaluation.
	 *
	 * @return        The ACL created.
	 */
	@Deprecated
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION_ACL, method = RequestMethod.POST)
	public @ResponseBody AccessControlList
	createAcl()
			throws DeprecatedServiceException {
		throw new DeprecatedServiceException("You cannot create an ACL for an evaluation. " +
				"ACLs for evaluations are created when the evaluation is created. " +
				"To update an existing ACL, see PUT /evaluation/acl");
	}

	/**
	 * Updates the supplied access control list (ACL) for an evaluation.
	 * The <a href="${org.sagebionetworks.repo.model.AccessControlList}">ACL</a>
	 * to be updated should have the ID of the evaluation. The user should have the proper
	 * <a href="${org.sagebionetworks.evaluation.model.UserEvaluationPermissions}">permissions</a>
	 * in order to update the ACL.
	 *
	 * @param userId  The user updating the ACL.
	 * @param acl     The ACL being updated.
	 * @return        The updated ACL.
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ACL, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList
	updateAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		return serviceProvider.getEvaluationService().updateAcl(userId, acl);
	}

	/**
	 * This method is deprecated and should be removed from future versions of the API.
	 * Deletes the ACL (access control list) of the specified evaluation. The user should have the proper
	 * <a href="${org.sagebionetworks.evaluation.model.UserEvaluationPermissions}">permissions</a>
	 * to delete the ACL.
	 *
	 */
	@Deprecated
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_ACL, method = RequestMethod.DELETE)
	public void deleteAcl()
			throws DeprecatedServiceException {
		throw new DeprecatedServiceException("You cannot delete an ACL for an evaluation. " +
				"To update an existing ACL, see PUT /evaluation/acl");
	}

	/**
	 * Gets the access control list (ACL) governing the given evaluation. The user should have the proper
	 * <a href="${org.sagebionetworks.evaluation.model.UserEvaluationPermissions}">permissions</a> to
	 * read the ACL.
	 *
	 * @param userId  The retrieving the ACL.
	 * @param evalId  The ID of the evaluation whose ACL is being retrieved.
	 * @return        The ACL requested.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_ACL, method = RequestMethod.GET)
	public @ResponseBody AccessControlList
	getAcl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId)
			throws NotFoundException, DatastoreException, ACLInheritanceException {
		return serviceProvider.getEvaluationService().getAcl(userId, evalId);
	}

	/**
	 * Gets the <a href="${org.sagebionetworks.evaluation.model.UserEvaluationPermissions}">user permissions</a>
	 * for the specified evaluation.
	 *
	 * @param userId  The ID of the user whose permissions over the specified evaluation are being retrieved.
	 * @param evalId  The ID of the evaluation over which the user permission are being retrieved.
	 * @return  The requested user permissions.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ID_PERMISSIONS, method = RequestMethod.GET)
	public @ResponseBody UserEvaluationPermissions
	getUserPermissionsForEvaluation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId)
			throws NotFoundException, DatastoreException {
		return serviceProvider.getEvaluationService().getUserPermissionsForEvaluation(userId, evalId);
	}
	
	/**
	 * 
	 * Executes a user-defined query over the Submissions of a specific Evaluation. Queries have the following form:
	 * 
	 * <p/>
	 * SELECT &lt;fields&gt; FROM evaluation_&lt;id&gt; [WHERE &lt;filter&gt; (AND &lt;filter&gt;)*] [ORDER BY &lt;name&gt; asc|desc] [LIMIT &lt;L&gt; OFFSET &lt;O&gt;]
	 * <p/>
	 * where
	 * <p/>
	 * &lt;fields&gt; is either "*" or a comma delimited list of names
	 * <br/>
	 * <ul>
	 * <li>&lt;name&gt; is the name either of a system-defined field in a Submission or of a user-defined annotation.  The system-defined field names are:
	 * objectId, scopeId, userId, submitterAlias, entityId, versionNumber, name, createdOn, modifiedOn, and status.
	 * Note:  If a user-defined annotation name and type of value matches/collides with those of a system-defined field, 
	 * the query will be against the field name, not the user defined annotation.</li>
	 * <li>&lt;id&gt; is the Evaluation's ID</li>
	 * <li>&lt;filter&gt; = &lt;name&gt; &lt;comparator&gt; &lt;value&gt;</li>
	 * <li>&lt;comparator&gt; is one of ==, !=, >, <, >=, or <=</li>
	 * <li>&lt;value&gt; is an annotation value, of type string, integer, or decimal</li>
	 * <li>&lt;L&gt; and &lt;O&gt; are optional limit and offset pagination parameters, limit>=1 and offset>=0.
	 * Note:  If pagination is used, LIMIT must precede OFFSET and the pair of parameters must follow ORDER BY (if used).</li>
	 * </ul>
	 * <br/>
	 * <p/>
	 * Examples:<br/>
	 * SELECT * FROM evaluation_123 WHERE myAnnotation == "foo"<br/>
	 * SELECT entityId, status, myAnnotation FROM evaluation_123 WHERE myAnnotation == "foo" AND status="RECEIVED"<br/>
	 * SELECT * FROM evaluation_123 order by status asc limit 20 offset 10<br/>
	 * <p/>
	 * Note:  The query is a <i>parameter</i> of the http request whose key is 'query' and the query parameter is URL encoded, so the URI is of the form:
	 * <br/>/evaluation/submission/query?query=select+*+from+evalution_123+WHERE+...<br/>
	 * <p/>
	 * <p/>
	 * Notes:  
	 * <p/>
	 * User must be granted READ access to the Evaluation in order to issue any query.  READ_PRIVATE access provides access to those 
	 * annotations having their "isPrivate" flag set to true.
	 * <p/>
	 * IF "SELECT *" is used and if the user lacks READ_PRIVATE access to the Evaluation, then any private annotations will
	 * be omitted from the resulting column headers.  However, if the selected annotations are specified explicitly then private
	 * annotation names <i>will</i> be included in the column headers, but their values will be returned as null.  
	 * Further, if the private annotation is included in a filter then no results are returned.
	 * <p/>
	 * Filtering on 'myAnnotaton==null' is allowed, but will only return entries having 'myAnnotation' explicitly set to null,
	 * and not entries which simply have no annotation called 'myAnnotation'.
	 * <p/>
	 * While privacy levels for user defined annotations are set by the user, the privacy level of system-defined fields are always public.
	 * <br/>
	 * The following fields are system-defined: 
	 * <br/>
	 * userId, teamId, submitterId, name, createdOn, submitterAlias, repositoryName, dockerDigest, objectId, scopeId, entityId, versionNumber, modifiedOn, status, canCancel, cancelRequested, 
	 * and <a href="${org.sagebionetworks.evaluation.model.CancelControl}">CancelControl</a> are public.
	 * 
	 * The submitterId field will be equal to either the userId, if the submission is for an individual, or the teamId, if the submission is on behalf of a team. 
	 * 
	 * <p/>
	 * The query is to be URL encoded in the submitted request.
	 * </p>
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum frequency this method can be called</td>
	 * <td>1 calls per second</td>
	 * </tr>
	 * </table>
	 * </p> 
	 * @throws JSONObjectAdapterException
	 * @throws ParseException 
	 * @throws  
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_QUERY, method = RequestMethod.GET)
	@Deprecated
	public @ResponseBody 
	QueryTableResults query(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.QUERY_PARAM, required = true) String query)
			throws NotFoundException, DatastoreException, ParseException, 
			JSONObjectAdapterException {
		return serviceProvider.getEvaluationService().query(query, userId);
	}

	/**
	 * User requests to cancel their submission. Only the user who submitted a submission
	 * can make this request.
	 * 
	 * @param userId
	 * @param subId
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.EVALUATION_SUBMISSION_CANCALLATION, method = RequestMethod.PUT)
	public @ResponseBody void requestToCancelSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String subId) {
		serviceProvider.getEvaluationService().processCancelSubmissionRequest(userId, subId);
	}


	/**
	 * Creates a new EvaluationRound to associate with a Evaluation.
	 * You must have UPDATE permissions for the associated Evaluation in order to create an EvaluationRound.
	 *
	 * This is a replacement for the deprecated <a href="${org.sagebionetworks.evaluation.model.SubmissionQuota}">SubmissionQuota</a>
	 * which is a property inside of <a href="${org.sagebionetworks.evaluation.model.Evaluation}">Evaluation</a>.
	 *
	 * EvaluationRounds define a fixed time period during which submissions to an Evaluation queue are accepted.
	 * Limits to the number of allowed submissions may be defined inside a EvaluationRound.
	 *
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.EVALUATION_ROUND, method = RequestMethod.POST)
	public @ResponseBody
	EvaluationRound createEvaluationRound(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId,
			@RequestBody EvaluationRound evaluationRound)
	{
		if(evaluationRound.getEvaluationId() == null){
			evaluationRound.setEvaluationId(evalId);
		}
		if(!evalId.equals(evaluationRound.getEvaluationId())){
			throw new IllegalArgumentException("EvaluationId in URL path:"+ evalId +" does not match evaluationId in request body:"+ evaluationRound.getEvaluationId());
		}
		return serviceProvider.getEvaluationService().createEvaluationRound(userId, evaluationRound);
	}

	/**
	 * Retrieve an existing EvaluationRound associated with a Evaluation.
	 * You must have READ permissions for the associated Evaluation in order to retrieve an EvaluationRound.
	 *
	 * This is a replacement for the deprecated <a href="${org.sagebionetworks.evaluation.model.SubmissionQuota}">SubmissionQuota</a>
	 * which is a property inside of <a href="${org.sagebionetworks.evaluation.model.Evaluation}">Evaluation</a>.
	 *
	 * EvaluationRounds define a fixed time period during which submissions to an Evaluation queue are accepted.
	 * Limits to the number of allowed submissions may be defined inside a EvaluationRound.
	 *
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ROUND_WITH_ROUND_ID, method = RequestMethod.GET)
	public @ResponseBody
	EvaluationRound getEvaluationRound(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId,
			@PathVariable String roundId)
	{
		return serviceProvider.getEvaluationService().getEvaluationRound(userId, evalId, roundId);
	}

	/**
	 * Retrieve all EvaluationRounds associated with a Evaluation.
	 * You must have READ permissions for the associated Evaluation in order to retrieve all EvaluationRounds.
	 *
	 * This is a replacement for the deprecated <a href="${org.sagebionetworks.evaluation.model.SubmissionQuota}">SubmissionQuota</a>
	 * which is a property inside of <a href="${org.sagebionetworks.evaluation.model.Evaluation}">Evaluation</a>.
	 *
	 * EvaluationRounds define a fixed time period during which submissions to an Evaluation queue are accepted.
	 * Limits to the number of allowed submissions may be defined inside a EvaluationRound.
	 *
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ROUND + UrlHelpers.LIST, method = RequestMethod.POST)
	public @ResponseBody
	EvaluationRoundListResponse getAllEvaluationRounds(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId,
			@RequestBody EvaluationRoundListRequest request)
	{
		return serviceProvider.getEvaluationService().getAllEvaluationRounds(userId, evalId, request);
	}

	/**
	 * Update an existing EvaluationRound to associate with a Evaluation.
	 * You must have UPDATE permissions for the associated Evaluation in order to update an EvaluationRound.
	 *
	 * This is a replacement for the deprecated <a href="${org.sagebionetworks.evaluation.model.SubmissionQuota}">SubmissionQuota</a>
	 * which is a property inside of <a href="${org.sagebionetworks.evaluation.model.Evaluation}">Evaluation</a>.
	 *
	 * EvaluationRounds define a fixed time period during which submissions to an Evaluation queue are accepted.
	 * Limits to the number of allowed submissions may be defined inside a EvaluationRound.
	 *
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_ROUND_WITH_ROUND_ID, method = RequestMethod.PUT)
	public @ResponseBody
	EvaluationRound updateEvaluationRound(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId,
			@PathVariable String roundId,
			@RequestBody EvaluationRound evaluationRound)
	{
		if(!evalId.equals(evaluationRound.getEvaluationId())){
			throw new IllegalArgumentException("evalId in URL path:"+ evalId +" does not match evaluationId in request body:"+ evaluationRound.getEvaluationId());
		}
		if(!roundId.equals(evaluationRound.getId())){
			throw new IllegalArgumentException("roundId in URL path:"+ roundId +" does not match id in request body:"+ evaluationRound.getId());
		}
		return serviceProvider.getEvaluationService().updateEvaluationRound(userId, evaluationRound);
	}

	/**
	 * Delete an existing EvaluationRound to associate with a Evaluation.
	 * You must have UPDATE permissions for the associated Evaluation in order to delete an EvaluationRound.
	 *
	 * This is a replacement for the deprecated <a href="${org.sagebionetworks.evaluation.model.SubmissionQuota}">SubmissionQuota</a>
	 * which is a property inside of <a href="${org.sagebionetworks.evaluation.model.Evaluation}">Evaluation</a>.
	 *
	 * EvaluationRounds define a fixed time period during which submissions to an Evaluation queue are accepted.
	 * Limits to the number of allowed submissions may be defined inside a EvaluationRound.
	 *
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.EVALUATION_ROUND_WITH_ROUND_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteEvaluationRound(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String evalId,
			@PathVariable String roundId)
	{
		serviceProvider.getEvaluationService().deleteEvaluationRound(userId, evalId, roundId);
	}

	/**
	 * Migrates the DEPRECATED <a href="${org.sagebionetworks.evaluation.model.SubmissionQuota}">SubmissionQuota</a>
	 * in the "quota" field of an <a href="${org.sagebionetworks.evaluation.model.Evaluation}">Evaluation</a>
	 * into one or many <a href="${org.sagebionetworks.evaluation.model.EvaluationRound}">EvaluationRound</a>
	 * (depending on the "numberOfRounds" defined in the
	 * <a href="${org.sagebionetworks.evaluation.model.SubmissionQuota}">SubmissionQuota</a>)
	 *
	 * <p>
	 * <b>Note:</b> The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the specified Evaluation.
	 * </p>
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EVALUATION_SUBMISSIONQUOTA_MIGRATION, method = RequestMethod.POST)
	public @ResponseBody
	void migrateEvaluationSubmission(
			@PathVariable String evalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
	{
		serviceProvider.getEvaluationService().migrateEvaluationSubmissionQuota(userId, evalId);
	}

	// For some unknown reason binding a List<Long> with a @RequestParam is not working with our setup 
	// (Leaving this static method here as this is a feature present since spring 3, more investigation is needed)
	private static List<Long> stringToEvaluationIds(String value) {
		if (value == null || value.isEmpty()) {
			return Collections.emptyList();
		}
		String[] evalIdStrings = value.split(ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR);
		List<Long> evalIds = new ArrayList<>();
		for (String s : evalIdStrings) {
			Long l;
			try {
				l = Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new InvalidModelException("Expected an evaluation ID but found "+s);
			}
			evalIds.add(l);
		}
		return evalIds;
	}

}
