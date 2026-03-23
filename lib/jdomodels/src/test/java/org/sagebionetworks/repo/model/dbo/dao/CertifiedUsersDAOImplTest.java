package org.sagebionetworks.repo.model.dbo.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.CertifiedUsersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:jdomodels-test-context.xml"})
public class CertifiedUsersDAOImplTest {

    @Autowired
    CertifiedUsersDAO certifiedUsersDAO;
    @Autowired
    private UserGroupDAO userGroupDAO;
    private List<String> groupsToDelete;

    private Long longUserId;

    private Long userIdTwo;


    @BeforeEach
    public void setUp() throws Exception {
        groupsToDelete = new ArrayList<String>();

        UserGroup group = new UserGroup();
        group.setIsIndividual(true);
        group.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
        group.setCreationDate(new Date());
        longUserId = userGroupDAO.create(group);
        groupsToDelete.add(longUserId.toString());
        userIdTwo = userGroupDAO.create(group);
        groupsToDelete.add(userIdTwo.toString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        for (String toDelete : groupsToDelete) {
            userGroupDAO.delete(toDelete);
        }

    }

    @Test
    public void testCURDCertifiedUser() {
        //call under test
        //check if user id already certified
        Boolean isCertified = certifiedUsersDAO.isCertifiedUser(longUserId.toString());
        assertFalse(isCertified);

        //add user id to certified users
        certifiedUsersDAO.addCertifiedUser(longUserId, true);
        assertTrue(certifiedUsersDAO.isCertifiedUser(longUserId.toString()));

        //remove user id from certified users
        certifiedUsersDAO.removeCertifiedUser(longUserId);
        assertFalse(certifiedUsersDAO.isCertifiedUser(longUserId.toString()));
    }

    @Test
    public void testaddCertifiedUserIsIndividualFalse() {
        //call under test
        String message = assertThrows(IllegalArgumentException.class, () -> {
            certifiedUsersDAO.addCertifiedUser(longUserId, false);
        }).getMessage();

        assertEquals("Only individuals can be added as certified users.", message);
    }

    @Test
    public void testAreAllCertifiedUser() {
        //call under test
        //check if user id already certified
        Boolean isCertified = certifiedUsersDAO.isCertifiedUser(longUserId.toString());
        assertFalse(isCertified);

        //add user id to certified users
        certifiedUsersDAO.addCertifiedUser(longUserId, true);
        assertTrue(certifiedUsersDAO.isCertifiedUser(longUserId.toString()));

        //remove user id from certified users
        certifiedUsersDAO.removeCertifiedUser(longUserId);
        assertFalse(certifiedUsersDAO.isCertifiedUser(longUserId.toString()));
    }

    @Test
    public void testIsCertifiedWithWrongId() {
        // both user are uncertified
        assertFalse(certifiedUsersDAO.isCertifiedUser(longUserId.toString()));
        assertFalse(certifiedUsersDAO.isCertifiedUser(userIdTwo.toString()));

        // add one user to certified users
        certifiedUsersDAO.addCertifiedUser(longUserId, true);
        assertTrue(certifiedUsersDAO.isCertifiedUser(longUserId.toString()));
        assertFalse(certifiedUsersDAO.isCertifiedUser(userIdTwo.toString()));

        //call under test. Returns false because all users in the set are not certified.
        Boolean isCertified = certifiedUsersDAO.areAllCertifiedUsers(Set.of(userIdTwo.toString(), longUserId.toString()));
        assertFalse(isCertified);


    }

    @Test
    public void testIsCertifiedWithNullId() {
        //call under test
        Boolean isCertified = certifiedUsersDAO.isCertifiedUser(null);
        assertFalse(isCertified);

    }

    @Test
    public void testIsCertifiedWithEmptyString() {
        //call under test
        Boolean isCertified = certifiedUsersDAO.isCertifiedUser("");
        assertFalse(isCertified);

    }

    @Test
    public void testAreCertifiedWithNull() {
        //call under test
        Boolean isCertified = certifiedUsersDAO.areAllCertifiedUsers(null);
        assertFalse(isCertified);

    }

    @Test
    public void testAreCertifiedWithEmptySet() {
        //call under test
        Boolean isCertified = certifiedUsersDAO.areAllCertifiedUsers(Collections.emptySet());
        assertFalse(isCertified);

    }

}
