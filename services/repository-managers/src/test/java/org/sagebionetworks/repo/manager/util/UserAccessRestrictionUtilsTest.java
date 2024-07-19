package org.sagebionetworks.repo.manager.util;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ar.AccessRequirementType;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserAccessRestrictionUtilsTest {

	@Test
	public void testIsUserDataContributor() {
		UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L)
				.withHasUpdate(true)
                .withHasDelete(true);
		
		// Call under test
		assertTrue(UserAccessRestrictionUtils.isUserDataContributor(userEntityPermissions));
	}
	
	@Test
	public void testIsUserDataContributorWithNoUpdate() {
		UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L)
				.withHasUpdate(false)
                .withHasDelete(true);
		
		// Call under test
		assertFalse(UserAccessRestrictionUtils.isUserDataContributor(userEntityPermissions));
	}
	
	@Test
	public void testIsUserExempt() {
		UsersRequirementStatus reqStatus = new UsersRequirementStatus().withIsExemptionEligible(true);
        
		boolean isUserDataContributor = true;
		
		assertTrue(UserAccessRestrictionUtils.isUserExempt(reqStatus, isUserDataContributor));
		
	}
	
	@Test
	public void testIsUserExemptWithNoExemptionEligible() {
		UsersRequirementStatus reqStatus = new UsersRequirementStatus().withIsExemptionEligible(false);
        
		boolean isUserDataContributor = true;
		
		assertFalse(UserAccessRestrictionUtils.isUserExempt(reqStatus, isUserDataContributor));
		
	}
	
	@Test
	public void testIsUserExemptWithNotDataContributor() {
		UsersRequirementStatus reqStatus = new UsersRequirementStatus().withIsExemptionEligible(true);
        
		boolean isUserDataContributor = false;
		
		assertFalse(UserAccessRestrictionUtils.isUserExempt(reqStatus, isUserDataContributor));
		
	}
	
	@Test
	public void testIsUserDataContributorWithNoDelete() {
		UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L)
				.withHasUpdate(true)
                .withHasDelete(false);
		
		// Call under test
		assertFalse(UserAccessRestrictionUtils.isUserDataContributor(userEntityPermissions));
	}
	
    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWithNullPermissions(){
        UsersRestrictionStatus usersRequirementStatus = new UsersRestrictionStatus().withSubjectId(1l)
                .withUserId(1l)
                .withRestrictionStatus(List.of(
                        new UsersRequirementStatus().withRequirementId(1L)
                                .withRequirementType(AccessRequirementType.MANAGED_ATC)
                                .withIsUnmet(true).withIsExemptionEligible(true)));
        String message = assertThrows(IllegalArgumentException.class, () -> {
            UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(null, usersRequirementStatus);
        }).getMessage();
        assertEquals("userEntityPermissionsState is required.", message);

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForEntityWithNullUsersRestrictionStatus(){
        UserEntityPermissionsState userEntityPermissions = new UserEntityPermissionsState(1L).withHasUpdate(true)
                .withHasDelete(true);
        String message = assertThrows(IllegalArgumentException.class, () -> {
            UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(userEntityPermissions, null);
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
        assertEquals(arIds, UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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
        assertEquals(Collections.emptyList(), UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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
        assertEquals(arIds, UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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

        assertEquals(Collections.emptyList(), UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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
        assertEquals(arIds, UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(userEntityPermissions,
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

        assertEquals(Collections.emptyList(), UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForEntity(userEntityPermissions,
                usersRequirementStatus));

    }

    @Test
    public void testDoesUserHaveUnmetAccessRestrictionsForNonEntityWithNullUserRestrictionStatus(){
        String message = assertThrows(IllegalArgumentException.class, () -> {
            UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForNonEntity(null);
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
        assertEquals(arIds, UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForNonEntity(usersRequirementStatus));
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
        assertEquals(Collections.emptyList(), UserAccessRestrictionUtils.getUsersUnmetAccessRestrictionsForNonEntity(usersRequirementStatus));
    }
}
