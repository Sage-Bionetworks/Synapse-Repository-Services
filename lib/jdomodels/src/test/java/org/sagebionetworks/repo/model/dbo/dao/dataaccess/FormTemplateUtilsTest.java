package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class FormTemplateUtilsTest {

	@Test
	public void testExtractAttachmentFileHandleIds() {
		String json = JDOSecondaryPropertyUtils.createJSONFromObject(newTemplate().setAttachments(List.of("987", "654")));

		// call under test
		Set<Long> fileHandleIds = FormTemplateUtils.extractAttachmentFileHandleIds(json);

		assertEquals(Set.of(987L, 654L), fileHandleIds);
	}

	@Test
	public void testExtractAttachmentFileHandleIdsWithNonNumericAttachment() {
		String json = JDOSecondaryPropertyUtils
				.createJSONFromObject(newTemplate().setAttachments(List.of("987", "syn123", "654")));

		// call under test
		Set<Long> fileHandleIds = FormTemplateUtils.extractAttachmentFileHandleIds(json);

		assertEquals(Set.of(987L, 654L), fileHandleIds);
	}

	@Test
	public void testExtractAttachmentFileHandleIdsWithNoAttachments() {
		String json = JDOSecondaryPropertyUtils.createJSONFromObject(newTemplate().setAttachments(null));

		// call under test
		Set<Long> fileHandleIds = FormTemplateUtils.extractAttachmentFileHandleIds(json);

		assertEquals(Set.of(), fileHandleIds);
	}

	@Test
	public void testExtractAttachmentFileHandleIdsWithEmptyAttachments() {
		String json = JDOSecondaryPropertyUtils.createJSONFromObject(newTemplate().setAttachments(List.of()));

		// call under test
		Set<Long> fileHandleIds = FormTemplateUtils.extractAttachmentFileHandleIds(json);

		assertEquals(Set.of(), fileHandleIds);
	}

	@Test
	public void testExtractAttachmentFileHandleIdsWithNullJson() {
		// call under test
		Set<Long> fileHandleIds = FormTemplateUtils.extractAttachmentFileHandleIds(null);

		assertEquals(Set.of(), fileHandleIds);
	}

	private static FormTemplate newTemplate() {
		// The steps are required for the template to serialize, but their content does not matter here.
		return new FormTemplate().setId("123").setVersionNumber(2L).setName("NF Standard DAR")
				.setSchema$id("org.sagebionetworks.test-DataAccessRequest-1.0.0").setSteps(List.of());
	}
}
