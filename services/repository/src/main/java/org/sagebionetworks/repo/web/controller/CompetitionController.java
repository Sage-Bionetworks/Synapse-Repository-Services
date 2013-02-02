package org.sagebionetworks.repo.web.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.competition.model.SubmissionBundle;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.util.ControllerUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class CompetitionController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.COMPETITION, method = RequestMethod.POST)
	public @ResponseBody
	Competition createCompetition(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Competition comp = new Competition(new JSONObjectAdapterImpl(requestBody));
		return serviceProvider.getCompetitionService().createCompetition(userId, comp);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Competition getCompetition(
			@PathVariable String compId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getCompetitionService().getCompetition(compId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Competition> getCompetitionsPaginated(
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			HttpServletRequest request
			) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getCompetitionService().getCompetitionsInRange(limit, offset, request);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getCompetitionCount(HttpServletRequest request) throws DatastoreException, NotFoundException
	{
		return serviceProvider.getCompetitionService().getCompetitionCount();
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION_WITH_NAME, method = RequestMethod.GET)
	public @ResponseBody
	Competition findCompetition(
			@PathVariable String name,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, UnsupportedEncodingException 
	{
		String decodedName = URLDecoder.decode(name, "UTF-8");
		return serviceProvider.getCompetitionService().findCompetition(decodedName);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COMPETITION_WITH_ID, method = RequestMethod.PUT)
	public @ResponseBody
	Competition updateCompetition(
			@PathVariable String compId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, InvalidModelException, ConflictingUpdateException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Competition comp = new Competition(new JSONObjectAdapterImpl(requestBody));
		if (!compId.equals(comp.getId()))
			throw new IllegalArgumentException("Competition ID does not match requested ID: " + compId);
		return serviceProvider.getCompetitionService().updateCompetition(userId, comp);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.COMPETITION_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteCompetition(
			@PathVariable String compId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		serviceProvider.getCompetitionService().deleteCompetition(userId, compId);
	}
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.PARTICIPANT, method = RequestMethod.POST)
	public @ResponseBody
	Participant createParticipant(
			@PathVariable String compId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException
	{
		return serviceProvider.getCompetitionService().addParticipant(userId, compId);
	}
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_WITH_ID, method = RequestMethod.POST)
	public @ResponseBody
	Participant createParticipantAsAdmin(
			@PathVariable String compId,
			@PathVariable String partId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException
	{
		return serviceProvider.getCompetitionService().addParticipantAsAdmin(userId, compId, partId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Participant getParticipant(
			@PathVariable String compId,
			@PathVariable String partId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getCompetitionService().getParticipant(partId, compId);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_WITH_ID, method = RequestMethod.DELETE)
	public @ResponseBody
	void deleteParticipantAsAdmin(
			@PathVariable String compId,
			@PathVariable String partId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException
	{
		serviceProvider.getCompetitionService().removeParticipant(userId, compId, partId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Participant> getAllParticipants(
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@PathVariable String compId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getCompetitionService().getAllParticipants(compId, limit, offset, request);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.PARTICIPANT_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getParticipantCount(@PathVariable String compId, HttpServletRequest request) 
			throws DatastoreException, NotFoundException
	{
		return serviceProvider.getCompetitionService().getParticipantCount(compId);
	}
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody
	Submission createSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException
	{
		String requestBody = ControllerUtil.getRequestBodyAsString(request);
		Submission sub = new Submission(new JSONObjectAdapterImpl(requestBody));
		return serviceProvider.getCompetitionService().createSubmission(userId, sub);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	Submission getSubmission(
			@PathVariable String subId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getCompetitionService().getSubmission(subId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_STATUS, method = RequestMethod.GET)
	public @ResponseBody
	SubmissionStatus getSubmissionStatus(
			@PathVariable String subId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getCompetitionService().getSubmissionStatus(subId);
	}
	
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
		return serviceProvider.getCompetitionService().updateSubmissionStatus(userId, status);
	}
	
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
		serviceProvider.getCompetitionService().deleteSubmission(userId, subId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_COMP_ID_ADMIN, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Submission> getAllSubmissions(
			@PathVariable String compId,
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
		return serviceProvider.getCompetitionService().getAllSubmissions(userId, compId, status, offset, limit, request);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_COMP_ID_ADMIN_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionBundle> getAllSubmissionBundles(
			@PathVariable String compId,
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
		return serviceProvider.getCompetitionService().getAllSubmissionBundles(userId, compId, status, offset, limit, request);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_COMP_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Submission> getMySubmissions(
			@PathVariable String compId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getCompetitionService().getAllSubmissionsByCompetitionAndUser(compId, userId, limit, offset, request);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_WITH_COMP_ID_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<SubmissionBundle> getMySubmissionBundles(
			@PathVariable String compId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) long limit,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException 
	{
		return serviceProvider.getCompetitionService().getAllSubmissionBundlesByCompetitionAndUser(compId, userId, limit, offset, request);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SUBMISSION_COUNT, method = RequestMethod.GET)
	public @ResponseBody
	long getSubmissionCount(@PathVariable String compId, HttpServletRequest request) 
			throws DatastoreException, NotFoundException
	{
		return serviceProvider.getCompetitionService().getSubmissionCount(compId);
	}
}
