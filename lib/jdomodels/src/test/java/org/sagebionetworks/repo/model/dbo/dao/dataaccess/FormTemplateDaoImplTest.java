package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateField;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateStep;
import org.sagebionetworks.repo.model.dataaccess.schema.SubmissionContext;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:jdomodels-test-context.xml")
public class FormTemplateDaoImplTest {

	@Autowired
	private FormTemplateDao formTemplateDao;

	@Autowired
	private UserGroupDAO userGroupDao;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private TransactionTemplate transactionTemplate;
	private Long userId;

	@BeforeEach
	public void before() {
		formTemplateDao.truncateAll();

		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user.setCreationDate(new Date());
		user.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		userId = userGroupDao.create(user);

		transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@AfterEach
	public void after() {
		formTemplateDao.truncateAll();
		if (userId != null) {
			userGroupDao.delete(userId.toString());
		}
	}

	@Test
	public void testCreate() {
		FormTemplate template = newTemplate("NF Standard DAR");

		// call under test
		FormTemplate created = formTemplateDao.create(userId, template);

		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertEquals(FormTemplateDaoImpl.FIRST_VERSION_NUMBER, created.getVersionNumber().longValue());
		assertTemplateEquals(created, formTemplateDao.getLatestVersion(Long.parseLong(created.getId())).get());
	}

	@Test
	public void testCreateWithDuplicateName() {
		formTemplateDao.create(userId, newTemplate("NF Standard DAR"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formTemplateDao.create(userId, newTemplate("NF Standard DAR"));
		}).getMessage();

		assertEquals("A form template with the name 'NF Standard DAR' already exists.", message);
	}

	@Test
	public void testCreateWithNameThatDiffersByCase() {
		formTemplateDao.create(userId, newTemplate("NF Standard DAR"));

		// The name is compared without regard to case, so this is the same name.
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formTemplateDao.create(userId, newTemplate("nf standard dar"));
		});
	}

	@Test
	public void testCreateNewVersion() {
		FormTemplate first = formTemplateDao.create(userId, newTemplate("NF Standard DAR"));
		FormTemplate secondBody = newTemplate("NF Standard DAR v2").setId(first.getId())
				.setSchema$id("org.sagebionetworks.test-DataAccessRequest-2.0.0").setVersionNumber(2L);

		// call under test
		FormTemplate second = formTemplateDao.createNewVersion(userId, secondBody);

		assertEquals(2L, second.getVersionNumber().longValue());
		assertNotEquals(first.getEtag(), second.getEtag());
		assertTemplateEquals(second, formTemplateDao.getLatestVersion(Long.parseLong(first.getId())).get());

		// The first version is unchanged, other than reporting the current etag.
		FormTemplate firstFetched = formTemplateDao.getVersion(Long.parseLong(first.getId()), 1L).get();
		assertEquals("NF Standard DAR", firstFetched.getName());
		assertEquals("org.sagebionetworks.test-DataAccessRequest-1.0.0", firstFetched.getSchema$id());
		assertEquals(second.getEtag(), firstFetched.getEtag());
	}

	@Test
	public void testCreateNewVersionWithNameOfAnotherTemplate() {
		FormTemplate first = formTemplateDao.create(userId, newTemplate("NF Standard DAR"));
		formTemplateDao.create(userId, newTemplate("AD Standard DAR"));

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formTemplateDao.createNewVersion(userId,
					newTemplate("AD Standard DAR").setId(first.getId()).setVersionNumber(2L));
		});
	}

	@Test
	public void testGetLatestVersionWithNonExistentId() {
		// call under test
		assertEquals(Optional.empty(), formTemplateDao.getLatestVersion(-1L));
	}

	@Test
	public void testGetVersionWithNonExistentVersion() {
		FormTemplate created = formTemplateDao.create(userId, newTemplate("NF Standard DAR"));

		// call under test
		assertEquals(Optional.empty(), formTemplateDao.getVersion(Long.parseLong(created.getId()), 2L));
	}

	@Test
	public void testGetForUpdate() {
		FormTemplate created = formTemplateDao.create(userId, newTemplate("NF Standard DAR"));
		Long id = Long.parseLong(created.getId());

		// call under test
		FormTemplateInfoForUpdate info = transactionTemplate
				.execute(status -> formTemplateDao.getForUpdate(id).get());

		assertEquals(new FormTemplateInfoForUpdate(id, created.getEtag(), 1L), info);
	}

	@Test
	public void testGetForUpdateWithoutTransaction() {
		FormTemplate created = formTemplateDao.create(userId, newTemplate("NF Standard DAR"));

		assertThrows(IllegalTransactionStateException.class, () -> {
			// call under test
			formTemplateDao.getForUpdate(Long.parseLong(created.getId()));
		});
	}

	@Test
	public void testSearchLatestVersions() {
		FormTemplate nfOne = formTemplateDao.create(userId, newTemplate("NF Standard DAR"));
		FormTemplate nfTwo = formTemplateDao.create(userId, newTemplate("NF Renewal DAR"));
		FormTemplate adOne = formTemplateDao.create(userId, newTemplate("AD Standard DAR"));
		FormTemplate adTwo = formTemplateDao.create(userId, newTemplate("AD Renewal DAR"));

		// call under test
		List<FormTemplate> all = formTemplateDao.searchLatestVersions(null, false, 10L, 0L);

		assertEquals(List.of(nfOne.getId(), nfTwo.getId(), adOne.getId(), adTwo.getId()),
				all.stream().map(FormTemplate::getId).toList());

		// call under test
		List<FormTemplate> nfOnly = formTemplateDao.searchLatestVersions("nf ", false, 10L, 0L);

		assertEquals(List.of(nfOne.getId(), nfTwo.getId()), nfOnly.stream().map(FormTemplate::getId).toList());

		// call under test
		List<FormTemplate> secondPage = formTemplateDao.searchLatestVersions(null, false, 2L, 2L);

		assertEquals(List.of(adOne.getId(), adTwo.getId()), secondPage.stream().map(FormTemplate::getId).toList());

		// call under test
		List<FormTemplate> noMatch = formTemplateDao.searchLatestVersions("XY Standard DAR", false, 10L, 0L);

		assertEquals(List.of(), noMatch);
	}

	@Test
	public void testSearchLatestVersionsWithDeprecated() {
		FormTemplate active = formTemplateDao.create(userId, newTemplate("NF Standard DAR"));
		FormTemplate retired = formTemplateDao.create(userId, newTemplate("NF Retired DAR"));
		// A template is retired by publishing a version that is deprecated.
		formTemplateDao.createNewVersion(userId,
				newTemplate("NF Retired DAR").setId(retired.getId()).setVersionNumber(2L).setDeprecated(true));

		// call under test
		List<FormTemplate> activeOnly = formTemplateDao.searchLatestVersions(null, false, 10L, 0L);

		assertEquals(List.of(active.getId()), activeOnly.stream().map(FormTemplate::getId).toList());

		// call under test
		List<FormTemplate> all = formTemplateDao.searchLatestVersions(null, true, 10L, 0L);

		assertEquals(List.of(active.getId(), retired.getId()), all.stream().map(FormTemplate::getId).toList());
	}

	@Test
	public void testSearchLatestVersionsWithWildcardInName() {
		FormTemplate percent = formTemplateDao.create(userId, newTemplate("100% DAR"));
		formTemplateDao.create(userId, newTemplate("NF Standard DAR"));

		// call under test
		List<FormTemplate> results = formTemplateDao.searchLatestVersions("100%", false, 10L, 0L);

		assertEquals(List.of(percent.getId()), results.stream().map(FormTemplate::getId).toList());
	}

	/**
	 * The uiDefinition of a field is an arbitrary JSON object that compares by identity, so the
	 * templates are compared as JSON documents. MySQL normalizes a JSON column by sorting the keys of
	 * every object, so the comparison must ignore key order.
	 */
	private static void assertTemplateEquals(FormTemplate expected, FormTemplate actual) {
		JSONObject expectedJson = JDOSecondaryPropertyUtils.createJSONObjectForEntity(expected);
		JSONObject actualJson = JDOSecondaryPropertyUtils.createJSONObjectForEntity(actual);

		assertTrue(expectedJson.similar(actualJson), () -> "expected: " + expectedJson + " but was: " + actualJson);
	}

	private static FormTemplate newTemplate(String name) {
		return new FormTemplate().setName(name)
				.setSchema$id("org.sagebionetworks.test-DataAccessRequest-1.0.0").setDeprecated(false)
				.setSteps(List.of(
						new FormTemplateStep().setTitle("Project").setDescription("Tell us about the project")
								.setFields(List.of(new FormTemplateField().setSchemaPath("/projectLead")
										.setSubmissionContext(SubmissionContext.ALWAYS).setIsPublic(true)
										.setUiDefinition(uiDefinition("{\"ui:widget\":\"textarea\",\"ui:autofocus\":true,\"ui:options\":{\"rows\":5}}")))),
						new FormTemplateStep().setTitle("Approvals")
								.setFields(List.of(new FormTemplateField().setSchemaPath("/irbApproval")
										.setSubmissionContext(SubmissionContext.REQUEST_ONLY).setIsPublic(false)
										.setTemplateFileHandleId("987")
										.setUiDefinition(uiDefinition("{\"ui:widget\":\"file\"}"))))));
	}

	private static JSONObjectAdapter uiDefinition(String json) {
		try {
			return new JSONObjectAdapterImpl(json);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
