package org.sagebionetworks.repo.manager.drs;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private FileHandleObjectHelper fileHandleObjectHelper;
    private Project project;
    private FileEntity file;
    private final List<String> entitiesToDelete = Lists.newArrayList();

    private UserInfo adminUserInfo;
    private UserInfo userInfo;


    @BeforeEach
    public void before() {
        adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
        final boolean acceptsTermsOfUse = true;
        final String userName = UUID.randomUUID().toString();
        userInfo = userManager.createOrGetTestUser(adminUserInfo,
                new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), acceptsTermsOfUse);
    }

    @AfterEach
    public void after() {
        for (final String id : Lists.reverse(entitiesToDelete)) {
            entityManager.deleteEntity(adminUserInfo, id);
        }
    }

    @Test
    public void testDrsObjectIsAccessedByUserHavingAccess() {
        createTestData(adminUserInfo, userInfo);
        final String drsObjectId = file.getId() + "." + file.getVersionNumber();
        final DrsObject drsObject = drsManager.getDrsObject(userInfo.getId(), drsObjectId, false);
        assertNotNull(drsObject);
        assertEquals(drsObject.getId(), drsObjectId);
    }

    public void createTestData(final UserInfo createdByUserInfo, final UserInfo permissionGrantedTo) {
        project = entityManager.getEntity(createdByUserInfo, createProject(createdByUserInfo), Project.class);
        entitiesToDelete.add(project.getId());

        aclDaoHelper.update(project.getId(), ObjectType.ENTITY, a -> {
            a.getResourceAccess().add(createResourceAccess(permissionGrantedTo.getId(), ACCESS_TYPE.READ));
        });

        final S3FileHandle fileHandle = fileHandleObjectHelper.createS3(f -> {
            f.setFileName("f0");
        });
        file = entityManager
                .getEntity(createdByUserInfo,
                        entityManager.createEntity(createdByUserInfo, new FileEntity().setName("TestFile")
                                .setParentId(project.getId()).setDataFileHandleId(fileHandle.getId()), null),
                        FileEntity.class);
        entitiesToDelete.add(file.getId());
    }

    private String createProject(final UserInfo createdByUser) {
        return entityManager.createEntity(createdByUser, new Project().setName(null), null);
    }


}
