package org.sagebionetworks.evaluation.manager;

import static org.sagebionetworks.evaluation.model.SubmissionStatusEnum.ACCEPTED;
import static org.sagebionetworks.evaluation.model.SubmissionStatusEnum.CLOSED;
import static org.sagebionetworks.evaluation.model.SubmissionStatusEnum.EVALUATION_IN_PROGRESS;
import static org.sagebionetworks.evaluation.model.SubmissionStatusEnum.OPEN;
import static org.sagebionetworks.evaluation.model.SubmissionStatusEnum.RECEIVED;
import static org.sagebionetworks.evaluation.model.SubmissionStatusEnum.SCORED;
import static org.sagebionetworks.evaluation.model.SubmissionStatusEnum.VALIDATED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.MemberSubmissionEligibility;
import org.sagebionetworks.evaluation.model.SubmissionEligibility;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;

public class SubmissionEligibilityManagerImpl implements
		SubmissionEligibilityManager {

	@Autowired
	SubmissionDAO submissionDAO;
	
	@Autowired
	private EvaluationDAO evaluationDAO;
	
	@Autowired
	private ChallengeDAO challengeDAO;
	
	@Autowired
	private ChallengeTeamDAO challengeTeamDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	public static int computeTeamSubmissionEligibilityHash(TeamSubmissionEligibility tse) {
		return tse.getTeamEligibility().hashCode() * 17 + tse.getMembersEligibility().hashCode();
	}

	// unless a submission is marked INVALID or REJECTED we count it toward
	// the submitter's quota
	// statuses not in the enum: INVALID, REJECTED
	public static final Set<SubmissionStatusEnum> STATUSES_COUNTED_TOWARD_QUOTA = 
			new HashSet<SubmissionStatusEnum>(Arrays.asList(new SubmissionStatusEnum[]{
					OPEN,
					CLOSED,
					SCORED,
					VALIDATED,
					EVALUATION_IN_PROGRESS,
					RECEIVED,
					ACCEPTED}));

	
	/**
	 * Authorization:  user making the request must have SUBMIT permission in the Evaluation 
	 * and must be a member of the specified Team.
	 * 
	 * For the team as a whole we say:
	 * - isRegistered (for this challenge)
	 * - isQuotaFilled (for this round)
	 * - isEligible (to submit in this round)
	 * 		this is the sum total of the previous results
	 *
	 * For each team member we say:
	 * - isRegistered (for this challenge)
	 * 	answered by listTeamMembers(team, ids)
	 * - isQuotaFilled (for this round)
	 * 	answered by the new 'countSubmissionsByTeamMember' service
	 * - hasSubmittedElsewhere (for this round):  contributed to a submission in this round having no Team OR a different Team
	 * - isEligible (to submit for this team):
	 * 	this is the sum total of the previous results
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws NumberFormatException 
	 */
	@Override
	public TeamSubmissionEligibility getTeamSubmissionEligibility(Evaluation evaluation, String teamId) throws DatastoreException, NumberFormatException, NotFoundException {
		TeamSubmissionEligibility tse = new TeamSubmissionEligibility();
		tse.setEvaluationId(evaluation.getId());
		tse.setTeamId(teamId);
		SubmissionEligibility teamEligibility = new SubmissionEligibility();
		tse.setTeamEligibility(teamEligibility);
		boolean isTeamEligible = true;
		
		// first check that the team is registered
		Challenge challenge;
		try {
			challenge = challengeDAO.getForProject(evaluation.getContentSource());
			teamEligibility.setIsRegistered(challengeTeamDAO.
				isTeamRegistered(Long.parseLong(challenge.getId()), Long.parseLong(teamId)));
		} catch (NotFoundException e) {
			// if there's no Challenge, the skip the registration requirement
			challenge=null;
			teamEligibility.setIsRegistered(null);
		}
		isTeamEligible &= (teamEligibility.getIsRegistered()==null || teamEligibility.getIsRegistered());
		
		// now check whether the Team's quota is filled
		Date now = new Date();
		Pair<Date,Date> roundInterval = SubmissionQuotaUtil.getRoundInterval(evaluation, now);
		int submissionCount = (int)submissionDAO.countSubmissionsByTeam(Long.parseLong(evaluation.getId()), 
				Long.parseLong(teamId), roundInterval.getFirst(), 
				roundInterval.getSecond(), STATUSES_COUNTED_TOWARD_QUOTA);
		Integer submissionLimit = SubmissionQuotaUtil.getSubmissionQuota(evaluation);
		teamEligibility.setIsQuotaFilled(submissionLimit!=null && submissionCount>=submissionLimit);
		isTeamEligible &= !teamEligibility.getIsQuotaFilled();
		
		// now put it all together to say whether the Team is eligible to submit to the Evaluation
		teamEligibility.setIsEligible(isTeamEligible);
		
		// ------------  Next we go through all the team members ------------
		List<MemberSubmissionEligibility> memberSubmissionEligibility = 
				new ArrayList<MemberSubmissionEligibility>();
		tse.setMembersEligibility(memberSubmissionEligibility);
		
		// first find all the team members
		List<UserGroup> teamMembers = groupMembersDAO.getMembers(teamId);
		// find the challenge registrants
		Set<Long> challengeRegistrants=null;
		if (challenge!=null) {
			challengeRegistrants = new HashSet<Long>();
			for (UserGroup participant : groupMembersDAO.getMembers(challenge.getParticipantTeamId())) {
				challengeRegistrants.add(Long.parseLong(participant.getId()));
			}
		}
		Map<Long,MemberSubmissionEligibility> membersEligibilityMap = new HashMap<Long,MemberSubmissionEligibility>();
		for (UserGroup member : teamMembers) {
			Long memberId = Long.parseLong(member.getId());
			MemberSubmissionEligibility se = new MemberSubmissionEligibility();
			se.setPrincipalId(memberId);
			if (challenge==null) {
				// don't check challenge registration if there's no challenge object
				se.setIsRegistered(null);
			} else {
				se.setIsRegistered(challengeRegistrants.contains(memberId));
			}
			se.setIsQuotaFilled(false); // will update for those whose quotas are filled, below
			se.setHasConflictingSubmission(false); // will update for those having conflicts, below
			membersEligibilityMap.put(memberId, se);
		}
		
		// now check, for each, whether they've exceeded their submission limit
		Map<Long,Long> subsByMembers = submissionDAO.countSubmissionsByTeamMembers(Long.parseLong(evaluation.getId()), 
				Long.parseLong(teamId), roundInterval.getFirst(), 
				roundInterval.getSecond(), STATUSES_COUNTED_TOWARD_QUOTA);
		for (Long principalId : subsByMembers.keySet()) {
			MemberSubmissionEligibility se = membersEligibilityMap.get(principalId);
			se.setIsQuotaFilled(submissionLimit!=null && subsByMembers.get(principalId)>=submissionLimit);
		}
 		
		// now see if members are ineligible because they've submitted elsewhere
		List<Long> membersSubmittingElsewhere = submissionDAO.getTeamMembersSubmittingElsewhere(Long.parseLong(evaluation.getId()), 
				Long.parseLong(teamId), roundInterval.getFirst(), 
				roundInterval.getSecond(), STATUSES_COUNTED_TOWARD_QUOTA);
		for (Long principalId : membersSubmittingElsewhere) {
			MemberSubmissionEligibility se = membersEligibilityMap.get(principalId);
			se.setHasConflictingSubmission(true);
		}
		
		// set overall 'isEligible' flag and build up the object list to return
		for (Long principalId : membersEligibilityMap.keySet()) {
			MemberSubmissionEligibility se = membersEligibilityMap.get(principalId);
			se.setIsEligible(!se.getHasConflictingSubmission() &&
					!se.getIsQuotaFilled() &&
					(se.getIsRegistered()==null || se.getIsRegistered()));
			memberSubmissionEligibility.add(se);
		}
		
		// finally, compute the hash of the content and add it into the object
		tse.setEligibilityStateHash((long)computeTeamSubmissionEligibilityHash(tse));
		return tse;
	}	
	
	/*
	 * Determine whether a Team and its members are authorized to submit to 
	 * the given evaluation.
	 */
	@Override
	public AuthorizationStatus isTeamEligible(String evalId, String teamId, 
			List<String> contributors, String submissionEligibilityHashString, Date now) throws DatastoreException, NotFoundException {
		Evaluation evaluation = evaluationDAO.get(evalId);
		if (!SubmissionQuotaUtil.isSubmissionAllowed(evaluation, now)) {
			return AuthorizationStatus.accessDenied("It is currently outside of the time range allowed for submissions.");
		}
		
		if (submissionEligibilityHashString==null) 
			return AuthorizationStatus.accessDenied("Submission Eligibilty Hash is required.");
		int seHash;
		try {
			seHash = Integer.parseInt(submissionEligibilityHashString);
		} catch (NumberFormatException e) {
			return AuthorizationStatus.accessDenied("Submission Eligibilty Hash is invalid.");
		}
		TeamSubmissionEligibility tse = getTeamSubmissionEligibility(evaluation, teamId);
		if (seHash!=computeTeamSubmissionEligibilityHash(tse)) 
			return AuthorizationStatus.accessDenied("Submissions or Team composition have changed.  Please try again.");

		if (!tse.getTeamEligibility().getIsEligible()) {
			return AuthorizationStatus.accessDenied("The specified Team is ineligible to submit to the specified Evaluation at this time.");
		}
		// this will be the list of all team members who CAN contribute
		Set<String> eligibleContributors = new HashSet<String>();
		for (MemberSubmissionEligibility memberEligibility : tse.getMembersEligibility()) {
			if (memberEligibility.getIsEligible()) {
				eligibleContributors.add(memberEligibility.getPrincipalId().toString());
			}
		}
		// now go through the list of would-be contributors and find those who are ineligible
		List<String> ineligibleContributors = new ArrayList<String>();
		for (String contributor : contributors) {
			if (!eligibleContributors.contains(contributor)) ineligibleContributors.add(contributor);
		}
		if (!ineligibleContributors.isEmpty()) {
			return AuthorizationStatus.accessDenied(
					"The following Team members are ineligible to contribute to a Submission in the specified Evaluation at this time "+
					ineligibleContributors);
		}
		return AuthorizationStatus.authorized();
	}
	
	/*
	 * Determine whether an individual is authorized to submit to the given evaluation
	 * 
	 * - how many submissions by a _given user_ in the current round?
	 * - is a given user a contributor to some team submission?
	 */
	@Override
	public AuthorizationStatus isIndividualEligible(String evalId, UserInfo userInfo, Date now) throws DatastoreException, NotFoundException {
		Evaluation evaluation = evaluationDAO.get(evalId);
		SubmissionQuota quota = evaluation.getQuota();
		String principalId = userInfo.getId().toString();
		// check whether registered
		{
			Challenge challenge=null;
			try {
				challenge = challengeDAO.getForProject(evaluation.getContentSource());
				Long participantTeamId = Long.parseLong(challenge.getParticipantTeamId());
				if (!userInfo.getGroups().contains(participantTeamId)) {
					return AuthorizationStatus.accessDenied("Submitter is not registered for the challenge.");
				}
			} catch (NotFoundException e) {
				// skip this check
			}
		}
		if (!SubmissionQuotaUtil.isSubmissionAllowed(evaluation, now)) {
			return AuthorizationStatus.accessDenied("It is currently outside of the time range allowed for submissions.");
		}
		Pair<Date,Date> roundInterval = SubmissionQuotaUtil.getRoundInterval(evaluation, now);
		int submissionCount = (int)submissionDAO.countSubmissionsByContributor(Long.parseLong(evalId), 
				Long.parseLong(principalId), roundInterval.getFirst(), 
				roundInterval.getSecond(), STATUSES_COUNTED_TOWARD_QUOTA);
		Integer submissionLimit = SubmissionQuotaUtil.getSubmissionQuota(evaluation);
		String messageSuffix = ".";
		if (!(roundInterval.getFirst()==null && roundInterval.getSecond()==null)) {
			messageSuffix += " (for the current submission round).";
			
		}
		if (submissionLimit!=null && submissionCount>=submissionLimit) {
			return AuthorizationStatus.accessDenied("Submitter has reached the limit of "+submissionLimit+messageSuffix);
		}
		
		if (submissionDAO.hasContributedToTeamSubmission(Long.parseLong(evalId), 
				Long.parseLong(principalId), roundInterval.getFirst(), 
				roundInterval.getSecond(), STATUSES_COUNTED_TOWARD_QUOTA)) {
			return AuthorizationStatus.accessDenied(
					"Submitter may not submit as an individual when having submitted as part of a Team"+
							messageSuffix	
			);			
		}
		return AuthorizationStatus.authorized();
	}


}
