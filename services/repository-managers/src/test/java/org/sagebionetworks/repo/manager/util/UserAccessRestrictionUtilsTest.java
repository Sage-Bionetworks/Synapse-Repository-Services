package org.sagebionetworks.repo.manager.util;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ar.AccessRequirementType;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(null, usersRequirementStatus);
        }).getMessage();
        assertEquals("userEntityPermissionsState is required.", message);

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWithNullUsersRestrictionStatus(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(true)
                .withHasDelete(true);
        String message = assertThrows(IllegalArgumentException.class, () -> {
            UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(userEntityPermissions, null);
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
        List<Long> arIds = List.of(1L);

        //call under test
        assertEquals(arIds, UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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

        //call under test
        assertEquals(Collections.emptyList(), UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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
        List<Long> arIds = List.of(1L);
        //call under test
        assertEquals(arIds, UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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

        assertEquals(Collections.emptyList(), UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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
                        new UsersRequirementStatus().withRequirementId(2L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false)));
        List<Long> arIds = List.of(1L);
        assertEquals(arIds, UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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
                        new UsersRequirementStatus().withRequirementId(2L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false)));

        assertEquals(Collections.emptyList(), UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForEntity(userEntityPermissions,
                usersRequirementStatus));

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForNonEntityWithNullUserRestrictionStatus(){
        String message = assertThrows(IllegalArgumentException.class, () -> {
            UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForNonEntity(null);
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
                        new UsersRequirementStatus().withRequirementId(2L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(false)));
        List<Long> arIds = List.of(2L);
        assertEquals(arIds, UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForNonEntity(usersRequirementStatus));
    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForNonEntityWithHasMet(){
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false),
                        new UsersRequirementStatus().withRequirementId(2L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(false).withIsExemptionEligible(false)));
        assertEquals(Collections.emptyList(), UserAccessRestrictionUtils.usersUnmetAccessRestrictionsForNonEntity(usersRequirementStatus));
    }
}
