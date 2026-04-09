package org.sagebionetworks.repo.model.dbo.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.SynonymRule;
import org.sagebionetworks.repo.model.search.table.SynonymRuleType;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SynonymSetDaoImplAutowiredTest {

	@Autowired
	private SynonymSetDao synonymSetDao;

	@Autowired
	private OrganizationDao organizationDao;

	private Long adminUserId;
	private String org1Id;
	private String org1Name;
	private String org2Id;
	private String org2Name;

	@BeforeEach
	public void before() {
		adminUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		synonymSetDao.truncateAll();

		Organization org1 = organizationDao.createOrganization("test-org-" + UUID.randomUUID(), adminUserId);
		org1Id = org1.getId();
		org1Name = org1.getName();

		Organization org2 = organizationDao.createOrganization("test-org-" + UUID.randomUUID(), adminUserId);
		org2Id = org2.getId();
		org2Name = org2.getName();
	}

	@AfterEach
	public void after() {
		synonymSetDao.truncateAll();
		if (org1Id != null) {
			organizationDao.deleteOrganization(org1Id);
		}
		if (org2Id != null) {
			organizationDao.deleteOrganization(org2Id);
		}
	}

	@Test
	public void testCreateAndGetWithRules() {
		SynonymRule rule = new SynonymRule();
		rule.setRuleType(SynonymRuleType.EQUIVALENT);
		rule.setTerms(Arrays.asList("cancer", "tumor", "neoplasm"));

		SynonymSet toCreate = newSynonymSet(org1Name, "test-create", "A test set");
		toCreate.setRules(Arrays.asList(rule));

		// call under test
		SynonymSet created = synonymSetDao.create(adminUserId, toCreate);

		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertEquals("test-create", created.getName());
		assertEquals("A test set", created.getDescription());
		assertEquals(org1Name, created.getOrganizationName());
		assertNotNull(created.getCreatedOn());
		assertNotNull(created.getModifiedOn());
		assertEquals(adminUserId.toString(), created.getCreatedBy());
		assertEquals(adminUserId.toString(), created.getModifiedBy());
		assertEquals(Arrays.asList(rule), created.getRules());

		// call under test
		Optional<SynonymSet> fetched = synonymSetDao.get(created.getId());

		assertTrue(fetched.isPresent());
		assertEquals(created, fetched.get());
	}

	@Test
	public void testGetWithNonExistentId() {
		// call under test
		Optional<SynonymSet> result = synonymSetDao.get("999999");

		assertFalse(result.isPresent());
	}

	@Test
	public void testCreateWithDuplicateNameInSameOrg() {
		synonymSetDao.create(adminUserId, newSynonymSet(org1Name, "duplicate-name", "First"));

		SynonymSet second = newSynonymSet(org1Name, "duplicate-name", "Second");

		// call under test
		assertThrows(IllegalArgumentException.class, () -> synonymSetDao.create(adminUserId, second));
	}

	@Test
	public void testUpdateWithModifiedRulesAndDescription() {
		SynonymRule originalRule = new SynonymRule();
		originalRule.setRuleType(SynonymRuleType.EQUIVALENT);
		originalRule.setTerms(Arrays.asList("heart attack", "myocardial infarction"));

		SynonymSet toCreate = newSynonymSet(org1Name, "test-update", "original");
		toCreate.setRules(Arrays.asList(originalRule));

		SynonymSet created = synonymSetDao.create(adminUserId, toCreate);
		String originalEtag = created.getEtag();

		SynonymRule updatedRule = new SynonymRule();
		updatedRule.setRuleType(SynonymRuleType.EXPLICIT);
		updatedRule.setTerms(Arrays.asList("AD", "Alzheimer's disease"));

		created.setName("test-update-renamed");
		created.setDescription("updated");
		created.setRules(Arrays.asList(updatedRule));

		// call under test
		SynonymSet updated = synonymSetDao.update(adminUserId, created);

		assertEquals("test-update-renamed", updated.getName());
		assertEquals("updated", updated.getDescription());
		assertNotEquals(originalEtag, updated.getEtag());
		assertEquals(Arrays.asList(updatedRule), updated.getRules());
	}

	@Test
	public void testUpdateWithStaleEtagThrows() {
		SynonymSet created = synonymSetDao.create(adminUserId, newSynonymSet(org1Name, "test-occ", null));

		// First update succeeds and rotates the etag
		created.setDescription("first update");
		synonymSetDao.update(adminUserId, created);

		// Second update with the now-stale etag must fail
		created.setDescription("stale update");

		// call under test
		assertThrows(ConflictingUpdateException.class, () -> synonymSetDao.update(adminUserId, created));
	}

	@Test
	public void testDeleteWithExistingSet() {
		SynonymSet created = synonymSetDao.create(adminUserId, newSynonymSet(org1Name, "test-delete", null));
		assertTrue(synonymSetDao.get(created.getId()).isPresent());

		// call under test
		synonymSetDao.delete(created.getId());

		assertFalse(synonymSetDao.get(created.getId()).isPresent());
	}

	@Test
	public void testGetByOrganizationAndNameWithMatchingEntry() {
		SynonymRule rule = new SynonymRule();
		rule.setRuleType(SynonymRuleType.EQUIVALENT);
		rule.setTerms(Arrays.asList("cancer", "tumor"));

		SynonymSet toCreate = newSynonymSet(org1Name, "find-me", "target");
		toCreate.setRules(Arrays.asList(rule));
		SynonymSet created = synonymSetDao.create(adminUserId, toCreate);

		// Create another in a different org to ensure filtering
		synonymSetDao.create(adminUserId, newSynonymSet(org2Name, "find-me", "decoy"));

		// call under test
		Optional<SynonymSet> found = synonymSetDao.getByOrganizationAndName(org1Name, "find-me");

		assertTrue(found.isPresent());
		assertEquals(created, found.get());
	}

	@Test
	public void testGetByOrganizationAndNameWithNonExistentName() {
		// call under test
		Optional<SynonymSet> result = synonymSetDao.getByOrganizationAndName(org1Name, "does-not-exist");

		assertFalse(result.isPresent());
	}

	@Test
	public void testCreateAndGetWithMultipleRuleTypes() {
		SynonymRule equivalentRule = new SynonymRule();
		equivalentRule.setRuleType(SynonymRuleType.EQUIVALENT);
		equivalentRule.setTerms(Arrays.asList("heart attack", "myocardial infarction", "MI"));

		SynonymRule explicitRule = new SynonymRule();
		explicitRule.setRuleType(SynonymRuleType.EXPLICIT);
		explicitRule.setTerms(Arrays.asList("AD", "Alzheimer's disease"));

		SynonymSet set = newSynonymSet(org1Name, "rules-roundtrip", "Rules test");
		set.setRules(Arrays.asList(equivalentRule, explicitRule));

		SynonymSet created = synonymSetDao.create(adminUserId, set);

		// call under test
		SynonymSet fetched = synonymSetDao.get(created.getId()).get();

		assertEquals(Arrays.asList(equivalentRule, explicitRule), fetched.getRules());
		assertEquals(created, fetched);
	}

	@Test
	public void testListWithMultipleOrganizations() {
		// Create 2 synonym sets in org1
		SynonymSet org1SetA = synonymSetDao.create(adminUserId, newSynonymSet(org1Name, "org1-set-a", "first"));
		SynonymSet org1SetB = synonymSetDao.create(adminUserId, newSynonymSet(org1Name, "org1-set-b", "second"));

		// Create 2 synonym sets in org2
		SynonymSet org2SetA = synonymSetDao.create(adminUserId, newSynonymSet(org2Name, "org2-set-a", "third"));
		SynonymSet org2SetB = synonymSetDao.create(adminUserId, newSynonymSet(org2Name, "org2-set-b", "fourth"));

		// call under test — list by org1
		List<SynonymSet> org1Results = synonymSetDao.list(org1Name, 10, 0);

		assertEquals(2, org1Results.size());
		assertEquals(org1SetA.getId(), org1Results.get(0).getId());
		assertEquals(org1SetB.getId(), org1Results.get(1).getId());

		// call under test — list by org2
		List<SynonymSet> org2Results = synonymSetDao.list(org2Name, 10, 0);

		assertEquals(2, org2Results.size());
		assertEquals(org2SetA.getId(), org2Results.get(0).getId());
		assertEquals(org2SetB.getId(), org2Results.get(1).getId());

		// call under test — list all
		List<SynonymSet> allResults = synonymSetDao.listAll(10, 0);

		assertEquals(4, allResults.size());
		// Ordered by ID, so org1 sets come first (created first)
		assertEquals(org1SetA.getId(), allResults.get(0).getId());
		assertEquals(org1SetB.getId(), allResults.get(1).getId());
		assertEquals(org2SetA.getId(), allResults.get(2).getId());
		assertEquals(org2SetB.getId(), allResults.get(3).getId());
	}

	@Test
	public void testUniquenessConstraintWithMaxLengthNames() {
		// ORGANIZATION_NAME is varchar(250) ascii, NAME is varchar(256).
		// Create an org with a 250-char name to test max-length unique key behavior.
		char[] orgChars = new char[250];
		java.util.Arrays.fill(orgChars, 'o');
		String maxOrgName = new String(orgChars);
		Organization maxOrg = organizationDao.createOrganization(maxOrgName, adminUserId);
		String maxOrgId = maxOrg.getId();

		try {
			// Create two sets whose 256-char names differ only in the last character
			char[] nameChars = new char[256];
			java.util.Arrays.fill(nameChars, 'a');
			String nameA = new String(nameChars);
			nameChars[255] = 'b';
			String nameB = new String(nameChars);

			SynonymRule ruleA = new SynonymRule();
			ruleA.setRuleType(SynonymRuleType.EQUIVALENT);
			ruleA.setTerms(Arrays.asList("cancer", "tumor"));

			SynonymRule ruleB = new SynonymRule();
			ruleB.setRuleType(SynonymRuleType.EXPLICIT);
			ruleB.setTerms(Arrays.asList("heart", "cardiac"));

			SynonymSet setA = newSynonymSet(maxOrgName, nameA, "desc-a");
			setA.setRules(Arrays.asList(ruleA));
			SynonymSet setB = newSynonymSet(maxOrgName, nameB, "desc-b");
			setB.setRules(Arrays.asList(ruleB));

			// Both should succeed — they are distinct names
			SynonymSet createdA = synonymSetDao.create(adminUserId, setA);
			SynonymSet createdB = synonymSetDao.create(adminUserId, setB);

			// Verify each set retained its own data (not silently overwritten by index truncation)
			SynonymSet fetchedA = synonymSetDao.get(createdA.getId()).get();
			assertEquals(nameA, fetchedA.getName());
			assertEquals("desc-a", fetchedA.getDescription());
			assertEquals(maxOrgName, fetchedA.getOrganizationName());
			assertEquals(Arrays.asList(ruleA), fetchedA.getRules());

			SynonymSet fetchedB = synonymSetDao.get(createdB.getId()).get();
			assertEquals(nameB, fetchedB.getName());
			assertEquals("desc-b", fetchedB.getDescription());
			assertEquals(maxOrgName, fetchedB.getOrganizationName());
			assertEquals(Arrays.asList(ruleB), fetchedB.getRules());

			// A duplicate of the first name should fail
			// call under test
			assertThrows(IllegalArgumentException.class,
					() -> synonymSetDao.create(adminUserId, newSynonymSet(maxOrgName, nameA, null)));
		} finally {
			synonymSetDao.truncateAll();
			organizationDao.deleteOrganization(maxOrgId);
		}
	}

	private SynonymSet newSynonymSet(String organizationName, String name, String description) {
		SynonymSet set = new SynonymSet();
		set.setName(name);
		set.setDescription(description);
		set.setOrganizationName(organizationName);
		return set;
	}
}
