package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Text;

public class DocuSignTemplateValidatorTest {

	@Test
	public void testValidateWithValidTemplate() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(2);

		// call under test
		assertDoesNotThrow(() -> DocuSignTemplateValidator.validate(template));
	}

	@Test
	public void testValidateWithNoCollaborators() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);

		// call under test
		assertDoesNotThrow(() -> DocuSignTemplateValidator.validate(template));
	}

	@Test
	public void testValidateWithNullRecipients() {
		EnvelopeTemplate template = new EnvelopeTemplate();
		template.setRecipients(null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template));
		assertTrue(ex.getMessage().contains("no signer roles"));
	}

	@Test
	public void testValidateWithMissingSigningOfficialRole() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		template.getRecipients().getSigners().removeIf(s -> "signing_official".equals(s.getRoleName()));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template));
		assertTrue(ex.getMessage().contains("signing_official"));
	}

	@Test
	public void testValidateWithMissingPrincipalInvestigatorRole() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		template.getRecipients().getSigners().removeIf(s -> "principal_investigator".equals(s.getRoleName()));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template));
		assertTrue(ex.getMessage().contains("principal_investigator"));
	}

	@Test
	public void testValidateWithMissingSigningOfficialTab() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Signer so = TestTemplateHelper.findSigner(template, "signing_official");
		so.getTabs().setTitleTabs(null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template));
		assertTrue(ex.getMessage().contains("signing_official_title"));
		assertTrue(ex.getMessage().contains("TITLE"));
	}

	@Test
	public void testValidateWithMissingPrincipalInvestigatorTab() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Signer pi = TestTemplateHelper.findSigner(template, "principal_investigator");
		pi.getTabs().setTextTabs(null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template));
		assertTrue(ex.getMessage().contains("principal_investigator_user_name"));
		assertTrue(ex.getMessage().contains("TEXT"));
	}

	@Test
	public void testValidateWithNonSequentialCollaborators() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Recipients recipients = template.getRecipients();
		recipients.getSigners().add(TestTemplateHelper.buildCollaboratorSigner(1));
		recipients.getSigners().add(TestTemplateHelper.buildCollaboratorSigner(3));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template));
		assertTrue(ex.getMessage().contains("collaborator_2"));
	}

	@Test
	public void testValidateWithCollaboratorIndexTooLarge() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Recipients recipients = template.getRecipients();
		recipients.getSigners().add(TestTemplateHelper.buildCollaboratorSigner(99));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template));
		assertTrue(ex.getMessage().contains("exceeds maximum"));
	}

	@Test
	public void testValidateWithCollaboratorMissingTab() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		Signer collab = TestTemplateHelper.findSigner(template, "collaborator_1");
		collab.getTabs().setSignHereTabs(null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template));
		assertTrue(ex.getMessage().contains("collaborator_1_signature"));
		assertTrue(ex.getMessage().contains("SIGN_HERE"));
	}

	@Test
	public void testValidateWithExtraTabsAllowed() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		Signer so = TestTemplateHelper.findSigner(template, "signing_official");
		Text extra = new Text();
		extra.setTabLabel("some_extra_tab");
		List<Text> newList = new ArrayList<Text>(so.getTabs().getTextTabs());
		newList.add(extra);
		so.getTabs().setTextTabs(newList);

		// call under test
		assertDoesNotThrow(() -> DocuSignTemplateValidator.validate(template));
	}

	@Test
	public void testValidateWithEmailInEmailAddressTabs() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Signer so = TestTemplateHelper.findSigner(template, "signing_official");
		so.getTabs().setEmailTabs(null);
		com.docusign.esign.model.EmailAddress ea = new com.docusign.esign.model.EmailAddress();
		ea.setTabLabel("signing_official_email");
		so.getTabs().setEmailAddressTabs(List.of(ea));

		// call under test
		assertDoesNotThrow(() -> DocuSignTemplateValidator.validate(template));
	}

	@Test
	public void testTypeForRoleAndLabelWithSigningOfficial() {
		assertEquals(TabType.TEXT, DocuSignTemplateValidator.typeforRoleAndLabel("signing_official", "signing_official_institution"));
		assertEquals(TabType.FULL_NAME, DocuSignTemplateValidator.typeforRoleAndLabel("signing_official", "signing_official_name"));
		assertEquals(TabType.TITLE, DocuSignTemplateValidator.typeforRoleAndLabel("signing_official", "signing_official_title"));
		assertEquals(TabType.EMAIL_ADDRESS, DocuSignTemplateValidator.typeforRoleAndLabel("signing_official", "signing_official_email"));
		assertEquals(TabType.SIGN_HERE, DocuSignTemplateValidator.typeforRoleAndLabel("signing_official", "signing_official_signature"));
		assertEquals(TabType.DATE_SIGNED, DocuSignTemplateValidator.typeforRoleAndLabel("signing_official", "signing_official_date"));
	}

	@Test
	public void testTypeForRoleAndLabelWithPrincipalInvestigator() {
		assertEquals(TabType.FULL_NAME, DocuSignTemplateValidator.typeforRoleAndLabel("principal_investigator", "principal_investigator_name"));
		assertEquals(TabType.TITLE, DocuSignTemplateValidator.typeforRoleAndLabel("principal_investigator", "principal_investigator_title"));
		assertEquals(TabType.EMAIL_ADDRESS, DocuSignTemplateValidator.typeforRoleAndLabel("principal_investigator", "principal_investigator_email"));
		assertEquals(TabType.TEXT, DocuSignTemplateValidator.typeforRoleAndLabel("principal_investigator", "principal_investigator_user_name"));
		assertEquals(TabType.SIGN_HERE, DocuSignTemplateValidator.typeforRoleAndLabel("principal_investigator", "principal_investigator_signature"));
		assertEquals(TabType.DATE_SIGNED, DocuSignTemplateValidator.typeforRoleAndLabel("principal_investigator", "principal_investigator_date"));
	}

	@Test
	public void testTypeForRoleAndLabelWithCollaborator() {
		assertEquals(TabType.TEXT, DocuSignTemplateValidator.typeforRoleAndLabel("collaborator_1", "collaborator_1_user_name"));
		assertEquals(TabType.FULL_NAME, DocuSignTemplateValidator.typeforRoleAndLabel("collaborator_1", "collaborator_1_name"));
		assertEquals(TabType.SIGN_HERE, DocuSignTemplateValidator.typeforRoleAndLabel("collaborator_1", "collaborator_1_signature"));
		assertEquals(TabType.DATE_SIGNED, DocuSignTemplateValidator.typeforRoleAndLabel("collaborator_1", "collaborator_1_date"));
	}

	@Test
	public void testTypeForRoleAndLabelWithUnknownRole() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.typeforRoleAndLabel("unknown_role", "some_label"));
	}

	@Test
	public void testTypeForRoleAndLabelWithUnknownLabel() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.typeforRoleAndLabel("signing_official", "unknown_label"));
	}
}
