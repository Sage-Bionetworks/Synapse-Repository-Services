package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.Text;

public class DocuSignTemplateValidatorTest {

	// A template with no sender fields declares no document-level tabs.
	private static final List<Tabs> NO_DOCUMENT_TABS = List.of();

	@Test
	public void testValidateWithValidTemplate() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(2);

		// call under test
		assertDoesNotThrow(() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
	}

	@Test
	public void testValidateWithNoCollaborators() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);

		// call under test
		assertDoesNotThrow(() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
	}

	@Test
	public void testValidateWithNullRecipients() {
		EnvelopeTemplate template = new EnvelopeTemplate();
		template.setRecipients(null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
		assertTrue(ex.getMessage().contains("no signer roles"));
	}

	@Test
	public void testValidateWithMissingSigningOfficialRole() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		template.getRecipients().getSigners().removeIf(s -> "signing_official".equals(s.getRoleName()));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
		assertTrue(ex.getMessage().contains("signing_official"));
	}

	@Test
	public void testValidateWithMissingPrincipalInvestigatorRole() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		template.getRecipients().getSigners().removeIf(s -> "principal_investigator".equals(s.getRoleName()));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
		assertTrue(ex.getMessage().contains("principal_investigator"));
	}

	// The email may be an emailAddress, a text or a sender field, so it is only missing when the
	// template declares it under none of them.
	@Test
	public void testValidateWithSigningOfficialEmailDeclaredUnderNoAllowedType() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Signer so = TestTemplateHelper.findSigner(template, "signing_official");
		so.getTabs().setEmailAddressTabs(null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
		assertTrue(ex.getMessage().contains("signing_official_email"));
		assertTrue(ex.getMessage().contains("EMAIL_ADDRESS"));
	}

	@Test
	public void testValidateWithPrincipalInvestigatorUserNameDeclaredUnderNoAllowedType() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Signer pi = TestTemplateHelper.findSigner(template, "principal_investigator");
		pi.getTabs().setTextTabs(null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
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
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
		assertTrue(ex.getMessage().contains("collaborator_2"));
	}

	@Test
	public void testValidateWithCollaboratorIndexTooLarge() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Recipients recipients = template.getRecipients();
		recipients.getSigners().add(TestTemplateHelper.buildCollaboratorSigner(99));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
		assertTrue(ex.getMessage().contains("exceeds maximum"));
	}

	@Test
	public void testValidateWithCollaboratorMissingTab() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		Signer collab = TestTemplateHelper.findSigner(template, "collaborator_1");
		collab.getTabs().setSignHereTabs(null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
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
		assertDoesNotThrow(() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
	}

	@Test
	public void testValidateWithNameAsTextTab() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		TestTemplateHelper.useTabType(template, "signing_official", "signing_official_name", TabType.TEXT);

		// call under test
		EDucTemplateLayout layout = DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS);

		assertEquals(TabType.TEXT, layout.typeOf("signing_official", "signing_official_name"));
		assertFalse(layout.isSenderField("signing_official", "signing_official_name"));
	}

	@Test
	public void testValidateWithEmailAsTextTab() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		TestTemplateHelper.useTabType(template, "principal_investigator", "principal_investigator_email",
				TabType.TEXT);

		// call under test
		EDucTemplateLayout layout = DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS);

		assertEquals(TabType.TEXT, layout.typeOf("principal_investigator", "principal_investigator_email"));
	}

	@Test
	public void testValidateWithNameAsSenderField() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		Tabs documentTabs = TestTemplateHelper.emptyDocumentTabs();
		TestTemplateHelper.useSenderField(template, "collaborator_1", "collaborator_1_name", documentTabs);

		// call under test
		EDucTemplateLayout layout = DocuSignTemplateValidator.validate(template, List.of(documentTabs));

		assertEquals(TabType.PREFILL_TEXT, layout.typeOf("collaborator_1", "collaborator_1_name"));
		assertTrue(layout.isSenderField("collaborator_1", "collaborator_1_name"));
		assertEquals(1, layout.senderFieldDefinitions().size());
		assertEquals("collaborator_1_name", layout.senderFieldDefinitions().get(0).getTabLabel());
	}

	@Test
	public void testValidateWithInstitutionAsSenderField() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Tabs documentTabs = TestTemplateHelper.emptyDocumentTabs();
		TestTemplateHelper.useSenderField(template, "signing_official", "signing_official_institution",
				documentTabs);

		// call under test
		EDucTemplateLayout layout = DocuSignTemplateValidator.validate(template, List.of(documentTabs));

		assertEquals(TabType.PREFILL_TEXT, layout.typeOf("signing_official", "signing_official_institution"));
	}

	@Test
	public void testValidateWithSenderFieldsSpreadOverSeveralDocuments() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Tabs firstDocument = TestTemplateHelper.emptyDocumentTabs();
		Tabs secondDocument = TestTemplateHelper.emptyDocumentTabs();
		TestTemplateHelper.useSenderField(template, "signing_official", "signing_official_institution",
				firstDocument);
		TestTemplateHelper.useSenderField(template, "principal_investigator", "principal_investigator_user_name",
				secondDocument);

		// call under test
		EDucTemplateLayout layout = DocuSignTemplateValidator.validate(template,
				List.of(firstDocument, secondDocument));

		assertEquals(TabType.PREFILL_TEXT, layout.typeOf("signing_official", "signing_official_institution"));
		assertEquals(TabType.PREFILL_TEXT,
				layout.typeOf("principal_investigator", "principal_investigator_user_name"));
		assertEquals(2, layout.senderFieldDefinitions().size());
	}

	// The institution is only ever text or a sender field, so a full name tab does not satisfy it.
	@Test
	public void testValidateWithTabDeclaredUnderDisallowedType() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		TestTemplateHelper.useTabType(template, "signing_official", "signing_official_institution",
				TabType.FULL_NAME);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
		assertTrue(ex.getMessage().contains("signing_official_institution"));
	}

	@Test
	public void testValidateWithTabDeclaredAsTwoRecipientTypes() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Signer so = TestTemplateHelper.findSigner(template, "signing_official");
		// Left declared as a full name as well, so there is no single place to write the value.
		TabType.TEXT.addTabWithLabel(so.getTabs(), "signing_official_name", null);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
		assertTrue(ex.getMessage().contains("signing_official_name"));
		assertTrue(ex.getMessage().contains("more than one type"));
	}

	@Test
	public void testValidateWithTabDeclaredAsBothRecipientTabAndSenderField() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Tabs documentTabs = TestTemplateHelper.emptyDocumentTabs();
		// The recipient's tab is deliberately left in place alongside the sender field.
		TestTemplateHelper.addSenderField(documentTabs, "signing_official_name");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, List.of(documentTabs)));
		assertTrue(ex.getMessage().contains("signing_official_name"));
		assertTrue(ex.getMessage().contains("more than one type"));
	}

	// A signature is supplied by the signer and a date by DocuSign, so neither can be a sender field.
	@Test
	public void testValidateWithSignatureAsSenderField() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Signer so = TestTemplateHelper.findSigner(template, "signing_official");
		so.getTabs().setSignHereTabs(null);
		Tabs documentTabs = TestTemplateHelper.emptyDocumentTabs();
		TestTemplateHelper.addSenderField(documentTabs, "signing_official_signature");

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.validate(template, List.of(documentTabs)));
		assertTrue(ex.getMessage().contains("signing_official_signature"));
	}

	@Test
	public void testValidateWithEmailInEmailAddressTabs() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		Signer so = TestTemplateHelper.findSigner(template, "signing_official");
		so.getTabs().setEmailTabs(null);
		com.docusign.esign.model.EmailAddress ea = new com.docusign.esign.model.EmailAddress();
		ea.setTabLabel("signing_official_email");
		so.getTabs().setEmailAddressTabs(new ArrayList<>(List.of(ea)));

		// call under test
		assertDoesNotThrow(() -> DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS));
	}

	@Test
	public void testValidateReportsTheTypeOfEveryRequiredTab() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);

		// call under test
		EDucTemplateLayout layout = DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS);

		assertEquals(TabType.TEXT, layout.typeOf("signing_official", "signing_official_institution"));
		assertEquals(TabType.FULL_NAME, layout.typeOf("signing_official", "signing_official_name"));
		assertEquals(TabType.EMAIL_ADDRESS, layout.typeOf("signing_official", "signing_official_email"));
		assertEquals(TabType.SIGN_HERE, layout.typeOf("signing_official", "signing_official_signature"));
		assertEquals(TabType.DATE_SIGNED, layout.typeOf("signing_official", "signing_official_date"));
		assertEquals(TabType.TEXT, layout.typeOf("collaborator_1", "collaborator_1_user_name"));
		assertEquals(TabType.FULL_NAME, layout.typeOf("collaborator_1", "collaborator_1_name"));
	}

	@Test
	public void testTypeOfWithLabelTheTemplateDoesNotCarry() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(0);
		EDucTemplateLayout layout = DocuSignTemplateValidator.validate(template, NO_DOCUMENT_TABS);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> layout.typeOf("collaborator_1", "collaborator_1_name"));
	}

	@Test
	public void testAllowedTypesForRoleAndLabelWithSigningOfficial() {
		// call under test
		assertEquals(Set.of(TabType.TEXT, TabType.PREFILL_TEXT), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("signing_official", "signing_official_institution"));
		assertEquals(Set.of(TabType.FULL_NAME, TabType.TEXT, TabType.PREFILL_TEXT), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("signing_official", "signing_official_name"));
		assertEquals(Set.of(TabType.EMAIL_ADDRESS, TabType.TEXT, TabType.PREFILL_TEXT), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("signing_official", "signing_official_email"));
		assertEquals(Set.of(TabType.SIGN_HERE), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("signing_official", "signing_official_signature"));
		assertEquals(Set.of(TabType.DATE_SIGNED), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("signing_official", "signing_official_date"));
	}

	@Test
	public void testAllowedTypesForRoleAndLabelWithPrincipalInvestigator() {
		// call under test
		assertEquals(Set.of(TabType.FULL_NAME, TabType.TEXT, TabType.PREFILL_TEXT), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("principal_investigator", "principal_investigator_name"));
		assertEquals(Set.of(TabType.EMAIL_ADDRESS, TabType.TEXT, TabType.PREFILL_TEXT), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("principal_investigator", "principal_investigator_email"));
		assertEquals(Set.of(TabType.TEXT, TabType.PREFILL_TEXT), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("principal_investigator", "principal_investigator_user_name"));
	}

	@Test
	public void testAllowedTypesForRoleAndLabelWithCollaborator() {
		// call under test
		assertEquals(Set.of(TabType.TEXT, TabType.PREFILL_TEXT), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("collaborator_1", "collaborator_1_user_name"));
		assertEquals(Set.of(TabType.FULL_NAME, TabType.TEXT, TabType.PREFILL_TEXT), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("collaborator_1", "collaborator_1_name"));
		assertEquals(Set.of(TabType.SIGN_HERE), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("collaborator_1", "collaborator_1_signature"));
		assertEquals(Set.of(TabType.DATE_SIGNED), DocuSignTemplateValidator
				.allowedTypesForRoleAndLabel("collaborator_1", "collaborator_1_date"));
	}

	@Test
	public void testAllowedTypesForRoleAndLabelWithUnknownRole() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.allowedTypesForRoleAndLabel("unknown_role", "some_label"));
	}

	@Test
	public void testAllowedTypesForRoleAndLabelWithUnknownLabel() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> DocuSignTemplateValidator.allowedTypesForRoleAndLabel("signing_official", "unknown_label"));
	}
}
