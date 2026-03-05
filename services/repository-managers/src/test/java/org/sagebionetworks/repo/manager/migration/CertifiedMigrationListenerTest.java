package org.sagebionetworks.repo.manager.migration;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.CertifiedUsersDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOGroupMembers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class CertifiedMigrationListenerTest {
    @Autowired
    private GroupMembersDAO groupMembersDAO;
    @Autowired
    private UserManager userManager;
    @Autowired
    private CertifiedUsersDAO certifiedUsersDAO;
    @Autowired
    private CertifiedUserMigrationListener certifiedUserMigrationListener;

    private UserInfo userOne;
    private UserInfo userTwo;
    private UserInfo userThree;
    private UserInfo adminUserInfo;
    private List<Long> usersToBeDeleted = new ArrayList<>();

    @BeforeEach
    public void before() {
        adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

        userOne = userManager.createOrGetTestUser(adminUserInfo, new NewUser().setEmail(UUID.randomUUID() + "@test.com")
                .setUserName(UUID.randomUUID().toString()));
        usersToBeDeleted.add(userOne.getId());
        userTwo = userManager.createOrGetTestUser(adminUserInfo, new NewUser().setEmail(UUID.randomUUID() + "@test.com")
                .setUserName(UUID.randomUUID().toString()));
        usersToBeDeleted.add(userTwo.getId());
        userThree = userManager.createOrGetTestUser(adminUserInfo, new NewUser().setEmail(UUID.randomUUID() + "@test.com")
                .setUserName(UUID.randomUUID().toString()));
        usersToBeDeleted.add(userThree.getId());

        groupMembersDAO.addMembers(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
                List.of(userOne.getId().toString(), userTwo.getId().toString()));

    }

    @AfterEach
    public void after() {
        for (Long userId : usersToBeDeleted) {
            userManager.deletePrincipal(adminUserInfo, userId);
        }
    }

    @Test
    public void testMigrationListenerAddUserToCertifiedUserTable() {
        DBOGroupMembers dboGroupMemberOne = new DBOGroupMembers();
        dboGroupMemberOne.setGroupId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
        dboGroupMemberOne.setMemberId(userOne.getId());

        DBOGroupMembers dboGroupMemberTwo = new DBOGroupMembers();
        dboGroupMemberTwo.setGroupId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
        dboGroupMemberTwo.setMemberId(userTwo.getId());

        DBOGroupMembers dboGroupMemberThree = new DBOGroupMembers();
        dboGroupMemberThree.setGroupId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
        dboGroupMemberThree.setMemberId(userThree.getId());

        assertFalse(certifiedUsersDAO.isCertifiedUser(userOne.getId().toString()));
        assertFalse(certifiedUsersDAO.isCertifiedUser(userTwo.getId().toString()));
        assertFalse(certifiedUsersDAO.isCertifiedUser(userThree.getId().toString()));

        //call under test
        certifiedUserMigrationListener.afterCreateOrUpdate(null,
                List.of(dboGroupMemberOne, dboGroupMemberTwo, dboGroupMemberThree, dboGroupMemberOne));

        assertTrue(certifiedUsersDAO.isCertifiedUser(userOne.getId().toString()));
        assertTrue(certifiedUsersDAO.isCertifiedUser(userTwo.getId().toString()));
        assertFalse(certifiedUsersDAO.isCertifiedUser(userThree.getId().toString()));
    }
}
