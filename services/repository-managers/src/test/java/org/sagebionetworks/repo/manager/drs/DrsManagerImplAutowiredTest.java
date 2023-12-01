package org.sagebionetworks.repo.manager.drs;

import com.google.common.collect.Lists;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.GlobalConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.CallersContext;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.drs.AccessUrl;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class DrsManagerImplAutowiredTest {

    @Autowired
    private DrsManager drsManager;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private AccessControlListObjectHelper aclDaoHelper;
    @Autowired
    private SynapseS3Client s3Client;
    @Autowired
    private FileHandleDao fileHandleDao;
    @Autowired
    FileHandleManager fileHandleManager;
    private Project project;
    private FileEntity file;
    private final List<String> entitiesToDelete = Lists.newArrayList();
    private List<S3FileHandle> fileHandlesToDelete = Lists.newArrayList();
    private UserInfo adminUserInfo;
    private UserInfo userInfo;
    private String sesionId;


    @BeforeEach
    public void before() {
    	sesionId = UUID.randomUUID().toString();
        adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
        adminUserInfo.setContext(new CallersContext().setSessionId(sesionId));
        final boolean acceptsTermsOfUse = true;
        final String userName = UUID.randomUUID().toString();
        userInfo = userManager.createOrGetTestUser(adminUserInfo,
                new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), acceptsTermsOfUse);
        userInfo.setContext(new CallersContext().setSessionId(sesionId));
        ThreadContext.put(GlobalConstants.SESSION_ID, sesionId);
    }

    @AfterEach
    public void after() {
        // delete project and file entities
        for (final String id : Lists.reverse(entitiesToDelete)) {
            entityManager.deleteEntity(adminUserInfo, id);
        }

        if (fileHandlesToDelete != null && s3Client != null) {
            // Delete file handle created
            for (S3FileHandle meta : fileHandlesToDelete) {
                // delete the file from S3.
                s3Client.deleteObject(meta.getBucketName(), meta.getKey());
                if (meta.getId() != null) {
                    // We also need to delete the data from the database
                    fileHandleDao.delete(meta.getId());
                }
            }
        }
		ThreadContext.clearAll();
    }

    @Test
    public void testGetDrsObjectIsAccessedByUserHavingAccess() {
        createTestData(adminUserInfo, userInfo);
        final String drsObjectId = file.getId() + "." + file.getVersionNumber();
        final DrsObject drsObject = drsManager.getDrsObject(userInfo.getId(), drsObjectId, false);
        assertNotNull(drsObject);
        assertEquals(drsObject.getId(), drsObjectId);
    }

    @Test
    public void testGetAccessUrlByUserHavingAccess() {
        createTestData(adminUserInfo, userInfo);
        final String drsObjectId = KeyFactory.idAndVersion(file.getId(), file.getVersionNumber()).toString();
        final String accessId = drsManager.getDrsObject(adminUserInfo.getId(), drsObjectId, false)
                .getAccess_methods().get(0).getAccess_id();
        // user requires download access
        aclDaoHelper.update(project.getId(), ObjectType.ENTITY, a -> {
            a.getResourceAccess().add(createResourceAccess(userInfo.getId(), ACCESS_TYPE.DOWNLOAD));
        });

        //call under test
        final AccessUrl accessUrl = drsManager.getAccessUrl(userInfo.getId(), drsObjectId, accessId);
        assertNotNull(accessUrl);
        assertNotNull(accessUrl.getUrl());
    }

    @Test
    public void testGetAccessUrlByUserHavingNoAccess() {
        createTestData(adminUserInfo, userInfo);
        final String drsObjectId = KeyFactory.idAndVersion(file.getId(), file.getVersionNumber()).toString();
        final String accessId = drsManager.getDrsObject(adminUserInfo.getId(), drsObjectId, false)
                .getAccess_methods().get(0).getAccess_id();

        //call under test
        assertEquals("You lack DOWNLOAD access to the requested entity.", assertThrows(UnauthorizedException.class, () -> {
            drsManager.getAccessUrl(userInfo.getId(), drsObjectId, accessId);
        }).getMessage());
    }

    public void createTestData(final UserInfo createdByUserInfo, final UserInfo permissionGrantedTo) {
        try {
            project = entityManager.getEntity(createdByUserInfo, createProject(createdByUserInfo), Project.class);
            entitiesToDelete.add(project.getId());

            aclDaoHelper.update(project.getId(), ObjectType.ENTITY, a -> {
                a.getResourceAccess().add(createResourceAccess(permissionGrantedTo.getId(), ACCESS_TYPE.READ));
            });

            final S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(createdByUserInfo
                            .getId().toString(),new Date(), "Test file content".getBytes(StandardCharsets.UTF_8),
                    "TestFile.txt", ContentTypeUtil.TEXT_PLAIN_UTF8, null);
            file = entityManager
                    .getEntity(createdByUserInfo,
                            entityManager.createEntity(createdByUserInfo, new FileEntity().setName("TestFile")
                                    .setParentId(project.getId()).setDataFileHandleId(fileHandle.getId()), null),
                            FileEntity.class);
            entitiesToDelete.add(file.getId());
            fileHandlesToDelete.add(fileHandle);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String createProject(final UserInfo createdByUser) {
        return entityManager.createEntity(createdByUser, new Project().setName(null), null);
    }
}
