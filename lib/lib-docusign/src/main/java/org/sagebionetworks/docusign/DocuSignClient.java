package org.sagebionetworks.docusign;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;
import org.sagebionetworks.repo.model.educ.EDucSignerStatus;
import org.sagebionetworks.repo.model.educ.EDucSignerStatusEnum;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.educ.EDucTemplate;
import org.sagebionetworks.repo.model.educ.EDucTemplatePage;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.EnvelopeTemplateResults;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.TemplateRole;

import io.jsonwebtoken.lang.Collections;

/**
 * Client for the DocuSign REST API. Authenticates headlessly via the JWT Bearer
 * Grant: a JWT assertion signed with the configured RSA private key is
 * exchanged for an access token, which is cached until just before its expiry.
 */
@Service
public class DocuSignClient {

	private final DocuSignTemplatesApi templatesApi;
	private final DocuSignEnvelopesApi envelopesApi;

	DocuSignClient(DocuSignTemplatesApi templatesApi, DocuSignEnvelopesApi envelopesApi) {
		this.templatesApi = templatesApi;
		this.envelopesApi = envelopesApi;
	}

	/**
	 * List a page of templates from the configured DocuSign account.
	 *
	 * @param startPosition 0-based offset into the DocuSign template set
	 * @param count page size to request from DocuSign
	 * @return a page of templates; the {@code nextPageToken} field is left null
	 *         (callers are responsible for assembling the Synapse-style token)
	 */
	public EDucTemplatePage listTemplates(int startPosition, int count) {
		EnvelopeTemplateResults results = templatesApi.listTemplates(
				String.valueOf(startPosition), String.valueOf(count));
		return toEDucTemplatePage(results);
	}

	/**
	 * Validates that the given template has the required signer roles and tabs.
	 * Throws IllegalArgumentException if the template does not meet requirements.
	 */
	public void validateTemplate(String templateId) {
		ValidateArgument.required(templateId, "templateId");
		EnvelopeTemplate template = templatesApi.getTemplate(templateId);
		DocuSignTemplateValidator.validate(template);
	}

	/**
	 * Creates a draft envelope from the specified template without sending it.
	 *
	 * @param templateId the DocuSign template ID
	 * @param roleEmails map from role name to the signer's email address
	 * @param tabValues map from (roleName, tabLabel) to the text value to pre-fill
	 * @return the envelope ID of the created draft envelope
	 */
	public String createEnvelope(String templateId, Map<String, String> roleEmails,
			Map<RoleLabelKey, String> tabValues) {
		ValidateArgument.required(templateId, "templateId");
		ValidateArgument.required(roleEmails, "roleEmails");
		ValidateArgument.required(tabValues, "tabValues");

		EnvelopeTemplate template = templatesApi.getTemplate(templateId);
		DocuSignTemplateValidator.validate(template);

		List<TemplateRole> templateRoles = buildTemplateRoles(roleEmails, tabValues);
		EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
		envelopeDefinition.setTemplateId(templateId);
		envelopeDefinition.setTemplateRoles(templateRoles);
		envelopeDefinition.setStatus("created");

		EnvelopeSummary summary = envelopesApi.createEnvelope(envelopeDefinition);
		return summary.getEnvelopeId();
	}

	/**
	 * Sends an existing draft envelope.
	 *
	 * @param envelopeId the ID of the draft envelope to send
	 */
	public void sendEnvelope(String envelopeId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		Envelope envelope = new Envelope();
		envelope.setStatus("sent");
		envelopesApi.updateEnvelope(envelopeId, envelope);
	}

	static List<TemplateRole> buildTemplateRoles(Map<String, String> roleEmails,
			Map<RoleLabelKey, String> tabValues) {
		List<TemplateRole> roles = new ArrayList<>();
		for (Map.Entry<String, String> entry : roleEmails.entrySet()) {
			String roleName = entry.getKey();
			String email = entry.getValue();

			TemplateRole role = new TemplateRole();
			role.setRoleName(roleName);
			role.setEmail(email);

			Tabs tabs = new Tabs();
			role.setTabs(tabs);

			for (Map.Entry<RoleLabelKey, String> tabEntry : tabValues.entrySet()) {
				if (!tabEntry.getKey().roleName().equals(roleName)) {
					continue;
				}
				String tabLabel = tabEntry.getKey().tabLabel();
				String value = tabEntry.getValue();
				TabType type = DocuSignTemplateValidator.typeforRoleAndLabel(roleName, tabLabel);
				type.fillInTabValue(tabs, tabLabel, value);
				if (TabType.FULL_NAME.equals(type)) {
					role.setName(value);
				}
			}

			roles.add(role);
		}
		return roles;
	}

	public void voidEnvelope(String envelopeId, String reason) {
		ValidateArgument.required(envelopeId, "envelopeId");
		ValidateArgument.required(reason, "reason");
		envelopesApi.voidEnvelope(envelopeId, reason);
	}

