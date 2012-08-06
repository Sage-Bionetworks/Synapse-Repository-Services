package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpHeaders;

public interface AccessRequirementService {

	public AccessRequirement createAccessRequirement(String userId,
			HttpHeaders header, HttpServletRequest request) throws Exception;

	public AccessRequirement deserialize(HttpServletRequest request,
			HttpHeaders header) throws DatastoreException, IOException;

	public PaginatedResults<AccessRequirement> getUnfulfilledAccessRequirement(
			String userId, String entityId, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public PaginatedResults<AccessRequirement> getAccessRequirements(
			String userId, String entityId, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

	public void deleteAccessRequirements(String userId, String requirementId)
			throws DatastoreException, UnauthorizedException,
			NotFoundException, ForbiddenException;

}