package org.sagebionetworks.repo.manager.util;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ar.AccessRequirementType;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserAccessRestrictionUtilsTest {

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWithNullPermissions(){
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(true)));
        String message = assertThrows(IllegalArgumentException.class, () -> {
            UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(null, usersRequirementStatus);
        }).getMessage();
        assertEquals("userEntityPermissionsState is required.", message);

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWithNullUsersRestrictionStatus(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(true)
                .withHasDelete(true);
        String message = assertThrows(IllegalArgumentException.class, () -> {
            UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(userEntityPermissions, null);
        }).getMessage();
        assertEquals("usersRestrictionStatus is required.", message);

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWhenUserIsNotContributorWithUnmetWithExemption(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(false)
                .withHasDelete(true);
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(true)));

        assertTrue(UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(userEntityPermissions,
                usersRequirementStatus));

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWhenUserIsNotContributorWithMetWithEligible(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(false)
                .withHasDelete(true);
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(true)));

        assertFalse(UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(userEntityPermissions,
                usersRequirementStatus));

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWhenUserIsNotContributorWithoutMetWithoutEligible(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(true)
                .withHasDelete(false);
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(false)));

        assertTrue(UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(userEntityPermissions,
                usersRequirementStatus));

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWithoutMetAndExemptionEligible(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(true)
                .withHasDelete(true);
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(true)));

        assertFalse(UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(userEntityPermissions,
                usersRequirementStatus));

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWithSomeMetAndSomeUnmet(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(true)
                .withHasDelete(false);
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(false),
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false)));

        assertTrue(UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(userEntityPermissions,
                usersRequirementStatus));

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWithSomeMetAndSomeExemptionEligible(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(true)
                .withHasDelete(true);
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(true),
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false)));

        assertFalse(UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForEntity(userEntityPermissions,
                usersRequirementStatus));

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForNonEntityWithNullUserRestrictionStatus(){
        String message = assertThrows(IllegalArgumentException.class, () -> {
            UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForNonEntity(null);
        }).getMessage();
        assertEquals("usersRestrictionStatus is required.", message);
    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForNonEntityWithHasUnmet(){
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false),
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(false)));
        assertTrue(UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForNonEntity(usersRequirementStatus));
    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForNonEntityWithHasMet(){
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false),
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false)));
        assertFalse(UserAccessRestrictionUtils.doesUserHaveUnmetAccessRestrictionsForNonEntity(usersRequirementStatus));
    }
}
