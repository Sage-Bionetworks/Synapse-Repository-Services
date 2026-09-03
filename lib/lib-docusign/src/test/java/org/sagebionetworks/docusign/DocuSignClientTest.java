package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.educ.EDucTemplate;
import org.sagebionetworks.repo.model.educ.EDucTemplatePage;

import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;
import org.sagebionetworks.repo.model.educ.EDucSignerStatus;
import org.sagebionetworks.repo.model.educ.EDucSignerStatusEnum;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;

import com.docusign.esign.client.ApiException;

import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.EnvelopeTemplateResults;
import com.docusign.esign.model.FullName;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.TemplateInformation;
import com.docusign.esign.model.TemplateRole;
import com.docusign.esign.model.TemplateSummary;

@ExtendWith(MockitoExtension.class)
public class DocuSignClientTest {

	@Mock
	private DocuSignTemplatesApi mockDocuSignTemplatesApi;
	@Mock
	private DocuSignEnvelopesApi mockDocuSignEnvelopesApi;

	@InjectMocks
	private DocuSignClient client;

	@Test
	public void testListTemplatesSuccess() {
		String createdIso = "2024-01-15T10:00:00.0000000Z";
		String modifiedIso = "2024-02-20T15:30:00.0000000Z";
		EnvelopeTemplate t1 = new EnvelopeTemplate();
		t1.setTemplateId("tpl-1");
		t1.setName("Consent Form");
		t1.setDescription("Standard consent form");
		t1.setCreated(createdIso);
		t1.setLastModified(modifiedIso);
		EnvelopeTemplate t2 = new EnvelopeTemplate();
		t2.setTemplateId("tpl-2");
		t2.setName("Data Sharing Agreement");
		t2.setDescription("Data sharing agreement");
		EnvelopeTemplateResults results = new EnvelopeTemplateResults();
		results.setEnvelopeTemplates(Arrays.asList(t1, t2));
		when(mockDocuSignTemplatesApi.listTemplates("0", "51")).thenReturn(results);

		// call under test
		EDucTemplatePage page = client.listTemplates(0, 51);

		assertNotNull(page);
		assertNull(page.getNextPageToken());
		assertEquals(2, page.getResults().size());

		EDucTemplate mapped1 = new EDucTemplate();
		mapped1.setTemplateId("tpl-1");
		mapped1.setName("Consent Form");
		mapped1.setDescription("Standard consent form");
		mapped1.setCreatedOn(Date.from(Instant.parse(createdIso)));
		mapped1.setModifiedOn(Date.from(Instant.parse(modifiedIso)));
		assertEquals(mapped1, page.getResults().get(0));

		EDucTemplate mapped2 = new EDucTemplate();
		mapped2.setTemplateId("tpl-2");
		mapped2.setName("Data Sharing Agreement");
		mapped2.setDescription("Data sharing agreement");
		assertEquals(mapped2, page.getResults().get(1));

		verify(mockDocuSignTemplatesApi).listTemplates("0", "51");
	}

	@Test
	public void testListTemplatesWithEmptyResults() {
		EnvelopeTemplateResults results = new EnvelopeTemplateResults();
		results.setEnvelopeTemplates(null);
		when(mockDocuSignTemplatesApi.listTemplates(any(), any())).thenReturn(results);

		// call under test
		EDucTemplatePage page = client.listTemplates(0, 51);

		assertNotNull(page);
		assertEquals(Collections.emptyList(), page.getResults());
	}

	@Test
	public void testValidateTemplateSuccess() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		when(mockDocuSignTemplatesApi.getTemplate("tpl-1")).thenReturn(template);

		// call under test
		client.validateTemplate("tpl-1");

