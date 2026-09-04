package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.SearchConfigurationDao;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class OrganizationDaoImplTest {

	private static final String CHILD_FK_MESSAGE = "All resources defined under an organization must be deleted before the organization can be deleted.";

	@Autowired
	private OrganizationDao organizationDao;
	@Autowired
	private JsonSchemaDao schemaDao;
	@Autowired
	private SynonymSetDao synonymSetDao;
	@Autowired
	private TextAnalyzerDao textAnalyzerDao;
	@Autowired
	private ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao;
	@Autowired
	private SearchConfigurationDao searchConfigurationDao;
	Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	String name;

	@BeforeEach
	public void before() {
		name = "Foo.Bar";
		truncateAll();
	}

	@AfterEach
	public void afterEach() {
		truncateAll();
	}

	/**
	 * Every child table must be cleared before ORGANIZATION, since the organization delete-all
	 * is itself subject to the ON DELETE RESTRICT constraints under test here.
	 */
	private void truncateAll() {
		searchConfigurationDao.truncateAll();
		columnAnalyzerOverrideDao.truncateAll();
		synonymSetDao.truncateAll();
		textAnalyzerDao.truncateAll();
		schemaDao.truncateAll();
		organizationDao.truncateAll();
	}

	@Test
	public void testCreateGetDelete() {
		// Call under test
		Organization created = organizationDao.createOrganization(name, adminUserId);
		assertNotNull(created);
		assertEquals(name, created.getName());
		assertNotNull(created.getId());
		assertNotNull(created.getCreatedOn());
		assertEquals(""+adminUserId, created.getCreatedBy());

		// call under test
		Organization fetched = organizationDao.getOrganizationByName(name);
		assertEquals(created, fetched);
		// call under test
		organizationDao.deleteOrganization(fetched.getId());
	}

	@Test
	public void testGetOrganizationById() {
		Organization created = organizationDao.createOrganization(name, adminUserId);
		assertNotNull(created);

		// call under test
		Optional<Organization> fetched = organizationDao.getOrganizationById(created.getId());
		assertTrue(fetched.isPresent());
		assertEquals(created, fetched.get());
		// call under test
		organizationDao.deleteOrganization(fetched.get().getId());
	}

	@Test
	public void testGetOrganizationByNonExistingId() {
		// call under test
		Optional<Organization> fetched = organizationDao.getOrganizationById("-99");
		assertTrue(fetched.isEmpty());

	}
	
	@Test
	public void testGetOrganizationDifferentCase() {
		Organization created = organizationDao.createOrganization(name.toLowerCase(), adminUserId);
		// call under test lookup
		Organization fetched = organizationDao.getOrganizationByName(name.toUpperCase());
		assertEquals(created, fetched);
	}

	@Test
	public void testCreateOrganizationDuplicateName() {
		// Call under test
		Organization created = organizationDao.createOrganization(name, adminUserId);
		assertNotNull(created);
		// Attempt to create a duplicate name.
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			organizationDao.createOrganization(name, adminUserId);
		}).getMessage();
		assertEquals("An Organization with the name: 'Foo.Bar' already exists", message);
	}
	
	@Test
	public void testCreateOrganizationDuplicateNameByCase() {
		// Call under test
		Organization created = organizationDao.createOrganization(name.toLowerCase(), adminUserId);
		assertNotNull(created);
		// Attempt to create a duplicate name.
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			organizationDao.createOrganization(name.toUpperCase(), adminUserId);
		}).getMessage();
		assertEquals("An Organization with the name: 'FOO.BAR' already exists", message);
	}

	@Test
	public void testGetOrganizationByNameNotFound() {
		String name = "Foo.Bar";
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			organizationDao.getOrganizationByName(name);
		}).getMessage();
		assertEquals("Organization with name: 'Foo.Bar' not found", message);
	}

	@Test
	public void testDeleteOrganizationNotFound() {
		String id = "-123";
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			organizationDao.deleteOrganization(id);
		}).getMessage();
		assertEquals("Organization with id: '-123' not found", message);
	}
	
	@Test
	public void testListOrganizations() {
		organizationDao.createOrganization("d", adminUserId);
		organizationDao.createOrganization("c", adminUserId);
		organizationDao.createOrganization("a", adminUserId);
		organizationDao.createOrganization("b", adminUserId);
		long limit = 2;
		long offset = 1;
		// Call under test
		List<Organization> page = organizationDao.listOrganizations(limit, offset);
		assertNotNull(page);
		assertEquals(2, page.size());
		assertEquals("b", page.get(0).getName());
		assertEquals("c", page.get(1).getName());
	}
	
	/**
	 * See PLFM-6400.
	 */
	@Test
	public void testDeleteOrganizationWithSchema() {
		Organization org = organizationDao.createOrganization("b", adminUserId);
		JsonSchema simpleSchema = new JsonSchema();
		String schemaName = "simple";
		simpleSchema.set$id(org.getName() + "-" + schemaName);
		simpleSchema.setDescription("Super simple schema");
		NewSchemaVersionRequest newRequest = new NewSchemaVersionRequest().withOrganizationId(org.getId())
				.withJsonSchema(simpleSchema).withSchemaName(schemaName).withCreatedBy(adminUserId);
		schemaDao.createNewSchemaVersion(newRequest);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			//call under test
			organizationDao.deleteOrganization(org.getId());
		}).getMessage();
		assertEquals(CHILD_FK_MESSAGE, message);
	}

	@Test
	public void testDeleteOrganizationWithSynonymSet() {
		Organization org = organizationDao.createOrganization("b", adminUserId);
		synonymSetDao.create(adminUserId, new SynonymSet()
				.setName("cancer_terms")
				.setDescription("Cancer synonyms")
				.setOrganizationName(org.getName())
				.setDefinition(new JSONObject()
						.put("type", "synonym_graph")
						.put("synonyms", new JSONArray().put("cancer, tumor, neoplasm"))));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			organizationDao.deleteOrganization(org.getId());
		}).getMessage();
		assertEquals(CHILD_FK_MESSAGE, message);
	}

	@Test
	public void testDeleteOrganizationWithTextAnalyzer() {
		Organization org = organizationDao.createOrganization("b", adminUserId);
		textAnalyzerDao.create(new TextAnalyzer()
				.setName("lowercase_analyzer")
				.setDescription("Lower cases everything")
				.setOrganizationName(org.getName())
				.setSettings(new JSONObject()
						.put("analyzer", new JSONObject()
								.put("default", new JSONObject()
										.put("type", "custom")
										.put("tokenizer", "standard")
										.put("filter", new JSONArray().put("lowercase"))))),
				adminUserId);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			organizationDao.deleteOrganization(org.getId());
		}).getMessage();
		assertEquals(CHILD_FK_MESSAGE, message);
	}

	@Test
	public void testDeleteOrganizationWithColumnAnalyzerOverride() {
		Organization org = organizationDao.createOrganization("b", adminUserId);
		columnAnalyzerOverrideDao.create(adminUserId, new ColumnAnalyzerOverride()
				.setName("diagnosis_override")
				.setDescription("Overrides the diagnosis column")
				.setOrganizationName(org.getName())
				.setOverrides(List.of(new ColumnAnalyzerOverrideEntry()
						.setColumnName("diagnosis")
						.setAnalyzer(new JSONObject().put("$ref", "org.sagebionetworks-SCIENTIFIC")))));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			organizationDao.deleteOrganization(org.getId());
		}).getMessage();
		assertEquals(CHILD_FK_MESSAGE, message);
	}

	@Test
	public void testDeleteOrganizationWithSearchConfiguration() {
		Organization org = organizationDao.createOrganization("b", adminUserId);
		// The default analyzer is inline rather than a $ref, so this organization owns exactly
		// one child row and only SC_ORG_FK can fire.
		searchConfigurationDao.create(adminUserId, new SearchConfiguration()
				.setName("default_config")
				.setDescription("Default search configuration")
				.setOrganizationName(org.getName())
				.setDefaultAnalyzer(new JSONObject()
						.put("analyzer", new JSONObject()
								.put("default", new JSONObject()
										.put("type", "custom")
										.put("tokenizer", "standard")
										.put("filter", new JSONArray().put("lowercase"))))));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			organizationDao.deleteOrganization(org.getId());
		}).getMessage();
		assertEquals(CHILD_FK_MESSAGE, message);
	}
}
