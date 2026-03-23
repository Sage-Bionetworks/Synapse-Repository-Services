package org.sagebionetworks.repo.model.dbo.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
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
import org.sagebionetworks.repo.model.table.search.TextAnalyzer;
import org.sagebionetworks.repo.model.table.search.TextAnalyzerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TextAnalyzerDaoImplAutowiredTest {

	@Autowired
	private TextAnalyzerDao textAnalyzerDao;

	@Autowired
	private OrganizationDao organizationDao;

	private Long adminUserId;
	private String organizationId;
	private String organizationName;

	@BeforeEach
	public void before() {
		adminUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		textAnalyzerDao.truncateAll();
		Organization org = organizationDao.createOrganization("test-org-" + UUID.randomUUID(), adminUserId);
		organizationId = org.getId();
		organizationName = org.getName();
	}

	@AfterEach
	public void after() {
		textAnalyzerDao.truncateAll();
		if (organizationId != null) {
			organizationDao.deleteOrganization(organizationId);
		}
	}

	@Test
	public void testCreateAndGet() {
		TextAnalyzer analyzer = newAnalyzer("test-create", "A test analyzer");

		TextAnalyzer created = textAnalyzerDao.create(analyzer, adminUserId);

		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertEquals("test-create", created.getName());
		assertEquals("A test analyzer", created.getDescription());
		assertNotNull(created.getCreatedOn());
		assertNotNull(created.getModifiedOn());
		assertEquals(adminUserId.toString(), created.getCreatedBy());
		assertEquals(adminUserId.toString(), created.getModifiedBy());
		assertEquals("standard", created.getSettings().getTokenizer());

		// Verify get returns the same data
		Optional<TextAnalyzer> fetched = textAnalyzerDao.get(Long.parseLong(created.getId()));
		assertTrue(fetched.isPresent());
		assertEquals(created.getId(), fetched.get().getId());
		assertEquals(created.getEtag(), fetched.get().getEtag());
	}

	@Test
	public void testGetNotFound() {
		Optional<TextAnalyzer> result = textAnalyzerDao.get(999999L);
		assertFalse(result.isPresent());
	}

	@Test
	public void testCreateDuplicateNameInSameOrgThrows() {
		textAnalyzerDao.create(newAnalyzer("duplicate-name", "First"), adminUserId);

		TextAnalyzer analyzer2 = newAnalyzer("duplicate-name", "Second");
		assertThrows(IllegalArgumentException.class, () -> textAnalyzerDao.create(analyzer2, adminUserId));
	}

	@Test
	public void testUpdatePersistsChangesAndRotatesEtag() {
		TextAnalyzer created = textAnalyzerDao.create(newAnalyzer("test-update", "original"), adminUserId);
		String originalEtag = created.getEtag();

		created.setName("test-update-renamed");
		created.setDescription("updated");
		TextAnalyzerSettings newSettings = new TextAnalyzerSettings();
		newSettings.setTokenizer("whitespace");
		created.setSettings(newSettings);

		TextAnalyzer updated = textAnalyzerDao.update(created, adminUserId);

		assertEquals("test-update-renamed", updated.getName());
		assertEquals("updated", updated.getDescription());
		assertEquals("whitespace", updated.getSettings().getTokenizer());
		assertNotEquals(originalEtag, updated.getEtag());
	}

	@Test
	public void testUpdateWithStaleEtagThrows() {
		TextAnalyzer created = textAnalyzerDao.create(newAnalyzer("test-occ", null), adminUserId);

		// First update succeeds and rotates the etag
		created.setDescription("first update");
		textAnalyzerDao.update(created, adminUserId);

		// Second update with the now-stale etag must fail
		created.setDescription("stale update");
		assertThrows(ConflictingUpdateException.class, () -> textAnalyzerDao.update(created, adminUserId));
	}

	@Test
	public void testDelete() {
		TextAnalyzer created = textAnalyzerDao.create(newAnalyzer("test-delete", null), adminUserId);
		Long id = Long.parseLong(created.getId());

		assertTrue(textAnalyzerDao.exists(id));
		textAnalyzerDao.delete(id);
		assertFalse(textAnalyzerDao.exists(id));
	}

	@Test
	public void testCreateOrUpdateSystemAnalyzerIsIdempotent() {
		// First call inserts
		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(1L, newAnalyzer("SCIENTIFIC", "V1"), organizationName, adminUserId);
		Optional<TextAnalyzer> first = textAnalyzerDao.get(1L);
		assertTrue(first.isPresent());
		assertEquals("V1", first.get().getDescription());
		String firstEtag = first.get().getEtag();

		// Second call with same ID updates in place
		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(1L, newAnalyzer("SCIENTIFIC", "V2"), organizationName, adminUserId);
		Optional<TextAnalyzer> second = textAnalyzerDao.get(1L);
		assertTrue(second.isPresent());
		assertEquals("V2", second.get().getDescription());
		assertNotEquals(firstEtag, second.get().getEtag());

		// Still only one row for this org
		assertEquals(1, textAnalyzerDao.listByOrganization(organizationName, 100, 0).size());
	}

	@Test
	public void testSettingsRoundTripThroughDatabase() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setSynonymAware(true);
		settings.setFilterOrder(Arrays.asList("lowercase", "english_stop", "english_stemmer"));
		settings.setTokenFilters("{\"english_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"},"
				+ "\"english_stemmer\":{\"type\":\"stemmer\",\"language\":\"english\"}}");

		TextAnalyzer analyzer = new TextAnalyzer();
		analyzer.setName("settings-roundtrip");
		analyzer.setOrganizationName(organizationName);
		analyzer.setSettings(settings);

		TextAnalyzer created = textAnalyzerDao.create(analyzer, adminUserId);
		TextAnalyzer fetched = textAnalyzerDao.get(Long.parseLong(created.getId())).get();

		TextAnalyzerSettings fetchedSettings = fetched.getSettings();
		assertEquals("standard", fetchedSettings.getTokenizer());
		assertTrue(fetchedSettings.getSynonymAware());
		assertEquals(Arrays.asList("lowercase", "english_stop", "english_stemmer"), fetchedSettings.getFilterOrder());
		assertNotNull(fetchedSettings.getTokenFilters());
		assertTrue(fetchedSettings.getTokenFilters().contains("english_stop"));
		assertTrue(fetchedSettings.getTokenFilters().contains("english_stemmer"));
	}

	private TextAnalyzer newAnalyzer(String name, String description) {
		TextAnalyzer analyzer = new TextAnalyzer();
		analyzer.setName(name);
		analyzer.setDescription(description);
		analyzer.setOrganizationName(organizationName);
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setFilterOrder(Arrays.asList("lowercase"));
		analyzer.setSettings(settings);
		return analyzer;
	}
}
