package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.educ.EDucTemplate;
import org.sagebionetworks.repo.model.educ.EDucTemplatePage;

import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;
import org.sagebionetworks.repo.model.educ.EDucSignerStatusEnum;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;

import com.docusign.esign.client.ApiException;

import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.EnvelopeTemplateResults;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.TemplateRole;

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

		Map<String, String> roleEmails = Map.of(
				"signing_official", "so@example.com",
				"principal_investigator", "pi@example.com"
		);
		Map<RoleLabelKey, String> tabValues = Map.of(
				new RoleLabelKey("signing_official", "signing_official_name"), "Dr. Smith",
				new RoleLabelKey("principal_investigator", "principal_investigator_name"), "Dr. Jones"
		);

		// call under test
		String envelopeId = client.createEnvelope("tpl-1", roleEmails, tabValues);

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

		Map<String, String> roleEmails = Map.of("signing_official", "so@example.com");
		Map<RoleLabelKey, String> tabValues = Map.of();

		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> client.createEnvelope("tpl-1", roleEmails, tabValues));

		verifyNoInteractions(mockDocuSignEnvelopesApi);
	}

	@Test
	public void testBuildTemplateRolesWithCorrectTabTypes() {
		Map<String, String> roleEmails = Map.of(
				"signing_official", "so@example.com",
				"principal_investigator", "pi@example.com"
		);
		Map<RoleLabelKey, String> tabValues = Map.of(
				new RoleLabelKey("signing_official", "signing_official_name"), "Dr. Smith",
				new RoleLabelKey("signing_official", "signing_official_title"), "Director",
				new RoleLabelKey("signing_official", "signing_official_email"), "so@example.com",
				new RoleLabelKey("signing_official", "signing_official_institution"), "MIT"
		);

		// call under test
		List<TemplateRole> roles = DocuSignClient.buildTemplateRoles(roleEmails, tabValues);

		assertEquals(2, roles.size());
		TemplateRole soRole = roles.stream()
				.filter(r -> "signing_official".equals(r.getRoleName())).findFirst().orElseThrow();
		assertEquals("so@example.com", soRole.getEmail());
		assertEquals("Dr. Smith", soRole.getName());
		assertEquals(1, soRole.getTabs().getFullNameTabs().size());
		assertEquals("signing_official_name", soRole.getTabs().getFullNameTabs().get(0).getTabLabel());
		assertEquals(1, soRole.getTabs().getTitleTabs().size());
		assertEquals("Director", soRole.getTabs().getTitleTabs().get(0).getValue());
		assertEquals(1, soRole.getTabs().getEmailTabs().size());
		assertEquals("so@example.com", soRole.getTabs().getEmailTabs().get(0).getValue());
		assertEquals("MIT", soRole.getTabs().getTextTabs().get(0).getValue());
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
	public void testGetEnvelopeStatusSuccess() {
		Signer signer1 = new Signer();
		signer1.setName("Dr. Jones");
		signer1.setEmail("pi@university.edu");
		signer1.setStatus("completed");
		Signer signer2 = new Signer();
		signer2.setName("Jane Admin");
		signer2.setEmail("so@university.edu");
		signer2.setStatus("sent");

		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(signer1, signer2));

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
		assertEquals(2, status.getSignerStatus().size());
		assertEquals("Dr. Jones", status.getSignerStatus().get(0).getName());
		assertEquals(EDucSignerStatusEnum.done, status.getSignerStatus().get(0).getStatus());
		assertEquals("Jane Admin", status.getSignerStatus().get(1).getName());
		assertEquals(EDucSignerStatusEnum.pending, status.getSignerStatus().get(1).getStatus());

		assertEquals(List.of("pi@university.edu", "so@university.edu"), result.signerEmails());
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

	@Test
	public void testSendEnvelopeSuccess() {
		// call under test
		client.sendEnvelope("env-1");

		ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
		verify(mockDocuSignEnvelopesApi).updateEnvelope(eq("env-1"), captor.capture());
		assertEquals("sent", captor.getValue().getStatus());
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
}