	/*
	 * Return the status for the given envelope.
	 * Note, email addresses are omitted from the EDucSignatureStatus DTO though
	 * they are needed by the caller to determine which (if any) Synapse
	 * user the signer is, so this method returns the list of email
	 * addresses alongside the EDucSignatureStatus object.
	 */
	public EnvelopeStatusResult getEnvelopeStatus(String envelopeId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		Envelope envelope = envelopesApi.getEnvelope(envelopeId);

		EDucSignatureStatus status = new EDucSignatureStatus();
		status.setCreatedOn(parseDate(envelope.getCreatedDateTime()));
		status.setModifiedOn(parseDate(envelope.getLastModifiedDateTime()));
		status.setDucStatus(toEDucStatusEnum(envelope.getStatus()));

		List<EDucSignerStatus> signerStatuses = new ArrayList<>();
		List<String> signerEmails = new ArrayList<>();
		if (envelope.getRecipients() != null && envelope.getRecipients().getSigners() != null) {
			for (Signer signer : envelope.getRecipients().getSigners()) {
				EDucSignerStatus signerStatus = new EDucSignerStatus();
				signerStatus.setName(signer.getName());
				signerStatus.setStatus(toEDucSignerStatusEnum(signer.getStatus()));
				signerStatuses.add(signerStatus);
				signerEmails.add(signer.getEmail());
			}
		}
		status.setSignerStatus(signerStatuses);

		return new EnvelopeStatusResult(status, signerEmails);
	}

	public List<Envelope> listEnvelopeStatuses(List<String> envelopeIds) {
		if (Collections.isEmpty(envelopeIds)) {
			return List.of();
		}
		return envelopesApi.listStatus(envelopeIds);
	}

	public byte[] getDocument(String envelopeId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		return envelopesApi.getDocument(envelopeId, "combined");
	}
	
	public byte[] getSignedDocument(String envelopeId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		Envelope envelope = envelopesApi.getEnvelope(envelopeId);
		String status = envelope.getStatus();
		if (!"completed".equalsIgnoreCase(status)) {
			throw new IllegalArgumentException("Cannot retrieve signed document: envelope status is " + status + ".");
		}
		return envelopesApi.getDocument(envelopeId, "combined");
	}

	public static EDucStatusEnum toEDucStatusEnum(String docuSignStatus) {
		if (docuSignStatus == null) {
			return null;
		}
		switch (docuSignStatus.toLowerCase()) {
			case "created":
				return EDucStatusEnum.draft;
			case "sent":
				return EDucStatusEnum.sent;
			case "delivered":
				return EDucStatusEnum.delivered;
			case "completed":
			case "signed":
				return EDucStatusEnum.completed;
			case "declined":
				return EDucStatusEnum.declined;
			case "voided":
				return EDucStatusEnum.voided;
			case "correct":
				return EDucStatusEnum.correct;
			default:
				throw new IllegalArgumentException("Unexpected status " + docuSignStatus);
		}
	}

	static EDucSignerStatusEnum toEDucSignerStatusEnum(String docuSignStatus) {
		if (docuSignStatus == null) {
			return EDucSignerStatusEnum.pending;
		}
		switch (docuSignStatus.toLowerCase()) {
			case "sent":
			case "delivered":
			case "created":
			case "faxpending":
				return EDucSignerStatusEnum.pending;
			case "completed":
			case "signed":
				return EDucSignerStatusEnum.done;
			case "declined":
				return EDucSignerStatusEnum.declined;
			case "autoresponded":
				return EDucSignerStatusEnum.bounced;
			default:
				throw new IllegalArgumentException("Unexpected status " + docuSignStatus);
		}
	}

	static EDucTemplatePage toEDucTemplatePage(EnvelopeTemplateResults results) {
		EDucTemplatePage page = new EDucTemplatePage();
		List<EnvelopeTemplate> templates = results == null ? null : results.getEnvelopeTemplates();
		if (templates == null) {
			page.setResults(java.util.Collections.emptyList());
			return page;
		}
		java.util.List<EDucTemplate> mapped = new java.util.ArrayList<>(templates.size());
		for (EnvelopeTemplate t : templates) {
			mapped.add(toEDucTemplate(t));
		}
		page.setResults(mapped);
		return page;
	}

	static EDucTemplate toEDucTemplate(EnvelopeTemplate t) {
		EDucTemplate out = new EDucTemplate();
		out.setTemplateId(t.getTemplateId());
		out.setName(t.getName());
		out.setDescription(t.getDescription());
		out.setCreatedOn(parseDate(t.getCreated()));
		out.setModifiedOn(parseDate(t.getLastModified()));
		return out;
	}

	public static Date parseDate(String iso8601) {
		if (iso8601 == null || iso8601.isEmpty()) {
			return null;
		}
		return Date.from(Instant.parse(iso8601));
	}

}
