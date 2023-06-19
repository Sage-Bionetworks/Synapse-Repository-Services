package org.sagebionetworks.snapshot.workers;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.verification.VerificationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class VerificationSubmissionObjectSnapshotWorkerIntegrationTest {
    private static final String FIRST_NAME = "fname";
    private static final String LAST_NAME = "lname";
    private static final String COMPANY = "company";
    private static final String LOCATION = "location";
    private static final Random random = new Random();
    @Autowired
    private VerificationManager verificationManager;
    @Autowired
    private FileHandleManager fileHandleManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private ObjectRecordDAO objectRecordDAO;
    @Autowired
    private UserProfileManager userProfileManager;
    @Autowired
    private FileHandleDao fileMetadataDao;
    @Autowired
    private VerificationDAO verificationDAO;
    private String type;
    private UserInfo userInfo;
    private List<String> emails;
    private String orcid;
    private S3FileHandle fileHandleToDelete;
    private UserInfo adminUserInfo;
    private List<Long> verificationSubmissionTobeDeleted = new ArrayList<>();

    private static String randomOrcid() {
        StringBuilder sb = new StringBuilder("https://orcid.org/");
        for (int i = 0; i < 4; i++) {
            sb.append(StringUtils.leftPad("" + random.nextInt(10000), 4, "0"));
            if (i < 3) sb.append("-");
        }
        return sb.toString();
    }

    @BeforeEach
    public void before() {
        adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
        NewUser user = new NewUser();
        user.setEmail(UUID.randomUUID() + "@test.com");
        emails = Arrays.asList(user.getEmail());
        user.setUserName(UUID.randomUUID().toString());
        userInfo = userManager.getUserInfo(userManager.createUser(user));
        this.orcid = randomOrcid();
        userManager.bindAlias(orcid, AliasType.USER_ORCID, userInfo.getId());

        UserProfile userProfile = userProfileManager.getUserProfile(userInfo.getId().toString());
        assertNotNull(userProfile.getEmails());
        userProfile.setFirstName(FIRST_NAME);
        userProfile.setLastName(LAST_NAME);
        userProfile.setCompany(COMPANY);
        userProfile.setLocation(LOCATION);
        userProfileManager.updateUserProfile(userInfo, userProfile);

        type = VerificationSubmission.class.getSimpleName().toLowerCase();
    }

    @AfterEach
    public void after() {
        for (Long id : verificationSubmissionTobeDeleted) {
            verificationDAO.deleteVerificationSubmission(id);
        }

        if (fileHandleToDelete != null) {
            fileMetadataDao.delete(fileHandleToDelete.getId());
        }
    }

    @Test
    public void testVerificationSubmissionSnapshotCreation() throws Exception {
        createTestData(userInfo);
        Set<String> keys = ObjectSnapshotWorkerIntegrationTestUtils.listAllKeys(objectRecordDAO, type);
        VerificationSubmission vs = new VerificationSubmission();
        vs.setFirstName(FIRST_NAME);
        vs.setLastName(LAST_NAME);
        vs.setCompany(COMPANY);
        vs.setLocation(LOCATION);
        vs.setEmails(emails);
        String fileHandleId = fileHandleToDelete.getId();
        AttachmentMetadata attachmentMetadata = new AttachmentMetadata();
        attachmentMetadata.setId(fileHandleId);
        vs.setAttachments(Collections.singletonList(attachmentMetadata));
        vs.setOrcid(orcid);
        VerificationSubmission createdVs = verificationManager.createVerificationSubmission(userInfo, vs);
        ObjectRecord expectedRecord = new ObjectRecord();
        expectedRecord.setJsonClassName(createdVs.getClass().getSimpleName().toLowerCase());
        expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(createdVs));
        verificationSubmissionTobeDeleted.add(Long.parseLong(createdVs.getId()));
        //call under test
        assertTrue(ObjectSnapshotWorkerIntegrationTestUtils.waitForObjects(keys, Arrays.asList(expectedRecord), objectRecordDAO, type));
    }

    @Test
    public void testVerificationSubmissionSnapshotUpdate() throws Exception {
        createTestData(userInfo);
        Set<String> keys = ObjectSnapshotWorkerIntegrationTestUtils.listAllKeys(objectRecordDAO, type);
        VerificationSubmission vs = new VerificationSubmission();
        vs.setFirstName(FIRST_NAME);
        vs.setLastName(LAST_NAME);
        vs.setCompany(COMPANY);
        vs.setLocation(LOCATION);
        vs.setEmails(emails);
        String fileHandleId = fileHandleToDelete.getId();
        AttachmentMetadata attachmentMetadata = new AttachmentMetadata();
        attachmentMetadata.setId(fileHandleId);
        vs.setAttachments(Collections.singletonList(attachmentMetadata));
        vs.setOrcid(orcid);
        VerificationSubmission createdVs = verificationManager.createVerificationSubmission(userInfo, vs);
        Long vsId = Long.parseLong(createdVs.getId());
        verificationSubmissionTobeDeleted.add(vsId);
        VerificationState newState = new VerificationState();
        newState.setState(VerificationStateEnum.APPROVED);
        //update state to approved
        verificationManager.changeSubmissionState(adminUserInfo, vsId, newState);

        VerificationSubmission expectedVs = verificationDAO.getCurrentVerificationSubmissionForUser(userInfo.getId());
        ObjectRecord expectedRecord = new ObjectRecord();
        expectedRecord.setJsonClassName(expectedVs.getClass().getSimpleName().toLowerCase());
        expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(expectedVs));
        //call under test
        assertTrue(ObjectSnapshotWorkerIntegrationTestUtils.waitForObjects(keys, Arrays.asList(expectedRecord), objectRecordDAO, type));
    }

    public void createTestData(final UserInfo createdByUserInfo) {
        try {
            fileHandleToDelete = fileHandleManager.createFileFromByteArray(createdByUserInfo.getId().toString(), new Date(),
                    "Test file content".getBytes(StandardCharsets.UTF_8), "TestFile.txt", ContentTypeUtil.TEXT_PLAIN_UTF8, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
