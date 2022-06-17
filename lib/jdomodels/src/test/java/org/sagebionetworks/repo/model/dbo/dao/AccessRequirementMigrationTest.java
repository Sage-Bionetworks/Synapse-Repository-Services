package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementMigration.AccessRequirementData;
import org.sagebionetworks.repo.model.helper.TermsOfUseAccessRequirementObjectHelper;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@TemporaryCode(author = "john.hill@sagebase.org", comment = "One time migration of AR names.  Can be removed after all ARs have a name.")

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AccessRequirementMigrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private TermsOfUseAccessRequirementObjectHelper helper;

	private AccessRequirementMigration migration;

	@BeforeEach
	public void before() {
		accessRequirementDAO.truncateAll();
		migration = new AccessRequirementMigration(jdbcTemplate);
	}
	
	@AfterEach
	public void after() {
		accessRequirementDAO.truncateAll();
	}

	@Test
	public void testGetWithMultipleVersions() {

		TermsOfUseAccessRequirement ar = helper.create((t) -> {
			t.setName(null);
			t.setDescription("v1");
		});
		ar.setVersionNumber(ar.getVersionNumber() + 1);
		ar.setName(null);
		ar.setDescription("v2");
		ar = accessRequirementDAO.update(ar);
		setNameToId(ar.getId());

		// call under test
		AccessRequirementData data = migration.getAccessRequirementData(ar.getId());

		Long expectedId = ar.getId();
		String expectedName = ar.getId().toString();
		String expectedDescription = "v2";
		AccessRequirementData expected = new AccessRequirementData(expectedId, expectedName, expectedDescription);
		assertEquals(expected, data);
	}
	
	@Test
	public void testMigrateWithNullName() {
		TermsOfUseAccessRequirement ar = helper.create((t) -> {
			t.setName(null);
			t.setDescription("v1");
		});
		ar.setVersionNumber(ar.getVersionNumber() + 1);
		ar.setName(null);
		ar.setDescription("v2");
		ar = accessRequirementDAO.update(ar);
		setNameToId(ar.getId());
		
		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		assertEquals(ar.getId().toString(), ar.getName());
		
		String oldEag = ar.getEtag();

		// call under test
		migration.migrate(ar.getId());

		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		assertNotEquals(oldEag, ar.getEtag());
		assertEquals("v2", ar.getName());
		
		String newEtag = ar.getEtag();
		
		// migrate again should be a no-op
		migration.migrate(ar.getId());
		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		// etag should not have changed the second time
		assertEquals(newEtag, ar.getEtag());
		assertEquals("v2", ar.getName());
	}
	
	@Test
	public void testMigrateWithName() {
		TermsOfUseAccessRequirement ar = helper.create((t) -> {
			t.setName("some name v1");
			t.setDescription(null);
		});
		ar.setVersionNumber(ar.getVersionNumber() + 1);
		ar.setName("some name v2");
		ar.setDescription(null);
		ar = accessRequirementDAO.update(ar);
		
		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		assertEquals("some name v2", ar.getName());
		
		String oldEag = ar.getEtag();

		// call under test
		migration.migrate(ar.getId());

		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		assertEquals(oldEag, ar.getEtag());
		assertEquals("some name v2", ar.getName());
	}
	
	@Test
	public void testMigrateWithNullDescription() {
		TermsOfUseAccessRequirement ar = helper.create((t) -> {
			t.setName(null);
			t.setDescription(null);
		});
		
		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		assertEquals(ar.getId().toString(), ar.getName());
		
		String oldEag = ar.getEtag();

		// call under test
		migration.migrate(ar.getId());

		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		assertEquals(oldEag, ar.getEtag());
		assertEquals(ar.getId().toString(), ar.getName());	
	}
	
	@Test
	public void testMigrateWithEmptyDescription() {
		TermsOfUseAccessRequirement ar = helper.create((t) -> {
			t.setName(null);
			t.setDescription("");
		});
		
		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		assertEquals(ar.getId().toString(), ar.getName());
		
		String oldEag = ar.getEtag();

		// call under test
		migration.migrate(ar.getId());

		ar = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar.getId().toString());
		assertEquals(oldEag, ar.getEtag());
		assertEquals(ar.getId().toString(), ar.getName());	
	}
	
	@Test
	public void testMigrateWithDuplicateDescription() {
		TermsOfUseAccessRequirement ar1 = helper.create((t) -> {
			t.setName(null);
			t.setDescription("duplicated");
		});
		TermsOfUseAccessRequirement ar2 = helper.create((t) -> {
			t.setName(null);
			t.setDescription(null);
		});
		// simulate a duplicate description on the second ar.
		setLatestDescription(ar2.getId(), "duplicated");
		
		ar2 = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar2.getId().toString());
		assertEquals(ar2.getId().toString(), ar2.getName());
		
		String oldEag = ar2.getEtag();

		// call under test
		migration.migrate(ar2.getId());

		ar2 = (TermsOfUseAccessRequirement) accessRequirementDAO.get(ar2.getId().toString());
		assertEquals(oldEag, ar2.getEtag());
		assertEquals(ar2.getId().toString(), ar2.getName());	
	}
	
	@Test
	public void testMigrateStreamOverIds() {
		TermsOfUseAccessRequirement ar1 = helper.create((t) -> {
			t.setName(null);
			t.setDescription(null);
		});
		TermsOfUseAccessRequirement ar2 = helper.create((t) -> {
			t.setName(null);
			t.setDescription(null);
		});
		
		List<Long> results = migration.streamOverIds().collect(Collectors.toList());
		assertEquals(Arrays.asList(ar1.getId(),ar2.getId()), results);
	}
	
	/**
	 * Set the state of the AR to match what it would be after the first migration with the name equal to the ID.
	 * 
	 * @param id
	 */
	public void setNameToId(Long id) {
		this.jdbcTemplate.update("UPDATE ACCESS_REQUIREMENT SET NAME = ? WHERE ID = ?", id.toString(), id);
	}
	
	/**
	 * Helper to change the description of the current revision of the passed AR.
	 * @param id
	 * @param description
	 */
	public void setLatestDescription(Long id, String description) {
		AccessRequirement ar = accessRequirementDAO.get(id.toString());
		ar.setDescription(description);
		byte[] newBlog = AccessRequirementUtils.writeSerializedField(ar);
		this.jdbcTemplate.update(
				"UPDATE ACCESS_REQUIREMENT_REVISION SET SERIALIZED_ENTITY = ? WHERE OWNER_ID = ? AND NUMBER = ?",
				newBlog, id, ar.getVersionNumber());
	}
}
