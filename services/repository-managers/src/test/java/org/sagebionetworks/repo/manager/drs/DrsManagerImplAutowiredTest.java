package org.sagebionetworks.repo.manager.drs;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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
    private List<Folder> folders = new ArrayList<>();
    private List<FileEntity> fileEntities = new ArrayList<>();
    private List<String> entitiesToDelete = Lists.newArrayList();
    private CreateProjectHierarchyTestDataUtil testData;

    private UserInfo adminUserInfo;
    private UserInfo userInfo;


    @BeforeEach
    public void before() {
        testData = new CreateProjectHierarchyTestDataUtil(entityManager, aclDaoHelper, fileHandleObjectHelper);
        entitiesToDelete = testData.entitiesToDelete;
        adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
        boolean acceptsTermsOfUse = true;
        String userName = UUID.randomUUID().toString();
        userInfo = userManager.createOrGetTestUser(adminUserInfo,
                new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), acceptsTermsOfUse);
    }

    @AfterEach
    public void after() {
        for (String id : Lists.reverse(entitiesToDelete)) {
            entityManager.deleteEntity(adminUserInfo, id);
        }
    }

    @Test
    public void testDrsObjectIsAccessedByUserHavingAccess() {
        testData.createProjectHierachy(1, adminUserInfo, userInfo);
        project = testData.project;
        folders = testData.folders;
        fileEntities = testData.fileEntities;

        final String drsObjectId = fileEntities.get(0).getId() + ".1";
        final DrsObject drsObject = drsManager.getDrsObject(userInfo.getId(), drsObjectId);
        assertNotNull(drsObject);
        assertEquals(drsObject.getId(), drsObjectId);
    }
}
