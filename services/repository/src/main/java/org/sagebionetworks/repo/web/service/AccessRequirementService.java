package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementService {

	public AccessRequirement createAccessRequirement(String userId,
			AccessRequirement accessRequirement) throws Exception;

	public PaginatedResults<AccessRequirement> getUnfulfilledEntityAccessRequirements(
			String userId, String entityId,	HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public PaginatedResults<AccessRequirement> getEntityAccessRequirements(
			String userId, String entityId,	HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public PaginatedResults<AccessRequirement> getUnfulfilledEvaluationAccessRequirements(
			String userId, String evaluationId,	HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public PaginatedResults<AccessRequirement> getEvaluationAccessRequirements(
			String userId, String evaluationId,	HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public void deleteAccessRequirements(String userId, String requirementId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

}