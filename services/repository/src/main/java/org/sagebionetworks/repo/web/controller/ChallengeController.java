package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;
import static org.sagebionetworks.repo.web.UrlHelpers.ID_PATH_VARIABLE;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengePagedResults;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamPagedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
 * 
 * A Challenge is a special object that supplements a project, providing additional features
 * specific to challenges.  This set of services provides "CRUD" for Challenge objects and
 * ChallengeTeam objects, which register a Team for a Challenge.  The services also provide
 * a number of queries regarding Challenges, challenge participants and challenge Teams.
 *
 */
@ControllerInfo(displayName="Challenge Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class ChallengeController {
	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a Challenge object, associated with a Project.  A participant Team must
	 * be specified.  To create a Challenge one must have CREATE permission on the
	 * associated Project.
	 * 
	 * @param userId
	 * @param challenge
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.CHALLENGE, method = RequestMethod.POST)
	public @ResponseBody
	Challenge createChallenge(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Challenge challenge
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getChallengeService().createChallenge(userId, challenge);
	}

	/**
	 * Retrieve a Challenge given its ID.  To retrieve a
	 * Challenge one must have READ permission on the associated Project.
	 * 
	 * @param userId
	 * @param challengeId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHALLENGE_ID, method = RequestMethod.GET)
	public @ResponseBody Challenge getChallenge(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable long challengeId
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getChallengeService().getChallenge(userId, challengeId);
	}

	/**
	 * Retrieve a Challenge given the ID of its associated Project.  To retrieve a
	 * Challenge one must have READ permission on the Project.
	 * 
	 * @param userId
	 * @param projectId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_CHALLENGE, method = RequestMethod.GET)
	public @ResponseBody Challenge getChallengeByProjectId(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = ID_PATH_VARIABLE) String projectId
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getChallengeService().getChallengeByProjectId(userId, projectId);
	}

	/**
	 * List the Challenges for which the given participant is registered.  
	 * To be in the returned list the caller must have READ permission on the 
	 * project associated with the Challenge.
	 * 
	 * @param userId
	 * @param participantId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE, method = RequestMethod.GET)
	public @ResponseBody ChallengePagedResults listChallengesForParticipant(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required=true) long participantId, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getChallengeService().listChallengesForParticipant(userId, participantId, limit, offset);
	}

	/**
	 * Update a Challenge.  The caller must have UPDATE permission on the 
	 * project associated with the Challenge.  It is not permitted to
	 * change the project associated with a Challenge.
	 * 
	 * @param userId
	 * @param challenge
	 * @param challengeId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHALLENGE_ID, method = RequestMethod.PUT)
	public @ResponseBody Challenge updateChallenge(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Challenge challenge,
			@PathVariable(value = UrlHelpers.CHALLENGE_ID_PATH_VARIABLE) long challengeId
			) throws DatastoreException, NotFoundException {
		if (challenge.getId()!=null && challengeId!=Long.parseLong(challenge.getId()))
			throw new InvalidModelException("Challenge ID in URI in path must match that in request body.");
		return serviceProvider.getChallengeService().updateChallenge(userId, challenge);
	}
	
	/**
	 * Delete a Challenge.  The caller must have DELETE permission on the 
	 * project associated with the Challenge.  
	 * @param userId
	 * @param challengeId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHALLENGE_ID, method = RequestMethod.DELETE)
	public void deleteChallenge(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.CHALLENGE_ID_PATH_VARIABLE) long challengeId
			) throws DatastoreException, NotFoundException {
		serviceProvider.getChallengeService().deleteChallenge(userId, challengeId);
	}

	/**
	 * List the participants registered for a Challenge.  
	 * The caller must have READ permission on the 
	 * project associated with the Challenge.
	 * @param userId
	 * @param challengeId
	 * @param affiliated If affiliated=true, return just participants affiliated with some 
	 * registered Team.  If false, return those not affiliated with any registered Team.  
	 * If omitted return all participants.
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHAL_ID_PARTICIPANT, method = RequestMethod.GET)
	public @ResponseBody PaginatedIds listParticipantsInChallenge(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.CHALLENGE_ID_PATH_VARIABLE) long challengeId,
			@RequestParam(required=false) Boolean affiliated, 
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getChallengeService().listParticipantsInChallenge(userId, challengeId, affiliated, limit, offset);
	}

	/**
	 * Register a Team with a Challenge. You must be a member of the Challenge's
	 * participant Team (i.e. you must be already registered for the Challenge)
	 * and be an administrator on the Team being registered.
	 * @param userId
	 * @param challengeTeam
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHAL_ID_CHAL_TEAM, method = RequestMethod.POST)
	public @ResponseBody ChallengeTeam createChallengeTeam(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.CHALLENGE_ID_PATH_VARIABLE) long challengeId,
			@RequestBody ChallengeTeam challengeTeam
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		if (challengeTeam.getChallengeId()!=null && challengeId!=Long.parseLong(challengeTeam.getChallengeId()))
			throw new InvalidModelException("Challenge ID in URI in path must match that in request body.");
		return serviceProvider.getChallengeService().createChallengeTeam(userId, challengeTeam);
	}

	/**
	 * List the Teams registered for a Challenge.  You must have READ permission
	 * in the associated Project to make this request.
	 * 
	 * @param userId
	 * @param challengeId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHAL_ID_CHAL_TEAM, method = RequestMethod.GET)
	public @ResponseBody ChallengeTeamPagedResults listChallengeTeams(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.CHALLENGE_ID_PATH_VARIABLE) long challengeId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset
			) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getChallengeService().listChallengeTeams(userId, challengeId, limit, offset);
	}

	/**
	 * List the Teams that caller can register for the Challenge, i.e. Teams on which
	 * the caller is an administrator and which are not already registered.
	 * The caller must have READ permission on the 
	 * project associated with the Challenge to make this request.
	 * 
	 * @param userId
	 * @param challengeId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHAL_ID_REGISTRATABLE_TEAM, method = RequestMethod.GET)
	public @ResponseBody PaginatedIds listRegistratableTeams(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.CHALLENGE_ID_PATH_VARIABLE) long challengeId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset
			) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getChallengeService().listRegistratableTeams(userId, challengeId, limit, offset);
	}

	/**
	 * Update a Challenge Team. You must be a member of the Challenge's
	 * participant Team (i.e. you must be already registered for the Challenge)
	 * and be an administrator on the associated Team.
	 * 
	 * @param userId
	 * @param challengeTeam
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHAL_ID_CHAL_TEAM_CHAL_TEAM_ID, method = RequestMethod.PUT)
	public @ResponseBody ChallengeTeam updateChallengeTeam(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.CHALLENGE_ID_PATH_VARIABLE) long challengeId,
			@PathVariable(value = UrlHelpers.CHALLENGE_TEAM_ID_PATH_VARIABLE) long challengeTeamId,
			@RequestBody ChallengeTeam challengeTeam
			) throws DatastoreException, NotFoundException {
		if (challengeTeam.getChallengeId()!=null && challengeId!=Long.parseLong(challengeTeam.getChallengeId()))
			throw new InvalidModelException("Challenge ID in URI in path must match that in request body.");
		if (challengeTeam.getId()!=null && challengeTeamId!=Long.parseLong(challengeTeam.getId()))
			throw new InvalidModelException("ChallengeTeam ID in URI in path must match that in request body.");
		return serviceProvider.getChallengeService().updateChallengeTeam(userId, challengeTeam);
	}

	/**
	 * De-register a Team from a Challenge. You must be a member of the Challenge's
	 * participant Team (i.e. you must be already registered for the Challenge)
	 * and be an administrator on the Team being de-registered.
	 * 
	 * @param userId
	 * @param challengeTeamId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_TEAM_CHAL_TEAM_ID, method = RequestMethod.DELETE)
	public void deleteChallengeTeam(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.CHALLENGE_TEAM_ID_PATH_VARIABLE) long challengeTeamId
			) throws DatastoreException, NotFoundException {
		serviceProvider.getChallengeService().deleteChallengeTeam(userId, challengeTeamId);
	}

	/**
	 * List the Teams under which the given submitter may submit to the Challenge, i.e. the Teams on which
	 * the user is a member and which are registered for the Challenge.
	 * 
	 * @param userId
	 * @param challengeId
	 * @param submitterPrincipalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.CHALLENGE_CHAL_ID_SUBMISSION_TEAMS, method = RequestMethod.GET)
	public @ResponseBody PaginatedIds listSubmissionTeams(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(value = UrlHelpers.CHALLENGE_ID_PATH_VARIABLE) long challengeId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) long submitterPrincipalId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) long offset
			) throws DatastoreException, NotFoundException {
		return serviceProvider.getChallengeService().listSubmissionTeams(userId, challengeId, submitterPrincipalId, limit, offset);
	}
}
