/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class MembershipInvitationManagerImpl implements
		MembershipInvitationManager {

	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired 
	private MembershipInvtnSubmissionDAO membershipInvtnSubmissionDAO;
	
	public MembershipInvitationManagerImpl() {}
	
	// for testing
	public MembershipInvitationManagerImpl(
			AuthorizationManager authorizationManager,
			MembershipInvtnSubmissionDAO membershipInvtnSubmissionDAO
			) {
		this.authorizationManager = authorizationManager;
		this.membershipInvtnSubmissionDAO = membershipInvtnSubmissionDAO;
	}
	
	public static void validateForCreate(MembershipInvtnSubmission mis) {
		if (mis.getCreatedBy()!=null) throw new InvalidModelException("'createdBy' field is not user specifiable.");
		if (mis.getCreatedOn()!=null) throw new InvalidModelException("'createdOn' field is not user specifiable.");
		if (mis.getId()!=null) throw new InvalidModelException("'id' field is not user specifiable.");
		if (mis.getInviteeId()==null) throw new InvalidModelException("'inviteeId' field is required.");
		if (mis.getTeamId()==null) throw new InvalidModelException("'teamId' field is required.");
	}

	public static void populateCreationFields(UserInfo userInfo, MembershipInvtnSubmission mis, Date now) {
		mis.setCreatedBy(userInfo.getId().toString());
		mis.setCreatedOn(now);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#create(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.MembershipInvtnSubmission)
	 */
	@Override
	public MembershipInvtnSubmission create(UserInfo userInfo,
			MembershipInvtnSubmission mis) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {
		validateForCreate(mis);
		if (!authorizationManager.canAccess(
				userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot create membership invitation.");
		Date now = new Date();
		populateCreationFields(userInfo, mis, now);
		return membershipInvtnSubmissionDAO.create(mis);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#get(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public MembershipInvtnSubmission get(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException {
		MembershipInvtnSubmission mis = membershipInvtnSubmissionDAO.get(id);
		if (!authorizationManager.canAccess(
				userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot retrieve membership invitation.");
		return mis;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		MembershipInvtnSubmission mis = null;
		try {
			mis = membershipInvtnSubmissionDAO.get(id);
		} catch (NotFoundException e) {
			return;
		}
		if (!authorizationManager.canAccess(
				userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot delete membership invitation.");
		membershipInvtnSubmissionDAO.delete(id);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#getOpenForUserInRange(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipInvitation> getOpenForUserInRange(
			String principalId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Date now = new Date();
		long principalIdAsLong = Long.parseLong(principalId);
		List<MembershipInvitation> miList = membershipInvtnSubmissionDAO.getOpenByUserInRange(principalIdAsLong, now.getTime(), limit, offset);
		long count = membershipInvtnSubmissionDAO.getOpenByUserCount(principalIdAsLong, now.getTime());
		PaginatedResults<MembershipInvitation> results = new PaginatedResults<MembershipInvitation>();
		results.setResults(miList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipInvitationManager#getOpenForUserAndTeamInRange(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipInvitation> getOpenForUserAndTeamInRange(
			String principalId, String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Date now = new Date();
		long principalIdAsLong = Long.parseLong(principalId);
		long teamIdAsLong = Long.parseLong(teamId);
		List<MembershipInvitation> miList = membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamIdAsLong, principalIdAsLong, now.getTime(), limit, offset);
		long count = membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamIdAsLong, principalIdAsLong, now.getTime());
		PaginatedResults<MembershipInvitation> results = new PaginatedResults<MembershipInvitation>();
		results.setResults(miList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public PaginatedResults<MembershipInvtnSubmission> getOpenSubmissionsForTeamInRange(
			UserInfo userInfo, String teamId, long limit, long offset) throws NotFoundException {
		if (!authorizationManager.canAccess(
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot retrieve membership invitations for team "+teamId+".");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		List<MembershipInvtnSubmission> miList = membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamInRange(teamIdAsLong, now.getTime(), limit, offset);
		long count = membershipInvtnSubmissionDAO.getOpenByTeamCount(teamIdAsLong, now.getTime());
		PaginatedResults<MembershipInvtnSubmission> results = new PaginatedResults<MembershipInvtnSubmission>();
		results.setResults(miList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public PaginatedResults<MembershipInvtnSubmission> getOpenSubmissionsForUserAndTeamInRange(
			UserInfo userInfo, String inviteeId, String teamId, long limit,
			long offset) throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(
				userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized()) 
			throw new UnauthorizedException("Cannot retrieve membership invitations for team "+teamId+".");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		long inviteeIdAsLong = Long.parseLong(inviteeId);
		List<MembershipInvtnSubmission> miList = membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(teamIdAsLong, inviteeIdAsLong, now.getTime(), limit, offset);
		long count = membershipInvtnSubmissionDAO.getOpenByTeamCount(teamIdAsLong, now.getTime());
		PaginatedResults<MembershipInvtnSubmission> results = new PaginatedResults<MembershipInvtnSubmission>();
		results.setResults(miList);
		results.setTotalNumberOfResults(count);
		return results;
	}

}
