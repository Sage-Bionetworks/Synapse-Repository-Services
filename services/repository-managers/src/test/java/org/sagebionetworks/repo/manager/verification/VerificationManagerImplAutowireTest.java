package org.sagebionetworks.repo.manager.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class VerificationManagerImplAutowireTest {

	private static final String USER_NAME = UUID.randomUUID().toString() + "username";
	private static final String EMAIL = UUID.randomUUID().toString() + "@test.com";
	private static final String FIRST_NAME = "fname";
	private static final String LAST_NAME = "lname";
	private static final String COMPANY = "company";
	private static final String LOCATION = "location";
	private static final String ORCID = "https://orcid.org/" + generateORCID();
	
	private static String generateORCID() {
		StringJoiner id = new StringJoiner("-");
		IntStream.range(0, 4).forEach(_i -> id.add(RandomStringUtils.randomNumeric(4)));
		return id.toString();
	}
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private VerificationManager verificationManager;
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	
	private Long submissionId;

	@BeforeEach
	public void before() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setFirstName(FIRST_NAME);
		nu.setLastName(LAST_NAME);
		nu.setEmail(EMAIL);
		nu.setUserName(USER_NAME);

		
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		userInfo = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
		
		userManager.bindAlias(ORCID, AliasType.USER_ORCID, userInfo.getId());
		
		UserProfile profile = userProfileManager.getUserProfile(userInfo.getId().toString());
		
		profile.setCompany(COMPANY);
		profile.setLocation(LOCATION);
		
		userProfileManager.updateUserProfile(userInfo, profile);
	}
	
	@AfterEach
	public void after() {
		runIf(true, () -> userManager.deletePrincipal(adminUserInfo, userInfo.getId()));
		runIf(submissionId != null, () -> verificationManager.deleteVerificationSubmission(userInfo, submissionId));
	}
	
	private void runIf(boolean condition, Runnable ex) {
		if (condition) {
			try {
				ex.run();
			} catch (Exception ignore) {
			}
		}
	}
	
	@Test
	public void testVerificationWithNotes() {
		VerificationSubmission submission = getSubmission(userInfo);
		submission = verificationManager.createVerificationSubmission(userInfo, submission);
		submissionId = Long.valueOf(submission.getId());
		
		List<VerificationState> states = submission.getStateHistory();
		
		assertEquals(1, states.size());
		assertNull(states.get(0).getNotes());
		
		VerificationState newState = new VerificationState();
		newState.setState(VerificationStateEnum.REJECTED);
		newState.setReason("Some Reason");
		newState.setNotes("Some notes");
		
		verificationManager.changeSubmissionState(adminUserInfo, submissionId, newState);
		
		// From the user perspective
		VerificationSubmission currentSubmission = userProfileManager.getCurrentVerificationSubmission(userInfo.getId());
		
		states = currentSubmission.getStateHistory();
		
		assertEquals(2, states.size());
		assertNull(states.get(states.size() - 1).getNotes());
		
		// From the ACT perspective
		List<VerificationSubmission> submissions = verificationManager.listVerificationSubmissions(adminUserInfo, null, userInfo.getId(), 10, 0).getResults();
		
		assertEquals(1, submissions.size());
		
		currentSubmission = submissions.get(0);
		
		states = currentSubmission.getStateHistory();
		
		assertEquals(2, states.size());
		assertNull(states.get(0).getNotes());
		assertEquals("Some notes", states.get(states.size() - 1).getNotes());
	}
	
	private VerificationSubmission getSubmission(UserInfo userInfo) {
		
		UserProfile profile = userProfileManager.getUserProfile(userInfo.getId().toString());
		
		VerificationSubmission submission = new VerificationSubmission();
		
		submission.setFirstName(profile.getFirstName());
		submission.setLastName(profile.getLastName());
		submission.setLocation(profile.getLocation());
		submission.setCompany(profile.getCompany());
		submission.setEmails(profile.getEmails());
		submission.setOrcid(ORCID);
		
		return submission;
	}
	
}
