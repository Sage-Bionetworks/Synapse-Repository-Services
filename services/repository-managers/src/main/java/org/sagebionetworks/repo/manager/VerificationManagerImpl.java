package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.APPROVED;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.REJECTED;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.SUSPENDED;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dbo.principal.AliasUtils;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;

public class VerificationManagerImpl implements VerificationManager {
	
	@Autowired
	private VerificationDAO verificationDao;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	public VerificationManagerImpl() {}

	// for testing
	public VerificationManagerImpl(
			VerificationDAO verificationDao,
			UserProfileManager userProfileManager,
			FileHandleManager fileHandleManager,
			PrincipalAliasDAO principalAliasDAO,
			AuthorizationManager authorizationManager) {
		this.verificationDao = verificationDao;
		this.userProfileManager = userProfileManager;
		this.fileHandleManager = fileHandleManager;
		this.principalAliasDAO = principalAliasDAO;
		this.authorizationManager = authorizationManager;
	}

	@Override
	public VerificationSubmission createVerificationSubmission(
			UserInfo userInfo, VerificationSubmission verificationSubmission) {
		// check whether there is already an active (submitted or approved) verification submission
		VerificationSubmission current = verificationDao.getCurrentVerificationSubmissionForUser(userInfo.getId());
		if (current!=null) {
			List<VerificationState> states = current.getStateHistory();
			VerificationStateEnum state = states.get(states.size()-1).getState();
			if (state==VerificationStateEnum.SUBMITTED) {
				throw new UnauthorizedException("A verification request has already been submitted.");
			} else if (state==VerificationStateEnum.APPROVED) {
				throw new UnauthorizedException("You are already verified.");
			}
		}
		populateCreateFields(verificationSubmission, userInfo, new Date());
		validateVerificationSubmission(verificationSubmission, 
				userProfileManager.getUserProfile(userInfo.getId().toString()),
				getOrcid(userInfo.getId()));
		// 		User must be owner of file handle Ids
		if (verificationSubmission.getFiles()!=null) {
			for (String fileHandleId : verificationSubmission.getFiles()) {
				AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId));
			}
		}
		return verificationDao.createVerificationSubmission(verificationSubmission);
	}
	
	public static void populateCreateFields(VerificationSubmission verificationSubmission, UserInfo userInfo, Date now) {
		verificationSubmission.setCreatedBy(userInfo.getId().toString());
		verificationSubmission.setCreatedOn(now);
		verificationSubmission.setStateHistory(null);
	}
	
	private String getOrcid(Long userId) {
		List<PrincipalAlias> orcidAliases = principalAliasDAO.listPrincipalAliases(userId, AliasType.USER_ORCID);
		if (orcidAliases.size()>1) throw new IllegalStateException("Cannot have multiple ORCIDs.");
		return orcidAliases.isEmpty() ? null : orcidAliases.get(0).getAlias();
	}
	
	// Validate the content:
	// 		Content must match user profile, emails, ORCID in system at the time the request is made.
	//		Rejected if required fields are blank.
	public static void validateVerificationSubmission(
			VerificationSubmission verificationSubmission, 
			UserProfile userProfile, 
			String orcId) {
		validateField(verificationSubmission.getCompany(), userProfile.getCompany(), "Company");
		validateField(verificationSubmission.getFirstName(), userProfile.getFirstName(), "First name");
		validateField(verificationSubmission.getLastName(), userProfile.getLastName(), "Last name");
		validateField(verificationSubmission.getLocation(), userProfile.getLocation(), "Location");
		validateField(AliasUtils.getUniqueAliasName(verificationSubmission.getOrcid()), 
					AliasUtils.getUniqueAliasName(orcId), "ORCID");
		if (verificationSubmission.getEmails()==null || verificationSubmission.getEmails().isEmpty()) 
			throw new InvalidModelException("Email(s) cannot be omitted.");
		Set<String> verificationEmailSet = new HashSet<String>(verificationSubmission.getEmails());
		Set<String> userProfileEmailSet = new HashSet<String>(userProfile.getEmails());
		if (!verificationEmailSet.equals(userProfileEmailSet))
			throw new InvalidModelException("Email(s) in submission must match those registered with the system.");
	}
	
	public static void validateField(String submissionField, String userProfileField, String fieldName) {
		if (submissionField==null) throw new InvalidModelException(fieldName+" cannot be null.");
		if (!submissionField.equals(userProfileField)) throw new InvalidModelException(fieldName+" does not match value in user profile.");
	}

	@Override
	public VerificationPagedResults listVerificationSubmissions(
			UserInfo userInfo, List<VerificationStateEnum> currentVerificationState,
			Long verifiedUserId, long limit, long offset) {
		// check that user is in ACT (or an admin)
		if(!authorizationManager.isACTTeamMemberOrAdmin(userInfo))
			throw new UnauthorizedException("You are not a member of the Synapse Access and Compliance Team.");
		List<VerificationSubmission>  list = verificationDao.listVerificationSubmissions(currentVerificationState, verifiedUserId, limit, offset);
		long totalNumberOfResults = verificationDao.countVerificationSubmissions(currentVerificationState, verifiedUserId);
		VerificationPagedResults result = new VerificationPagedResults();
		result.setResults(list);
		result.setTotalNumberOfResults(totalNumberOfResults);
		return result;
	}

	@WriteTransaction
	@Override
	public void changeSubmissionState(UserInfo userInfo,
			long verificationSubmissionId, VerificationState newState) {
		// check that user is in ACT (or an admin)
		if(!authorizationManager.isACTTeamMemberOrAdmin(userInfo))
			throw new UnauthorizedException("You are not a member of the Synapse Access and Compliance Team.");
		// check that the state transition is allowed, by comparing to the current state
		VerificationStateEnum currentState = verificationDao.getVerificationState(verificationSubmissionId);
		if (!isStateTransitionAllowed(currentState, newState.getState()))
			throw new InvalidModelException("Cannot transition verification submission from "+currentState+" to "+newState.getState());
		verificationDao.appendVerificationSubmissionState(verificationSubmissionId, newState);
	}
	
	public static boolean isStateTransitionAllowed(VerificationStateEnum currentState, VerificationStateEnum newState) {
		switch (currentState) {
		case SUBMITTED:
			return newState==APPROVED || newState==REJECTED;
		case APPROVED:
			return newState==SUSPENDED;
		case REJECTED:
			return false;
		case SUSPENDED:
			return false;
		default:
			throw new InvalidModelException("Unexpected state "+currentState);
		}
	}

}
