package org.sagebionetworks.repo.model.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;

@ExtendWith(MockitoExtension.class)
public class DboAutoDiscoveryTest {

	@Mock
	private MigratableDatabaseObject<?, ?> mockPrimaryRealm;

	@Mock
	private MigratableDatabaseObject<?, ?> mockSecondaryRealmIdentityProvider;

	@Mock
	private MigratableDatabaseObject<?, ?> mockPrimaryUserGroup;

	@Mock
	private MigratableDatabaseObject<?, ?> mockPrimaryChange;

	@Mock
	private DatabaseObject<?> mockNonMigratable;

	@Test
	public void testDiscoverPrimaryMigratableDatabaseObjectsWithFiltersSecondaryTypes() {
		// Setup: primary with secondary
		when(mockPrimaryRealm.getMigratableTableType()).thenReturn(MigrationType.REALM);
		when(mockPrimaryRealm.getSecondaryTypes()).thenReturn(List.of(mockSecondaryRealmIdentityProvider));

		// Setup: secondary
		when(mockSecondaryRealmIdentityProvider.getMigratableTableType()).thenReturn(MigrationType.REALM_IDP);
		when(mockSecondaryRealmIdentityProvider.getSecondaryTypes()).thenReturn(null);

		// Setup: other primaries
		when(mockPrimaryUserGroup.getMigratableTableType()).thenReturn(MigrationType.PRINCIPAL);
		when(mockPrimaryUserGroup.getSecondaryTypes()).thenReturn(null);

		when(mockPrimaryChange.getMigratableTableType()).thenReturn(MigrationType.CHANGE);
		when(mockPrimaryChange.getSecondaryTypes()).thenReturn(null);

		List<DatabaseObject> allDbos = List.of(
			mockPrimaryRealm,
			mockSecondaryRealmIdentityProvider,
			mockPrimaryUserGroup,
			mockPrimaryChange,
			mockNonMigratable  // Non-migratable - should be ignored
		);

		// Call under test
		List<MigratableDatabaseObject> result = DboAutoDiscovery.discoverPrimaryMigratableDatabaseObjects(allDbos);

		// Verify: only 3 primary DBOs returned (secondary and non-migratable filtered out)
		assertEquals(3, result.size(), "Should return only primary migratable DBOs");

		// Verify: no secondary types in result
		assertTrue(result.stream().noneMatch(dbo -> dbo.getMigratableTableType() == MigrationType.REALM_IDP),
			"Secondary type should be filtered out");
	}

	@Test
	public void testDiscoverPrimaryMigratableDatabaseObjectsWithSortsByMigrationType() {
		// Setup: stubs in WRONG order (CHANGE, REALM, PRINCIPAL)
		when(mockPrimaryChange.getMigratableTableType()).thenReturn(MigrationType.CHANGE);
		when(mockPrimaryChange.getSecondaryTypes()).thenReturn(null);

		when(mockPrimaryRealm.getMigratableTableType()).thenReturn(MigrationType.REALM);
		when(mockPrimaryRealm.getSecondaryTypes()).thenReturn(List.of());

		when(mockPrimaryUserGroup.getMigratableTableType()).thenReturn(MigrationType.PRINCIPAL);
		when(mockPrimaryUserGroup.getSecondaryTypes()).thenReturn(null);

		List<DatabaseObject> allDbos = List.of(
			mockPrimaryChange,      // CHANGE comes LAST in enum
			mockPrimaryRealm,       // REALM comes FIRST in enum
			mockPrimaryUserGroup    // PRINCIPAL comes in middle
		);

		// Call under test
		List<MigratableDatabaseObject> result = DboAutoDiscovery.discoverPrimaryMigratableDatabaseObjects(allDbos);

		// Verify: sorted by MigrationType enum order (REALM, PRINCIPAL, CHANGE)
		assertEquals(3, result.size());
		assertEquals(MigrationType.REALM, result.get(0).getMigratableTableType(),
			"REALM should be first (comes first in enum)");
		assertEquals(MigrationType.PRINCIPAL, result.get(1).getMigratableTableType(),
			"PRINCIPAL should be second");
		assertEquals(MigrationType.CHANGE, result.get(2).getMigratableTableType(),
			"CHANGE should be last (comes last in enum)");
	}

	@Test
	public void testDiscoverPrimaryMigratableDatabaseObjectsWithEmptyList() {
		// Call under test
		List<MigratableDatabaseObject> result = DboAutoDiscovery.discoverPrimaryMigratableDatabaseObjects(List.of());

		// Verify: empty result for empty input
		assertTrue(result.isEmpty(), "Should return empty list for empty input");
	}

	@Test
	public void testDiscoverPrimaryMigratableDatabaseObjectsWithOnlyNonMigratableTypes() {
		// Setup: list with only non-migratable DBOs
		List<DatabaseObject> allDbos = List.of(mockNonMigratable, mockNonMigratable);

		// Call under test
		List<MigratableDatabaseObject> result = DboAutoDiscovery.discoverPrimaryMigratableDatabaseObjects(allDbos);

		// Verify: empty result when no migratable types
		assertTrue(result.isEmpty(), "Should return empty list when no migratable types present");
	}

	@Test
	public void testDiscoverPrimaryMigratableDatabaseObjectsWithAllSecondaryTypes() {
		// Setup: list where the migratable DBO is secondary
		when(mockPrimaryRealm.getMigratableTableType()).thenReturn(MigrationType.REALM);
		when(mockPrimaryRealm.getSecondaryTypes()).thenReturn(List.of(mockSecondaryRealmIdentityProvider));

		when(mockSecondaryRealmIdentityProvider.getMigratableTableType()).thenReturn(MigrationType.REALM_IDP);
		when(mockSecondaryRealmIdentityProvider.getSecondaryTypes()).thenReturn(null);

		List<DatabaseObject> allDbos = List.of(
			mockPrimaryRealm,
			mockSecondaryRealmIdentityProvider
		);

		// Call under test
		List<MigratableDatabaseObject> result = DboAutoDiscovery.discoverPrimaryMigratableDatabaseObjects(allDbos);

		// Verify: only primary returned
		assertEquals(1, result.size());
		assertEquals(MigrationType.REALM, result.get(0).getMigratableTableType());
	}

	@Test
	public void testDiscoverPrimaryMigratableDatabaseObjectsWithNullSecondaryTypes() {
		// Setup: DBO with null getSecondaryTypes() should be treated as primary
		when(mockPrimaryChange.getMigratableTableType()).thenReturn(MigrationType.CHANGE);
		when(mockPrimaryChange.getSecondaryTypes()).thenReturn(null);

		List<DatabaseObject> allDbos = List.of(mockPrimaryChange);

		// Call under test
		List<MigratableDatabaseObject> result = DboAutoDiscovery.discoverPrimaryMigratableDatabaseObjects(allDbos);

		// Verify: DBO with null secondaryTypes is treated as primary
		assertEquals(1, result.size());
		assertEquals(MigrationType.CHANGE, result.get(0).getMigratableTableType());
	}
}
