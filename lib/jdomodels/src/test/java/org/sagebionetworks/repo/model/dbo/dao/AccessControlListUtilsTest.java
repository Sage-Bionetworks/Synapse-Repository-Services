package org.sagebionetworks.repo.model.dbo.dao;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;

import java.util.Date;;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccessControlListUtilsTest {

    public AccessControlList creatACL() {
        return new AccessControlList().setId("10000").setEtag("random").setCreationDate(new Date());
    }

    @Test
    public void testValidateAclResourceAccessForEntityOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.CREATE, ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ,
                        ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.CHANGE_SETTINGS, ACCESS_TYPE.DELETE, ACCESS_TYPE.MODERATE,
                        ACCESS_TYPE.UPDATE, ACCESS_TYPE.SEND_MESSAGE, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, ACCESS_TYPE.DELETE_SUBMISSION,
                        ACCESS_TYPE.PARTICIPATE, ACCESS_TYPE.READ_PRIVATE_SUBMISSION, ACCESS_TYPE.SUBMIT, ACCESS_TYPE.UPDATE_SUBMISSION,
                        ACCESS_TYPE.UPLOAD)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.SUBMIT))
        ));

        AccessControlListUtils.validateACL(acl, ObjectType.ENTITY);
    }

    @Test
    public void testValidateAclResourceAccessForEvaluationOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.READ, ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.CREATE,
                        ACCESS_TYPE.DELETE, ACCESS_TYPE.DELETE_SUBMISSION, ACCESS_TYPE.READ_PRIVATE_SUBMISSION, ACCESS_TYPE.SUBMIT,
                        ACCESS_TYPE.UPDATE, ACCESS_TYPE.UPDATE_SUBMISSION, ACCESS_TYPE.PARTICIPATE, ACCESS_TYPE.CHANGE_SETTINGS,
                        ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.MODERATE)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.MODERATE))
        ));

        AccessControlListUtils.validateACL(acl, ObjectType.EVALUATION);
    }

    @Test
    public void testValidateAclResourceAccessForTeamOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.DELETE, ACCESS_TYPE.READ, ACCESS_TYPE.SEND_MESSAGE,
                        ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE, ACCESS_TYPE.DOWNLOAD,
                        ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.CHANGE_SETTINGS, ACCESS_TYPE.MODERATE, ACCESS_TYPE.DELETE_SUBMISSION,
                        ACCESS_TYPE.SUBMIT, ACCESS_TYPE.UPDATE_SUBMISSION, ACCESS_TYPE.PARTICIPATE)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.READ))
        ));

        AccessControlListUtils.validateACL(acl, ObjectType.TEAM);
    }

    @Test
    public void testValidateAclResourceAccessForFormGroupOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.CHANGE_PERMISSIONS,
                        ACCESS_TYPE.READ, ACCESS_TYPE.READ_PRIVATE_SUBMISSION, ACCESS_TYPE.SUBMIT)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
        ));

        AccessControlListUtils.validateACL(acl, ObjectType.FORM_GROUP);
    }

    @Test
    public void testValidateAclResourceAccessForOrganizationOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.CHANGE_PERMISSIONS,
                        ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.CREATE))
        ));

        AccessControlListUtils.validateACL(acl, ObjectType.ORGANIZATION);
    }

    @Test
    public void testValidateAclResourceAccessForAccessRequirementOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.REVIEW_SUBMISSIONS,
                        ACCESS_TYPE.EXEMPTION_ELIGIBLE)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.EXEMPTION_ELIGIBLE))
        ));

        AccessControlListUtils.validateACL(acl, ObjectType.ACCESS_REQUIREMENT);
    }

    @Test
    public void testValidateInvalidAclResourceAccessForEntityOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.DELETE, ACCESS_TYPE.UPDATE, ACCESS_TYPE.DOWNLOAD)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.READ, ACCESS_TYPE.EXEMPTION_ELIGIBLE))
        ));

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.ENTITY);
        }).getMessage();

        assertEquals("The access type EXEMPTION_ELIGIBLE is not allowed for ENTITY.", message);
    }

    @Test
    public void testValidateInvalidAclResourceAccessForEvaluationOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.DELETE, ACCESS_TYPE.UPDATE, ACCESS_TYPE.DOWNLOAD)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.READ, ACCESS_TYPE.UPLOAD))
        ));

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.EVALUATION);
        }).getMessage();

        assertEquals("The access type UPLOAD is not allowed for EVALUATION.", message);
    }

    @Test
    public void testValidateInvalidAclResourceAccessForTeamOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.DELETE, ACCESS_TYPE.UPDATE, ACCESS_TYPE.DOWNLOAD)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
        ));

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.TEAM);
        }).getMessage();

        assertEquals("The access type READ_PRIVATE_SUBMISSION is not allowed for TEAM.", message);
    }

    @Test
    public void testValidateInvalidAclResourceAccessForFormGroupOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.DELETE, ACCESS_TYPE.READ, ACCESS_TYPE.READ_PRIVATE_SUBMISSION)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
        ));

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.FORM_GROUP);
        }).getMessage();

        assertEquals("The access type DELETE is not allowed for FORM_GROUP.", message);
    }

    @Test
    public void testValidateInvalidAclResourceAccessForOrganizationOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.DELETE, ACCESS_TYPE.READ, ACCESS_TYPE.UPLOAD)),
                new ResourceAccess().setPrincipalId(2L).setAccessType(Set.of(ACCESS_TYPE.READ))
        ));

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.ORGANIZATION);
        }).getMessage();

        assertEquals("The access type UPLOAD is not allowed for ORGANIZATION.", message);
    }

    @Test
    public void testValidateInvalidAclResourceAccessForAccessRequirementOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.REVIEW_SUBMISSIONS, ACCESS_TYPE.READ))
        ));

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.ACCESS_REQUIREMENT);
        }).getMessage();

        assertEquals("The access type READ is not allowed for ACCESS_REQUIREMENT.", message);
    }

    @Test
    public void testValidateAclWithInvalidOwnerType() {
        AccessControlList acl = creatACL().setResourceAccess(Set.of(
                new ResourceAccess().setPrincipalId(1L).setAccessType(Set.of(ACCESS_TYPE.REVIEW_SUBMISSIONS, ACCESS_TYPE.READ))
        ));

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.JSON_SCHEMA);
        }).getMessage();

        assertEquals("The Acl of owner type JSON_SCHEMA is not allowed.", message);
    }

    @Test
    public void testForNullAcl() {
        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(null, ObjectType.ENTITY);
        }).getMessage();

        assertEquals("acl is required.", message);
    }

    @Test
    public void testValidateAclWithoutId() {
        AccessControlList acl = new AccessControlList();

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.ENTITY);
        }).getMessage();

        assertEquals("acl.id is required.", message);
    }


    @Test
    public void testValidateAclWithoutEtag() {
        AccessControlList acl = new AccessControlList().setId("1");

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.ENTITY);
        }).getMessage();

        assertEquals("acl.etag is required.", message);
    }

    @Test
    public void testValidateAclWithoutCreationDate() {
        AccessControlList acl = new AccessControlList().setId("1").setEtag("test");

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.ENTITY);
        }).getMessage();

        assertEquals("acl.creationDate is required.", message);
    }

    @Test
    public void testValidateAclWithout() {
        AccessControlList acl = new AccessControlList().setId("1").setEtag("test").setCreationDate(new Date());

        String message = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            AccessControlListUtils.validateACL(acl, ObjectType.ENTITY);
        }).getMessage();

        assertEquals("acl.resourceAccess is required.", message);
    }
}