		verify(mockDocuSignTemplatesApi).getTemplate("tpl-1");
	}

	@Test
	public void testValidateTemplateWithInvalidTemplate() {
		EnvelopeTemplate template = new EnvelopeTemplate();
		template.setRecipients(null);
		when(mockDocuSignTemplatesApi.getTemplate("tpl-1")).thenReturn(template);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> client.validateTemplate("tpl-1"));
	}

	@Test
	public void testCreateAndSendEnvelopeSuccess() {
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(1);
		when(mockDocuSignTemplatesApi.getTemplate("tpl-1")).thenReturn(template);
		EnvelopeSummary summary = new EnvelopeSummary();
		summary.setEnvelopeId("env-123");
		when(mockDocuSignEnvelopesApi.createEnvelope(any())).thenReturn(summary);

		Map<String, RecipientInfo> recipients = Map.of(
				"signing_official", new RecipientInfo("so@example.com", "Dr. Smith"),
				"principal_investigator", new RecipientInfo("pi@example.com", "Dr. Jones")
		);
		Map<RoleLabelKey, String> tabValues = Map.of(
				new RoleLabelKey("signing_official", "signing_official_name"), "Dr. Smith",
				new RoleLabelKey("principal_investigator", "principal_investigator_name"), "Dr. Jones"
		);

		// call under test
		String envelopeId = client.createEnvelope("tpl-1", recipients, tabValues);

		assertEquals("env-123", envelopeId);
		ArgumentCaptor<EnvelopeDefinition> captor = ArgumentCaptor.forClass(EnvelopeDefinition.class);
		verify(mockDocuSignEnvelopesApi).createEnvelope(captor.capture());
		EnvelopeDefinition captured = captor.getValue();
		assertEquals("tpl-1", captured.getTemplateId());
		assertEquals("created", captured.getStatus());
		assertEquals(2, captured.getTemplateRoles().size());
	}

	@Test
	public void testCreateAndSendEnvelopeValidationFailure() {
		EnvelopeTemplate template = new EnvelopeTemplate();
		template.setRecipients(null);
		when(mockDocuSignTemplatesApi.getTemplate("tpl-1")).thenReturn(template);

		Map<String, RecipientInfo> recipients = Map.of(
				"signing_official", new RecipientInfo("so@example.com", "Dr. Smith"));
		Map<RoleLabelKey, String> tabValues = Map.of();

		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> client.createEnvelope("tpl-1", recipients, tabValues));

		verifyNoInteractions(mockDocuSignEnvelopesApi);
	}

	@Test
	public void testBuildTemplateRolesWithCorrectTabTypes() {
		Map<String, RecipientInfo> recipients = Map.of(
				"signing_official", new RecipientInfo("so@example.com", "Dr. Smith"),
				"principal_investigator", new RecipientInfo("pi@example.com", "Dr. Jones")
		);
		Map<RoleLabelKey, String> tabValues = Map.of(
				new RoleLabelKey("signing_official", "signing_official_name"), "Dr. Smith",
				new RoleLabelKey("signing_official", "signing_official_email"), "so@example.com",
				new RoleLabelKey("signing_official", "signing_official_institution"), "MIT"
		);

		// call under test
		List<TemplateRole> roles = DocuSignClient.buildTemplateRoles(recipients, tabValues);

		assertEquals(2, roles.size());
		TemplateRole soRole = roles.stream()
				.filter(r -> "signing_official".equals(r.getRoleName())).findFirst().orElseThrow();
		assertEquals("so@example.com", soRole.getEmail());
		assertEquals("Dr. Smith", soRole.getName());
		assertEquals(1, soRole.getTabs().getFullNameTabs().size());
		assertEquals("signing_official_name", soRole.getTabs().getFullNameTabs().get(0).getTabLabel());
		// an EMAIL_ADDRESS value goes to emailAddressTabs, the tab type a template declares and so
		// the one DocuSign matches the label against
		assertEquals(1, soRole.getTabs().getEmailAddressTabs().size());
		assertEquals("so@example.com", soRole.getTabs().getEmailAddressTabs().get(0).getValue());
		assertEquals("MIT", soRole.getTabs().getTextTabs().get(0).getValue());

		// The name comes from the recipient info (not the tab values or the email).
		TemplateRole piRole = roles.stream()
				.filter(r -> "principal_investigator".equals(r.getRoleName())).findFirst().orElseThrow();
		assertEquals("Dr. Jones", piRole.getName());
	}

	@Test
	public void testBuildTemplateRolesSetsNameFromRecipientInfo() {
		Map<String, RecipientInfo> recipients = Map.of(
				"signing_official", new RecipientInfo("so@example.com", "Sally Signer"),
				"principal_investigator", new RecipientInfo("pi@example.com", "Paul Investigator"),
				"collaborator_1", new RecipientInfo("c1@example.com", "Carl Collaborator")
		);
		Map<RoleLabelKey, String> tabValues = Map.of();

		// call under test
		List<TemplateRole> roles = DocuSignClient.buildTemplateRoles(recipients, tabValues);

		for (TemplateRole role : roles) {
			assertEquals(recipients.get(role.getRoleName()).name(), role.getName());
			assertEquals(recipients.get(role.getRoleName()).email(), role.getEmail());
		}
	}

	@Test
	public void testBuildTemplateRolesWithMissingName() {
		Map<String, RecipientInfo> recipients = Map.of(
				"signing_official", new RecipientInfo("so@example.com", null));
		Map<RoleLabelKey, String> tabValues = Map.of();

		// call under test — a role with no name is rejected
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignClient.buildTemplateRoles(recipients, tabValues));
		assertTrue(ex.getMessage().contains("name for role 'signing_official'"));
	}

	@Test
	public void testBuildTemplateRolesWithMissingEmail() {
		Map<String, RecipientInfo> recipients = Map.of(
				"signing_official", new RecipientInfo(null, "Dr. Smith"));
		Map<RoleLabelKey, String> tabValues = Map.of();

		// call under test — a role with no email is rejected
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignClient.buildTemplateRoles(recipients, tabValues));
		assertTrue(ex.getMessage().contains("email for role 'signing_official'"));
	}

	@Test
	public void testHandleApiExceptionMapping() {
		assertEquals(DocuSignUnauthorizedException.class,
				DocuSignApiRetryHelper.convertApiException(new ApiException(401, "x")).getClass());
		assertEquals(IllegalStateException.class,
				DocuSignApiRetryHelper.convertApiException(new ApiException(403, "x")).getClass());
		assertEquals(IllegalStateException.class,
				DocuSignApiRetryHelper.convertApiException(new ApiException(404, "x")).getClass());
		assertEquals(IllegalStateException.class,
				DocuSignApiRetryHelper.convertApiException(new ApiException(500, "x")).getClass());
		assertEquals(IllegalStateException.class,
				DocuSignApiRetryHelper.convertApiException(new ApiException(429, "x")).getClass());
	}

	@Test
	public void testConvertApiExceptionSurfacesResponseBody() {
		String responseBody = "{\"errorCode\":\"RECIPIENTS_NOT_PROVIDED\","
				+ "\"message\":\"A recipient is missing a required field.\"}";
		ApiException apiException = new ApiException(400, "Bad Request", null, responseBody);

		RuntimeException result = DocuSignApiRetryHelper.convertApiException(apiException);

		assertEquals(IllegalStateException.class, result.getClass());
		assertTrue(result.getMessage().contains("RECIPIENTS_NOT_PROVIDED"),
				"Expected the DocuSign response body in the message but was: " + result.getMessage());
		assertTrue(result.getMessage().contains("DocuSign API error 400."));
	}

	@Test
	public void testConvertApiExceptionWithNoResponseBody() {
		ApiException apiException = new ApiException(500, "boom");

		RuntimeException result = DocuSignApiRetryHelper.convertApiException(apiException);

		assertEquals("DocuSign API error 500.", result.getMessage());
	}

	@Test
	public void testGetEnvelopeStatusSuccess() {
		Signer signer1 = new Signer();
		signer1.setName("Dr. Jones");
		signer1.setEmail("pi@university.edu");
		signer1.setStatus("completed");
		Signer signer2 = new Signer();
		signer2.setName("Jane Admin");
		signer2.setEmail("so@university.edu");
		signer2.setStatus("sent");
		Signer signer3 = new Signer();
		signer3.setName("Carl Collaborator");
		signer3.setEmail("collab@university.edu");
		signer3.setStatus("declined");
		signer3.setDeclinedReason("I do not agree to these terms.");
		signer3.setDeclinedDateTime("2026-07-03T09:15:00Z");

		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(signer1, signer2, signer3));

		Envelope envelope = new Envelope();
		envelope.setStatus("sent");
		envelope.setCreatedDateTime("2026-07-01T10:00:00Z");
		envelope.setLastModifiedDateTime("2026-07-02T15:30:00Z");
		envelope.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-123")).thenReturn(envelope);

		// call under test
		EnvelopeStatusResult result = client.getEnvelopeStatus("env-123");

		EDucSignatureStatus status = result.status();
		assertEquals(EDucStatusEnum.sent, status.getDucStatus());
		assertNotNull(status.getCreatedOn());
		assertNotNull(status.getModifiedOn());
		assertEquals(3, status.getSignerStatus().size());
		assertEquals("Dr. Jones", status.getSignerStatus().get(0).getName());
		assertEquals(EDucSignerStatusEnum.done, status.getSignerStatus().get(0).getStatus());
		// A signer who has not declined has no reason or declined date.
		assertNull(status.getSignerStatus().get(0).getDeclinedReason());
		assertNull(status.getSignerStatus().get(0).getDeclinedOn());
		assertEquals("Jane Admin", status.getSignerStatus().get(1).getName());
		assertEquals(EDucSignerStatusEnum.pending, status.getSignerStatus().get(1).getStatus());

		// The declined signer carries the reason and the declined date/time.
		EDucSignerStatus declined = status.getSignerStatus().get(2);
		assertEquals("Carl Collaborator", declined.getName());
		assertEquals(EDucSignerStatusEnum.declined, declined.getStatus());
		assertEquals("I do not agree to these terms.", declined.getDeclinedReason());
		assertEquals(Date.from(Instant.parse("2026-07-03T09:15:00Z")), declined.getDeclinedOn());

		assertEquals(List.of("pi@university.edu", "so@university.edu", "collab@university.edu"), result.signerEmails());
	}

	@Test
	public void testToEDucStatusEnum() {
		assertEquals(EDucStatusEnum.sent, DocuSignClient.toEDucStatusEnum("sent"));
		assertEquals(EDucStatusEnum.delivered, DocuSignClient.toEDucStatusEnum("delivered"));
		assertEquals(EDucStatusEnum.completed, DocuSignClient.toEDucStatusEnum("completed"));
		assertEquals(EDucStatusEnum.completed, DocuSignClient.toEDucStatusEnum("signed"));
		assertEquals(EDucStatusEnum.declined, DocuSignClient.toEDucStatusEnum("declined"));
		assertEquals(EDucStatusEnum.voided, DocuSignClient.toEDucStatusEnum("voided"));
		assertEquals(EDucStatusEnum.correct, DocuSignClient.toEDucStatusEnum("correct"));
		assertEquals(EDucStatusEnum.draft, DocuSignClient.toEDucStatusEnum("created"));
		assertNull(DocuSignClient.toEDucStatusEnum(null));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignClient.toEDucStatusEnum("bogus_status"));
		assertEquals("Unexpected status bogus_status", ex.getMessage());
	}

	@Test
	public void testToEDucSignerStatusEnum() {
		assertEquals(EDucSignerStatusEnum.pending, DocuSignClient.toEDucSignerStatusEnum("sent"));
		assertEquals(EDucSignerStatusEnum.pending, DocuSignClient.toEDucSignerStatusEnum("delivered"));
		assertEquals(EDucSignerStatusEnum.pending, DocuSignClient.toEDucSignerStatusEnum("created"));
		assertEquals(EDucSignerStatusEnum.pending, DocuSignClient.toEDucSignerStatusEnum("faxpending"));
		assertEquals(EDucSignerStatusEnum.pending, DocuSignClient.toEDucSignerStatusEnum(null));
		assertEquals(EDucSignerStatusEnum.done, DocuSignClient.toEDucSignerStatusEnum("completed"));
		assertEquals(EDucSignerStatusEnum.done, DocuSignClient.toEDucSignerStatusEnum("signed"));
		assertEquals(EDucSignerStatusEnum.declined, DocuSignClient.toEDucSignerStatusEnum("declined"));
		assertEquals(EDucSignerStatusEnum.bounced, DocuSignClient.toEDucSignerStatusEnum("autoresponded"));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> DocuSignClient.toEDucSignerStatusEnum("bogus"));
		assertEquals("Unexpected status bogus", ex.getMessage());
	}

	private static Signer signerWithEmail(String recipientId, String roleName, String email) {
		Signer signer = new Signer();
		signer.setRecipientId(recipientId);
		signer.setRoleName(roleName);
		signer.setEmail(email);
		return signer;
	}

	@Test
	public void testSendEnvelopeSuccess() {
		// all recipients are resolved (have an email), so none are removed
		Signer pi = signerWithEmail("1", "principal_investigator", "pi@example.com");
		Signer collab1 = signerWithEmail("2", "collaborator_1", "c1@example.com");
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(pi, collab1));
		Envelope existing = new Envelope();
		existing.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(existing);

		// call under test
		client.sendEnvelope("env-1");

		verify(mockDocuSignEnvelopesApi, never()).deleteRecipients(any(), any());
		ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
		verify(mockDocuSignEnvelopesApi).updateEnvelope(eq("env-1"), captor.capture());
		assertEquals("sent", captor.getValue().getStatus());
	}

	@Test
	public void testSendEnvelopeRemovesUnusedCollaborators() {
		// collaborator_2 was defined by the template but left unfilled (no email)
		Signer pi = signerWithEmail("1", "principal_investigator", "pi@example.com");
		Signer collab1 = signerWithEmail("2", "collaborator_1", "c1@example.com");
		Signer collab2 = signerWithEmail("3", "collaborator_2", null);
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(pi, collab1, collab2));
		Envelope existing = new Envelope();
		existing.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(existing);

		// call under test
		client.sendEnvelope("env-1");

		ArgumentCaptor<Recipients> deleteCaptor = ArgumentCaptor.forClass(Recipients.class);
		verify(mockDocuSignEnvelopesApi).deleteRecipients(eq("env-1"), deleteCaptor.capture());
		List<Signer> removed = deleteCaptor.getValue().getSigners();
		assertEquals(1, removed.size());
		assertEquals("3", removed.get(0).getRecipientId());
		assertEquals("collaborator_2", removed.get(0).getRoleName());

		ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
		verify(mockDocuSignEnvelopesApi).updateEnvelope(eq("env-1"), captor.capture());
		assertEquals("sent", captor.getValue().getStatus());
	}

	@Test
	public void testSendEnvelopeDoesNotRemoveUnresolvedRequiredRoles() {
		// a required role with no email must NOT be removed — the send should be left to fail
		Signer pi = signerWithEmail("1", "principal_investigator", null);
		Signer so = signerWithEmail("2", "signing_official", null);
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(pi, so));
		Envelope existing = new Envelope();
		existing.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(existing);

		// call under test
		client.sendEnvelope("env-1");

		verify(mockDocuSignEnvelopesApi, never()).deleteRecipients(any(), any());
	}

	@Test
	public void testSendEnvelopeWithNullEnvelopeId() {
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.sendEnvelope(null));

		assertEquals("envelopeId is required.", ex.getMessage());
	}

	@Test
	public void testVoidEnvelopeSuccess() {
		// call under test
		client.voidEnvelope("env-1", "Cancelled by user.");

		verify(mockDocuSignEnvelopesApi).voidEnvelope("env-1", "Cancelled by user.");
	}

	@Test
	public void testVoidEnvelopeWithNullEnvelopeId() {
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.voidEnvelope(null, "reason"));

		assertEquals("envelopeId is required.", ex.getMessage());
	}

	@Test
	public void testVoidEnvelopeWithNullReason() {
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.voidEnvelope("env-1", null));

		assertEquals("reason is required.", ex.getMessage());
	}

	@Test
	public void testGetDocumentSuccess() {
		when(mockDocuSignEnvelopesApi.getDocument("env-1", "combined")).thenReturn(new byte[]{1, 2, 3});

		// call under test
		byte[] result = client.getDocument("env-1");

		assertEquals(3, result.length);
		verify(mockDocuSignEnvelopesApi).getDocument("env-1", "combined");
	}

	@Test
	public void testGetDocumentWithNullEnvelopeId() {
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.getDocument(null));

		assertEquals("envelopeId is required.", ex.getMessage());
	}

	@Test
	public void testGetSignedDocumentSuccess() {
		Envelope envelope = new Envelope();
		envelope.setStatus("completed");
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(envelope);
		when(mockDocuSignEnvelopesApi.getDocument("env-1", "combined")).thenReturn(new byte[]{1, 2, 3});

		// call under test
		byte[] result = client.getSignedDocument("env-1");

		assertEquals(3, result.length);
		verify(mockDocuSignEnvelopesApi).getDocument("env-1", "combined");
	}

	@Test
	public void testGetSignedDocumentWithIncompleteEnvelope() {
		Envelope envelope = new Envelope();
		envelope.setStatus("sent");
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(envelope);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.getSignedDocument("env-1"));

		assertEquals("Cannot retrieve signed document: envelope status is sent.", ex.getMessage());
	}

	@Test
	public void testGetSignedDocumentWithNullEnvelopeId() {
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.getSignedDocument(null));

		assertEquals("envelopeId is required.", ex.getMessage());
	}

	@Test
	public void testListEnvelopeStatusesSuccess() {
		Envelope env1 = new Envelope();
		env1.setEnvelopeId("env-1");
		env1.setStatus("sent");
		Envelope env2 = new Envelope();
		env2.setEnvelopeId("env-2");
		env2.setStatus("completed");
		when(mockDocuSignEnvelopesApi.listStatus(List.of("env-1", "env-2")))
				.thenReturn(List.of(env1, env2));

		// call under test
		List<Envelope> result = client.listEnvelopeStatuses(List.of("env-1", "env-2"));

		assertEquals(2, result.size());
		assertEquals("env-1", result.get(0).getEnvelopeId());
		assertEquals("sent", result.get(0).getStatus());
		assertEquals("env-2", result.get(1).getEnvelopeId());
		assertEquals("completed", result.get(1).getStatus());
	}

	@Test
	public void testListEnvelopeStatusesWithEmptyList() {
		// call under test
		List<Envelope> result = client.listEnvelopeStatuses(List.of());

		assertEquals(0, result.size());
		verifyNoInteractions(mockDocuSignEnvelopesApi);
	}

	@Test
	public void testListEnvelopeStatusesWithNull() {
		// call under test
		List<Envelope> result = client.listEnvelopeStatuses(null);

		assertEquals(0, result.size());
		verifyNoInteractions(mockDocuSignEnvelopesApi);
	}

	private static Signer existingSigner(String recipientId, String roleName, String status) {
		Signer signer = new Signer();
		signer.setRecipientId(recipientId);
		signer.setRoleName(roleName);
		signer.setStatus(status);
		return signer;
	}

	@Test
	public void testCorrectEnvelopeSequence() {
		// PI already signed; signing official still pending
		Signer pi = existingSigner("1", "principal_investigator", "completed");
		Signer so = existingSigner("2", "signing_official", "sent");
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(pi, so));
		Envelope existing = new Envelope();
		existing.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(existing);

		Map<String, RecipientInfo> desiredRecipients = Map.of(
				"principal_investigator", new RecipientInfo("pi@example.com", "Dr. Jones"),
				"signing_official", new RecipientInfo("so-new@example.com", "Jane Admin")
		);
		Map<RoleLabelKey, String> tabValues = Map.of(
				new RoleLabelKey("signing_official", "signing_official_name"), "Dr. Smith"
		);

		// call under test
		client.correctEnvelope("env-1", desiredRecipients, tabValues);

		// no role has to be added back, so the template is not needed
		verifyNoInteractions(mockDocuSignTemplatesApi);

		InOrder order = inOrder(mockDocuSignEnvelopesApi);
		order.verify(mockDocuSignEnvelopesApi).getEnvelope("env-1");
		ArgumentCaptor<Envelope> correctCaptor = ArgumentCaptor.forClass(Envelope.class);
		order.verify(mockDocuSignEnvelopesApi).updateEnvelope(eq("env-1"), correctCaptor.capture());
		assertEquals("correct", correctCaptor.getValue().getStatus());

		// only the not-yet-signed signing_official is updated (PI is left untouched)
		ArgumentCaptor<Recipients> updateCaptor = ArgumentCaptor.forClass(Recipients.class);
		order.verify(mockDocuSignEnvelopesApi).updateRecipients(eq("env-1"), updateCaptor.capture(), eq(true));
		List<Signer> updatedSigners = updateCaptor.getValue().getSigners();
		assertEquals(1, updatedSigners.size());
		assertEquals("2", updatedSigners.get(0).getRecipientId());
		assertEquals("signing_official", updatedSigners.get(0).getRoleName());
		assertEquals("so-new@example.com", updatedSigners.get(0).getEmail());

		ArgumentCaptor<Envelope> sendCaptor = ArgumentCaptor.forClass(Envelope.class);
		order.verify(mockDocuSignEnvelopesApi).updateEnvelope(eq("env-1"), sendCaptor.capture());
		assertEquals("sent", sendCaptor.getValue().getStatus());

		// nothing to add or remove in this scenario
		verify(mockDocuSignEnvelopesApi, never()).createRecipients(any(), any(), eq(true));
		verify(mockDocuSignEnvelopesApi, never()).deleteRecipients(any(), any());
	}

	@Test
	public void testCorrectEnvelopeAddsAndRemovesCollaborators() {
		// creator/collaborator_1 signed; collaborator_2 pending and about to be removed
		Signer pi = existingSigner("1", "principal_investigator", "completed");
		Signer collab1 = existingSigner("2", "collaborator_1", "completed");
		Signer collab2 = existingSigner("3", "collaborator_2", "sent");
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(pi, collab1, collab2));
		Envelope existing = new Envelope();
		existing.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(existing);

		// the envelope reports the template it was created from, and that template places
		// collaborator_3's tabs and routes it ahead of the other roles
		stubEnvelopeTemplate("env-1", "tpl-1");
		EnvelopeTemplate template = TestTemplateHelper.buildValidTemplate(3);
		Signer templateCollab3 = TestTemplateHelper.findSigner(template, "collaborator_3");
		templateCollab3.setRoutingOrder("2");
		placeTab(templateCollab3.getTabs().getFullNameTabs().get(0), "3", "7", "100", "200");
		when(mockDocuSignTemplatesApi.getTemplate("tpl-1")).thenReturn(template);

		// desired: keep PI and collaborator_1, drop collaborator_2, add collaborator_3
		Map<String, RecipientInfo> desiredRecipients = Map.of(
				"principal_investigator", new RecipientInfo("pi@example.com", "Dr. Jones"),
				"collaborator_1", new RecipientInfo("c1@example.com", "Collab One"),
				"collaborator_3", new RecipientInfo("c3@example.com", "New Collaborator")
		);
		Map<RoleLabelKey, String> tabValues = Map.of(
				new RoleLabelKey("collaborator_3", "collaborator_3_name"), "New Collaborator"
		);

		// call under test
		client.correctEnvelope("env-1", desiredRecipients, tabValues);

		// collaborator_2 (pending, no longer desired) is deleted
		ArgumentCaptor<Recipients> deleteCaptor = ArgumentCaptor.forClass(Recipients.class);
		verify(mockDocuSignEnvelopesApi).deleteRecipients(eq("env-1"), deleteCaptor.capture());
		List<Signer> deleted = deleteCaptor.getValue().getSigners();
		assertEquals(1, deleted.size());
		assertEquals("3", deleted.get(0).getRecipientId());
		assertEquals("collaborator_2", deleted.get(0).getRoleName());

		// collaborator_3 (new) is added with a fresh recipientId beyond the existing max (3)
		ArgumentCaptor<Recipients> createCaptor = ArgumentCaptor.forClass(Recipients.class);
		verify(mockDocuSignEnvelopesApi).createRecipients(eq("env-1"), createCaptor.capture(), eq(true));
		List<Signer> created = createCaptor.getValue().getSigners();
		assertEquals(1, created.size());
		Signer addedCollaborator = created.get(0);
		assertEquals("collaborator_3", addedCollaborator.getRoleName());
		assertEquals("c3@example.com", addedCollaborator.getEmail());
		assertEquals("4", addedCollaborator.getRecipientId());
		assertEquals("New Collaborator", addedCollaborator.getName());
		// the routing order comes from the template, not from the recipient ID
		assertEquals("2", addedCollaborator.getRoutingOrder());

		// the tabs carry the template's placement, and the signature and date tabs — which never
		// carry a value — come across too
		Tabs addedTabs = addedCollaborator.getTabs();
		FullName addedName = addedTabs.getFullNameTabs().get(0);
		assertEquals("collaborator_3_name", addedName.getTabLabel());
		assertEquals("New Collaborator", addedName.getValue());
		assertEquals("3", addedName.getDocumentId());
		assertEquals("7", addedName.getPageNumber());
		assertEquals("100", addedName.getXPosition());
		assertEquals("200", addedName.getYPosition());
		assertEquals("collaborator_3_signature", addedTabs.getSignHereTabs().get(0).getTabLabel());
		assertEquals("collaborator_3_date", addedTabs.getDateSignedTabs().get(0).getTabLabel());

		// no not-yet-signed existing role remains to update (PI and collaborator_1 are completed)
		verify(mockDocuSignEnvelopesApi, never()).updateRecipients(any(), any(), eq(true));
	}

	@Test
	public void testGetRecipients() {
		Signer pi = existingSigner("1", "principal_investigator", "completed");
		pi.setEmail("pi@example.com");
		Signer collaborator = existingSigner("2", "collaborator_1", "sent");
		collaborator.setEmail("c1@example.com");
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(pi, collaborator));
		Envelope envelope = new Envelope();
		envelope.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(envelope);

		// call under test
		List<EnvelopeRecipient> result = client.getRecipients("env-1");

		assertEquals(List.of(
				new EnvelopeRecipient("principal_investigator", "pi@example.com", true),
				new EnvelopeRecipient("collaborator_1", "c1@example.com", false)), result);
	}

	@Test
	public void testGetRecipientsWithNoRecipients() {
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(new Envelope());

		// call under test
		assertEquals(List.of(), client.getRecipients("env-1"));
	}

	@Test
	public void testGetRecipientsWithNullEnvelopeId() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.getRecipients(null));
		assertEquals("envelopeId is required.", ex.getMessage());
		verifyNoInteractions(mockDocuSignEnvelopesApi);
	}

	private void stubEnvelopeTemplate(String envelopeId, String... templateIds) {
		TemplateInformation templateInformation = new TemplateInformation();
		List<TemplateSummary> summaries = new ArrayList<>();
		for (String templateId : templateIds) {
			TemplateSummary summary = new TemplateSummary();
			summary.setTemplateId(templateId);
			summaries.add(summary);
		}
		templateInformation.setTemplates(summaries);
		when(mockDocuSignEnvelopesApi.listTemplates(envelopeId)).thenReturn(templateInformation);
	}

	@Test
	public void testCorrectEnvelopeWithEnvelopeReportingNoTemplate() {
		// Without knowing the template the envelope was built from there is nowhere to take the tab
		// placements of the recipient being added from, so the correction fails before the envelope
		// is touched.
		Signer pi = existingSigner("1", "principal_investigator", "sent");
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(pi));
		Envelope existing = new Envelope();
		existing.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(existing);
		stubEnvelopeTemplate("env-1");

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				// call under test — collaborator_1 has to be added
				() -> client.correctEnvelope("env-1",
						Map.of("collaborator_1", new RecipientInfo("c1@example.com", "Collab One")),
						Map.of()));

		assertTrue(ex.getMessage().contains("exactly one template but found 0"));
		// the envelope was never placed into the "correct" state
		verify(mockDocuSignEnvelopesApi, never()).updateEnvelope(any(), any());
	}

	@Test
	public void testCorrectEnvelopeWithEnvelopeReportingSeveralTemplates() {
		Signer pi = existingSigner("1", "principal_investigator", "sent");
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(pi));
		Envelope existing = new Envelope();
		existing.setRecipients(recipients);
		when(mockDocuSignEnvelopesApi.getEnvelope("env-1")).thenReturn(existing);
		stubEnvelopeTemplate("env-1", "tpl-1", "tpl-2");

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				// call under test
				() -> client.correctEnvelope("env-1",
						Map.of("collaborator_1", new RecipientInfo("c1@example.com", "Collab One")),
						Map.of()));

		assertTrue(ex.getMessage().contains("exactly one template but found 2"));
		verify(mockDocuSignEnvelopesApi, never()).updateEnvelope(any(), any());
	}

	@Test
	public void testCorrectEnvelopeWithNullEnvelopeId() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.correctEnvelope(null, Map.of(), Map.of()));
		assertEquals("envelopeId is required.", ex.getMessage());
		verifyNoInteractions(mockDocuSignEnvelopesApi);
	}

	@Test
	public void testCorrectEnvelopeWithNullRecipients() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.correctEnvelope("env-1", null, Map.of()));
		assertEquals("recipients is required.", ex.getMessage());
		verifyNoInteractions(mockDocuSignEnvelopesApi);
	}

	@Test
	public void testCorrectEnvelopeWithNullTabValues() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> client.correctEnvelope("env-1", Map.of(), null));
		assertEquals("tabValues is required.", ex.getMessage());
		verifyNoInteractions(mockDocuSignEnvelopesApi);
	}

	@Test
	public void testBuildUpdatedRecipientsSkipsCompletedSigner() {
		Signer completed = existingSigner("1", "principal_investigator", "completed");
		Signer pending = existingSigner("2", "signing_official", "sent");

		Recipients result = DocuSignClient.buildUpdatedRecipients(List.of(completed, pending),
				Map.of("principal_investigator", new RecipientInfo("pi@example.com", "Dr. Jones"),
						"signing_official", new RecipientInfo("so@example.com", "Jane Admin")),
				Map.of());

		assertEquals(1, result.getSigners().size());
		assertEquals("signing_official", result.getSigners().get(0).getRoleName());
	}

	@Test
	public void testBuildRemovedRecipientsSkipsCompletedSigner() {
		// a completed signer no longer desired must NOT be removed
		Signer completed = existingSigner("1", "collaborator_1", "completed");
		Signer pending = existingSigner("2", "collaborator_2", "sent");

		Recipients result = DocuSignClient.buildRemovedRecipients(List.of(completed, pending), Map.of());

		assertEquals(1, result.getSigners().size());
		assertEquals("collaborator_2", result.getSigners().get(0).getRoleName());
	}

	// The template's signer roles, keyed by role name, as DocuSignClient reads them.
	private static Map<String, Signer> templateSigners(int numCollaborators) {
		Map<String, Signer> signersByRole = new HashMap<>();
		for (Signer signer : TestTemplateHelper.buildValidTemplate(numCollaborators)
				.getRecipients().getSigners()) {
			signersByRole.put(signer.getRoleName(), signer);
		}
		return signersByRole;
	}

	private static void placeTab(FullName tab, String documentId, String pageNumber, String x, String y) {
		tab.setDocumentId(documentId);
		tab.setPageNumber(pageNumber);
		tab.setXPosition(x);
		tab.setYPosition(y);
	}

	@Test
	public void testBuildNewRecipientsOnlyAddsMissingRoles() {
		Signer pi = existingSigner("1", "principal_investigator", "sent");

		// call under test
		Recipients result = DocuSignClient.buildNewRecipients(List.of(pi), () -> templateSigners(1),
				Map.of("principal_investigator", new RecipientInfo("pi@example.com", "Dr. Jones"),
						"collaborator_1", new RecipientInfo("c1@example.com", "Collab One")),
				Map.of());

		assertEquals(1, result.getSigners().size());
		assertEquals("collaborator_1", result.getSigners().get(0).getRoleName());
		assertEquals("2", result.getSigners().get(0).getRecipientId());
	}

	@Test
	public void testBuildNewRecipientsTakesRoutingOrderFromTemplate() {
		// The routing order of an added signer is the template's, not the recipient ID it happens to
		// be assigned, so a collaborator still routes ahead of the principal investigator and
		// signing official even though its recipient ID is the highest on the envelope.
		Signer pi = existingSigner("4", "principal_investigator", "completed");
		Signer so = existingSigner("5", "signing_official", "sent");
		Map<String, Signer> templateSigners = templateSigners(1);
		templateSigners.get("collaborator_1").setRoutingOrder("2");
		templateSigners.get("principal_investigator").setRoutingOrder("3");
		templateSigners.get("signing_official").setRoutingOrder("4");

		// call under test
		Recipients result = DocuSignClient.buildNewRecipients(List.of(pi, so), () -> templateSigners,
				Map.of("principal_investigator", new RecipientInfo("pi@example.com", "Dr. Jones"),
						"signing_official", new RecipientInfo("so@example.com", "Jane Admin"),
						"collaborator_1", new RecipientInfo("c1@example.com", "Collab One")),
				Map.of());

		assertEquals(1, result.getSigners().size());
		Signer added = result.getSigners().get(0);
		assertEquals("collaborator_1", added.getRoleName());
		assertEquals("6", added.getRecipientId());
		assertEquals("2", added.getRoutingOrder());
	}

	@Test
	public void testBuildNewRecipientsWithTemplateRoutingOrderMissing() {
		// A template role should always carry a routing order; if it does not, the added signer
		// routes as early as possible rather than after everyone else.
		Signer pi = existingSigner("4", "principal_investigator", "completed");
		Map<String, Signer> templateSigners = templateSigners(1);
		templateSigners.get("collaborator_1").setRoutingOrder(null);

		// call under test
		Recipients result = DocuSignClient.buildNewRecipients(List.of(pi), () -> templateSigners,
				Map.of("collaborator_1", new RecipientInfo("c1@example.com", "Collab One")), Map.of());

		assertEquals("1", result.getSigners().get(0).getRoutingOrder());
	}

	@Test
	public void testBuildNewRecipientsCopiesTabPlacementFromTemplate() {
		// The envelope recipient's own tabs were deleted along with it, so the template supplies the
		// placement that a tab needs in order to appear on the document.
		Signer pi = existingSigner("1", "principal_investigator", "sent");
		Map<String, Signer> templateSigners = templateSigners(1);
		Tabs templateTabs = templateSigners.get("collaborator_1").getTabs();
		placeTab(templateTabs.getFullNameTabs().get(0), "2", "5", "42", "84");
		// the template's own identifiers are not valid in the envelope and must not be carried over
		templateTabs.getFullNameTabs().get(0).setTabId("template-tab-id");
		templateTabs.getSignHereTabs().get(0).setRecipientId("template-recipient-id");

		// call under test
		Recipients result = DocuSignClient.buildNewRecipients(List.of(pi), () -> templateSigners,
				Map.of("collaborator_1", new RecipientInfo("c1@example.com", "Collab One")),
				Map.of(new RoleLabelKey("collaborator_1", "collaborator_1_name"), "Alice Smith",
						new RoleLabelKey("collaborator_1", "collaborator_1_user_name"), "alice"));

		Tabs tabs = result.getSigners().get(0).getTabs();

		FullName name = tabs.getFullNameTabs().get(0);
		assertEquals("collaborator_1_name", name.getTabLabel());
		assertEquals("Alice Smith", name.getValue());
		assertEquals("2", name.getDocumentId());
		assertEquals("5", name.getPageNumber());
		assertEquals("42", name.getXPosition());
		assertEquals("84", name.getYPosition());
		// DocuSign assigns the tab ID; every tab points at the recipient being added
		assertNull(name.getTabId());
		assertEquals("2", name.getRecipientId());

		assertEquals("alice", tabs.getTextTabs().get(0).getValue());

		// the signature and date tabs carry no value and so are absent from tabValues, but must
		// still reach the recipient or nothing appears on the document for them to sign
		assertEquals("collaborator_1_signature", tabs.getSignHereTabs().get(0).getTabLabel());
		assertEquals("2", tabs.getSignHereTabs().get(0).getRecipientId());
		assertEquals("collaborator_1_date", tabs.getDateSignedTabs().get(0).getTabLabel());
		assertEquals("2", tabs.getDateSignedTabs().get(0).getRecipientId());
	}

	@Test
	public void testBuildNewRecipientsWithMissingName() {
		// DocuSign rejects a recipient without a name, so the role is rejected here instead — while
		// the changes are still being assembled and none has been applied to the envelope
		Signer pi = existingSigner("1", "principal_investigator", "sent");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> DocuSignClient.buildNewRecipients(List.of(pi), () -> templateSigners(1),
						Map.of("collaborator_1", new RecipientInfo("c1@example.com", null)), Map.of()));

		assertTrue(ex.getMessage().contains("name for role 'collaborator_1'"));
	}

	@Test
	public void testBuildNewRecipientsWithMissingEmail() {
		Signer pi = existingSigner("1", "principal_investigator", "sent");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> DocuSignClient.buildNewRecipients(List.of(pi), () -> templateSigners(1),
						Map.of("collaborator_1", new RecipientInfo(null, "Collab One")), Map.of()));

		assertTrue(ex.getMessage().contains("email for role 'collaborator_1'"));
	}

	@Test
	public void testBuildUpdatedRecipientsWithMissingName() {
		Signer pending = existingSigner("1", "collaborator_1", "sent");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> DocuSignClient.buildUpdatedRecipients(List.of(pending),
						Map.of("collaborator_1", new RecipientInfo("c1@example.com", null)), Map.of()));

		assertTrue(ex.getMessage().contains("name for role 'collaborator_1'"));
	}

	@Test
	public void testBuildUpdatedRecipientsSetsNameFromRecipientInfo() {
		// the name is taken from the recipient rather than from the role's full-name tab value, so
		// that it is present even for a collaborator whose profile has no name
		Signer pending = existingSigner("1", "collaborator_1", "sent");

		// call under test
		Recipients result = DocuSignClient.buildUpdatedRecipients(List.of(pending),
				Map.of("collaborator_1", new RecipientInfo("c1@example.com", "collab1username")),
				Map.of(new RoleLabelKey("collaborator_1", "collaborator_1_user_name"), "collab1username"));

		assertEquals("collab1username", result.getSigners().get(0).getName());
		assertEquals("c1@example.com", result.getSigners().get(0).getEmail());
	}

	@Test
	public void testBuildNewRecipientsWithRoleMissingFromTemplate() {
		// the template defines only collaborator_1, so collaborator_2 cannot be placed
		Signer pi = existingSigner("1", "principal_investigator", "sent");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> DocuSignClient.buildNewRecipients(List.of(pi), () -> templateSigners(1),
						Map.of("collaborator_2", new RecipientInfo("c2@example.com", "Collab Two")), Map.of()));

		assertEquals("The template does not define the role 'collaborator_2'.", ex.getMessage());
	}

	@Test
	public void testBuildNewRecipientsRoutesEveryAddedSignerTogether() {
		// The template gives all collaborators the same routing order, so collaborators added in the
		// same correction are routed in parallel rather than sequentially.
		Signer pi = existingSigner("1", "principal_investigator", "sent");
		Map<String, Signer> templateSigners = templateSigners(2);
		templateSigners.get("collaborator_1").setRoutingOrder("2");
		templateSigners.get("collaborator_2").setRoutingOrder("2");

		// call under test
		Recipients result = DocuSignClient.buildNewRecipients(List.of(pi), () -> templateSigners,
				Map.of("collaborator_1", new RecipientInfo("c1@example.com", "Collab One"),
						"collaborator_2", new RecipientInfo("c2@example.com", "Collab Two")),
				Map.of());

		assertEquals(2, result.getSigners().size());
		Set<String> recipientIds = new HashSet<>();
		for (Signer added : result.getSigners()) {
			assertEquals("2", added.getRoutingOrder());
			recipientIds.add(added.getRecipientId());
		}
		// the recipient IDs remain distinct even though the routing order is shared
		assertEquals(Set.of("2", "3"), recipientIds);
	}
}
