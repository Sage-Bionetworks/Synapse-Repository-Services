package org.sagebionetworks.repo.manager.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.utils.ContentTypeUtil;
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
	private FileHandleManager fileHandleManager;

	@Autowired
	private VerificationManager verificationManager;
	
	@Autowired
	private SynapseS3Client s3Client;

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
		runIf(submissionId != null, () -> verificationManager.deleteVerificationSubmission(userInfo, submissionId));
		runIf(true, () -> userManager.deletePrincipal(adminUserInfo, userInfo.getId()));
	}

	private void runIf(boolean condition, Runnable ex) {
		if (condition) {
			try {
				ex.run();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}
	}

	@Test
	public void testVerificationWithNotes() {
		VerificationSubmission submission = newSubmission(userInfo, null);
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
		List<VerificationSubmission> submissions = verificationManager
				.listVerificationSubmissions(adminUserInfo, null, userInfo.getId(), 10, 0).getResults();

		assertEquals(1, submissions.size());

		currentSubmission = submissions.get(0);

		states = currentSubmission.getStateHistory();

		assertEquals(2, states.size());
		assertNull(states.get(0).getNotes());
		assertEquals("Some notes", states.get(states.size() - 1).getNotes());
	}

	@Test
	public void testRemoveAttachmentsAfterRejection() throws Exception {
		testRemoveAttachmentsAfterState(VerificationStateEnum.REJECTED);
	}
	
	@Test
	public void testRemoveAttachmentsAfterApproval() throws Exception {
		testRemoveAttachmentsAfterState(VerificationStateEnum.APPROVED);
	}
	
	@Test
	public void testRemoveAttachmentesAfterSubmissionDeletion() throws Exception {
		S3FileHandle fh1 = createFileHandle(userInfo.getId().toString());
		S3FileHandle fh2 = createFileHandle(userInfo.getId().toString());

		List<String> fileHandleIds = Arrays.asList(fh1.getId(), fh2.getId());

		VerificationSubmission submission = newSubmission(userInfo, fileHandleIds);

		submission = verificationManager.createVerificationSubmission(userInfo, submission);
		submissionId = Long.valueOf(submission.getId());

		verificationManager.deleteVerificationSubmission(userInfo, submissionId);
		
		assertDeleted(fh1, fh2);
	}
	
	private void testRemoveAttachmentsAfterState(VerificationStateEnum state) throws Exception {
		S3FileHandle fh1 = createFileHandle(userInfo.getId().toString());
		S3FileHandle fh2 = createFileHandle(userInfo.getId().toString());

		List<String> fileHandleIds = Arrays.asList(fh1.getId(), fh2.getId());

		VerificationSubmission submission = newSubmission(userInfo, fileHandleIds);

		submission = verificationManager.createVerificationSubmission(userInfo, submission);
		submissionId = Long.valueOf(submission.getId());

		assertEquals(2, submission.getAttachments().size());

		VerificationState rejection = new VerificationState();
		rejection.setState(state);
		rejection.setReason("Some Reason");
		rejection.setNotes("Some notes");

		verificationManager.changeSubmissionState(adminUserInfo, submissionId, rejection);

		VerificationSubmission currentSubmission = userProfileManager.getCurrentVerificationSubmission(userInfo.getId());

		assertTrue(currentSubmission.getAttachments().isEmpty());
		assertDeleted(fh1, fh2);

	}


	private VerificationSubmission newSubmission(UserInfo userInfo, List<String> fileHandleIds) {

		UserProfile profile = userProfileManager.getUserProfile(userInfo.getId().toString());

		VerificationSubmission submission = new VerificationSubmission();

		submission.setFirstName(profile.getFirstName());
		submission.setLastName(profile.getLastName());
		submission.setLocation(profile.getLocation());
		submission.setCompany(profile.getCompany());
		submission.setEmails(profile.getEmails());
		submission.setOrcid(ORCID);

		if (fileHandleIds != null) {

			List<AttachmentMetadata> attachments = fileHandleIds.stream().map(id -> {
				AttachmentMetadata attachment = new AttachmentMetadata();
				attachment.setId(id);
				return attachment;
			}).collect(Collectors.toList());

			submission.setAttachments(attachments);
		}

		return submission;
	}
	
	private void assertDeleted(S3FileHandle... handles) {
		for (S3FileHandle handle : handles) {
			assertFalse(s3Client.doesObjectExist(handle.getBucketName(), handle.getKey()));
		}
	}

	private S3FileHandle createFileHandle(String createdBy) throws Exception {
		String content = UUID.randomUUID().toString();
		String fileName = UUID.randomUUID().toString();

		return fileHandleManager.createFileFromByteArray(createdBy, new Date(), content.getBytes(StandardCharsets.UTF_8), fileName,
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);
	}

}
