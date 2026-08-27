package org.sagebionetworks.docusign;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.docusign.esign.model.Recipients;
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
	 * @param recipients map from role name to the signer's email and name (both are required for
	 *        every role before the envelope can be sent)
	 * @param tabValues map from (roleName, tabLabel) to the text value to pre-fill
	 * @return the envelope ID of the created draft envelope
	 */
	public String createEnvelope(String templateId, Map<String, RecipientInfo> recipients,
			Map<RoleLabelKey, String> tabValues) {
		ValidateArgument.required(templateId, "templateId");
		ValidateArgument.required(recipients, "recipients");
		ValidateArgument.required(tabValues, "tabValues");

		EnvelopeTemplate template = templatesApi.getTemplate(templateId);
		DocuSignTemplateValidator.validate(template);

		List<TemplateRole> templateRoles = buildTemplateRoles(recipients, tabValues);
		EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
		envelopeDefinition.setTemplateId(templateId);
		envelopeDefinition.setTemplateRoles(templateRoles);
		envelopeDefinition.setStatus("created");

		EnvelopeSummary summary = envelopesApi.createEnvelope(envelopeDefinition);
		return summary.getEnvelopeId();
	}

	/**
	 * Prepares and sends an existing draft envelope. Any unused collaborator recipients that the
	 * template defined but the request did not fill (they have no email) are removed first, since
	 * DocuSign rejects sending an envelope that has an unresolved recipient.
	 *
	 * @param envelopeId the ID of the draft envelope to send
	 */
	public void sendEnvelope(String envelopeId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		removeUnusedCollaboratorRecipients(envelopeId);
		Envelope envelope = new Envelope();
		envelope.setStatus("sent");
		envelopesApi.updateEnvelope(envelopeId, envelope);
	}

	// A DocuSign signer whose status is one of these has already finished and must not be modified.
	private static final Set<String> COMPLETED_SIGNER_STATUSES = Set.of("completed", "signed");

	/**
	 * Applies new signer emails and tab values to an in-flight (sent or delivered) envelope by
	 * correcting it: the envelope is placed into the "correct" state (which pauses signing),
	 * recipients are added, updated, and removed to match the desired content, and the envelope is
	 * then re-sent. Recipients who have already signed are left untouched.
	 *
	 * @param envelopeId the ID of the envelope to correct
	 * @param roleEmails map from role name to the signer's desired email address
	 * @param tabValues map from (roleName, tabLabel) to the desired text value to pre-fill
	 */
	public void correctEnvelope(String envelopeId, Map<String, String> roleEmails,
			Map<RoleLabelKey, String> tabValues) {
		ValidateArgument.required(envelopeId, "envelopeId");
		ValidateArgument.required(roleEmails, "roleEmails");
		ValidateArgument.required(tabValues, "tabValues");

		// Fetch the current recipients to obtain their recipientIds and per-signer status.
		Envelope existing = envelopesApi.getEnvelope(envelopeId);
		List<Signer> existingSigners = existing.getRecipients() == null ? List.of()
				: existing.getRecipients().getSigners();
		if (existingSigners == null) {
			existingSigners = List.of();
		}

		// Place the envelope into the "correct" state, which pauses the signing process.
		Envelope correcting = new Envelope();
		correcting.setStatus("correct");
		envelopesApi.updateEnvelope(envelopeId, correcting);

		Recipients toDelete = buildRemovedRecipients(existingSigners, roleEmails);
		Recipients toUpdate = buildUpdatedRecipients(existingSigners, roleEmails, tabValues);
		Recipients toCreate = buildNewRecipients(existingSigners, roleEmails, tabValues);

		if (hasSigners(toDelete)) {
			envelopesApi.deleteRecipients(envelopeId, toDelete);
		}
		if (hasSigners(toUpdate)) {
			envelopesApi.updateRecipients(envelopeId, toUpdate, true);
		}
		if (hasSigners(toCreate)) {
			envelopesApi.createRecipients(envelopeId, toCreate, true);
		}

		// Take the envelope out of "correct" and re-send it. "delivered" is a DocuSign-derived
		// status that cannot be set, so the resume transition is always to "sent"; DocuSign
		// re-derives "delivered"/"completed" as recipients act.
		Envelope resending = new Envelope();
		resending.setStatus("sent");
		envelopesApi.updateEnvelope(envelopeId, resending);
	}

	// Existing (not-yet-signed) signers whose role is no longer desired are removed.
	static Recipients buildRemovedRecipients(List<Signer> existingSigners, Map<String, String> roleEmails) {
		List<Signer> removed = new ArrayList<>();
		for (Signer existing : existingSigners) {
			if (isCompleted(existing)) {
				continue;
			}
			if (!roleEmails.containsKey(existing.getRoleName())) {
				Signer signer = new Signer();
				signer.setRecipientId(existing.getRecipientId());
				signer.setRoleName(existing.getRoleName());
				removed.add(signer);
			}
		}
		return toRecipients(removed);
	}

	// Existing (not-yet-signed) signers whose role is still desired get their email and tabs re-applied.
	static Recipients buildUpdatedRecipients(List<Signer> existingSigners,
			Map<String, String> roleEmails, Map<RoleLabelKey, String> tabValues) {
		List<Signer> updated = new ArrayList<>();
		for (Signer existing : existingSigners) {
			if (isCompleted(existing)) {
				continue;
			}
			String roleName = existing.getRoleName();
			if (!roleEmails.containsKey(roleName)) {
				continue;
			}
			Signer signer = new Signer();
			signer.setRecipientId(existing.getRecipientId());
			signer.setRoleName(roleName);
			signer.setEmail(roleEmails.get(roleName));
			Tabs tabs = new Tabs();
			signer.setTabs(tabs);
			String fullName = fillTabsForRole(roleName, tabs, tabValues);
			if (fullName != null) {
				signer.setName(fullName);
			}
			updated.add(signer);
		}
		return toRecipients(updated);
	}

	// Desired roles that are not present on the envelope are added as new recipients.
	static Recipients buildNewRecipients(List<Signer> existingSigners,
			Map<String, String> roleEmails, Map<RoleLabelKey, String> tabValues) {
		Set<String> existingRoles = new HashSet<>();
		int maxRecipientId = 0;
		for (Signer existing : existingSigners) {
			existingRoles.add(existing.getRoleName());
			maxRecipientId = Math.max(maxRecipientId, parseRecipientId(existing.getRecipientId()));
		}

		List<Signer> created = new ArrayList<>();
		int nextRecipientId = maxRecipientId;
		for (Map.Entry<String, String> entry : roleEmails.entrySet()) {
			String roleName = entry.getKey();
			if (existingRoles.contains(roleName)) {
				continue;
			}
			nextRecipientId++;
			Signer signer = new Signer();
			signer.setRecipientId(Integer.toString(nextRecipientId));
			signer.setRoutingOrder(Integer.toString(nextRecipientId));
			signer.setRoleName(roleName);
			signer.setEmail(entry.getValue());
			Tabs tabs = new Tabs();
			signer.setTabs(tabs);
			String fullName = fillTabsForRole(roleName, tabs, tabValues);
			if (fullName != null) {
				signer.setName(fullName);
			}
			created.add(signer);
		}
		return toRecipients(created);
	}

	private static boolean isCompleted(Signer signer) {
		return signer.getStatus() != null
				&& COMPLETED_SIGNER_STATUSES.contains(signer.getStatus().toLowerCase());
	}

	private static int parseRecipientId(String recipientId) {
		try {
			return Integer.parseInt(recipientId);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Recipients toRecipients(List<Signer> signers) {
		Recipients recipients = new Recipients();
		recipients.setSigners(signers);
		return recipients;
	}

	private static boolean hasSigners(Recipients recipients) {
		return recipients.getSigners() != null && !recipients.getSigners().isEmpty();
	}

	/**
	 * Removes collaborator recipients that were instantiated from the template but never filled in
	 * (i.e. have no email). Required roles (principal investigator, signing official) are never
	 * removed — if one of those is unresolved, the send is left to fail so the problem surfaces
	 * rather than being silently masked.
	 */
	private void removeUnusedCollaboratorRecipients(String envelopeId) {
		Envelope envelope = envelopesApi.getEnvelope(envelopeId);
		if (envelope.getRecipients() == null || envelope.getRecipients().getSigners() == null) {
			return;
		}
		List<Signer> unused = new ArrayList<>();
		for (Signer signer : envelope.getRecipients().getSigners()) {
			boolean isCollaborator = DocuSignTemplateValidator.collaboratorIndex(signer.getRoleName()) > 0;
			boolean hasNoEmail = signer.getEmail() == null || signer.getEmail().isBlank();
			if (isCollaborator && hasNoEmail) {
				Signer toRemove = new Signer();
				toRemove.setRecipientId(signer.getRecipientId());
				toRemove.setRoleName(signer.getRoleName());
				unused.add(toRemove);
			}
		}
		if (!unused.isEmpty()) {
			Recipients recipients = new Recipients();
			recipients.setSigners(unused);
			envelopesApi.deleteRecipients(envelopeId, recipients);
		}
	}

	static List<TemplateRole> buildTemplateRoles(Map<String, RecipientInfo> recipients,
			Map<RoleLabelKey, String> tabValues) {
		List<TemplateRole> roles = new ArrayList<>();
		for (Map.Entry<String, RecipientInfo> entry : recipients.entrySet()) {
			String roleName = entry.getKey();
			RecipientInfo recipient = entry.getValue();

			// DocuSign requires every recipient to have both an email and a name before the
			// envelope can be sent. The caller is responsible for supplying both.
			String email = recipient == null ? null : recipient.email();
			String name = recipient == null ? null : recipient.name();
			ValidateArgument.requiredNotBlank(email, "email for role '" + roleName + "'");
			ValidateArgument.requiredNotBlank(name, "name for role '" + roleName + "'");

			TemplateRole role = new TemplateRole();
			role.setRoleName(roleName);
			role.setEmail(email);
			role.setName(name);

			Tabs tabs = new Tabs();
			role.setTabs(tabs);
			// The recipient name comes from the RecipientInfo above; fill the tabs but ignore the
			// full-name tab value returned here so it does not override that name.
			fillTabsForRole(roleName, tabs, tabValues);

			roles.add(role);
		}
		return roles;
	}

	/**
	 * Populates the given {@code tabs} with the values for the given role. Any tab-level
	 * constraints (e.g. which tabs are locked/read-only for the signer) are defined on the
	 * template and carried forward by DocuSign, so they are not set here.
	 *
	 * @return the value of the role's FULL_NAME tab, if any, so the caller can also set it as the
	 *         recipient's name; null if the role has no FULL_NAME tab value.
	 */
	private static String fillTabsForRole(String roleName, Tabs tabs, Map<RoleLabelKey, String> tabValues) {
		String fullName = null;
		for (Map.Entry<RoleLabelKey, String> tabEntry : tabValues.entrySet()) {
			if (!tabEntry.getKey().roleName().equals(roleName)) {
				continue;
			}
			String tabLabel = tabEntry.getKey().tabLabel();
			String value = tabEntry.getValue();
			TabType type = DocuSignTemplateValidator.typeforRoleAndLabel(roleName, tabLabel);
			type.fillInTabValue(tabs, tabLabel, value);
			if (TabType.FULL_NAME.equals(type)) {
				fullName = value;
			}
		}
		return fullName;
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
				signerStatus.setDeclinedReason(signer.getDeclinedReason());
				signerStatus.setDeclinedOn(parseDate(signer.getDeclinedDateTime()));
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
