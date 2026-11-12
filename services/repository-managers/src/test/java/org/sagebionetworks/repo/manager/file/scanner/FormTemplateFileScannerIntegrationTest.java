package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.IdRange;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateField;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateStep;
import org.sagebionetworks.repo.model.dataaccess.schema.SubmissionContext;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateDao;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FormTemplateFileScannerIntegrationTest {

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private FormTemplateDao formTemplateDao;

	@Autowired
	private FileHandleAssociationScannerTestUtils utils;

	@Autowired
	private FileHandleAssociationManager manager;

	private FileHandleAssociateType associationType = FileHandleAssociateType.FormTemplateAttachment;

	private UserInfo user;

	@BeforeEach
	public void before() {
		formTemplateDao.truncateAll();
		fileHandleDao.truncateTable();

		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	@AfterEach
	public void after() {
		formTemplateDao.truncateAll();
		fileHandleDao.truncateTable();
	}

	@Test
	public void testScanner() {
		String fileOne = utils.generateFileHandle(user);
		String fileTwo = utils.generateFileHandle(user);
		String fileThree = utils.generateFileHandle(user);

		FormTemplate withAttachments = formTemplateDao.create(user.getId(),
				newTemplate("NF Standard DAR").setAttachments(List.of(fileOne, fileTwo)));
		// The second version drops the first file, which stays associated through the first version.
		formTemplateDao.createNewVersion(user.getId(), newTemplate("NF Standard DAR").setId(withAttachments.getId())
				.setVersionNumber(2L).setAttachments(List.of(fileTwo, fileThree)));
		FormTemplate withoutAttachments = formTemplateDao.create(user.getId(), newTemplate("AD Standard DAR"));

		Long withAttachmentsId = Long.valueOf(withAttachments.getId());

		// One association per version, in primary key order.
		List<ScannedFileHandleAssociation> expected = List.of(
				new ScannedFileHandleAssociation(withAttachmentsId)
						.withFileHandleIds(Set.of(Long.valueOf(fileOne), Long.valueOf(fileTwo))),
				new ScannedFileHandleAssociation(withAttachmentsId)
						.withFileHandleIds(Set.of(Long.valueOf(fileTwo), Long.valueOf(fileThree))),
				new ScannedFileHandleAssociation(Long.valueOf(withoutAttachments.getId()))
						.withFileHandleIds(Collections.emptySet()));

		IdRange range = manager.getIdRange(associationType);

		// call under test
		List<ScannedFileHandleAssociation> result = StreamSupport
				.stream(manager.scanRange(associationType, range).spliterator(), false)
				.collect(Collectors.toList());

		assertEquals(expected, result);
	}

	private static FormTemplate newTemplate(String name) {
		try {
			return new FormTemplate().setName(name)
					.setSchema$id("org.sagebionetworks.test-DataAccessRequest-1.0.0").setDeprecated(false)
					.setSteps(List.of(new FormTemplateStep().setTitle("Project").setDescription("Tell us about the project")
							.setFields(List.of(new FormTemplateField().setSchemaPath("/projectLead")
									.setSubmissionContext(SubmissionContext.ALWAYS).setIsPublic(true)
									.setUiDefinition(new JSONObjectAdapterImpl("{\"ui:widget\":\"textarea\"}"))))));
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
