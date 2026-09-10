package org.sagebionetworks.docusign;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
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
import com.docusign.esign.model.TemplateInformation;
import com.docusign.esign.model.TemplateRole;
import com.docusign.esign.model.TemplateSummary;

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
	 * The signers currently on the given envelope, in the order DocuSign reports them. Callers use
	 * this to work out which template role each person already occupies before deciding how to
	 * correct the envelope.
	 */
	public List<EnvelopeRecipient> getRecipients(String envelopeId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		Envelope envelope = envelopesApi.getEnvelope(envelopeId);
		if (envelope.getRecipients() == null || envelope.getRecipients().getSigners() == null) {
			return List.of();
		}
		List<EnvelopeRecipient> recipients = new ArrayList<>();
		for (Signer signer : envelope.getRecipients().getSigners()) {
			recipients.add(new EnvelopeRecipient(signer.getRoleName(), signer.getEmail(), isCompleted(signer)));
		}
		return recipients;
	}

	/**
	 * Applies new signer emails and tab values to an in-flight (sent or delivered) envelope by adding,
	 * updating and removing its recipients to match the desired content. Recipients who have already
	 * signed are left untouched, and those added or updated are notified.
	 * <p>
	 * The envelope is not moved into DocuSign's "correct" state to do this. That state belongs to the
	 * web console's correction session and can only be entered by the owner of an edit lock, so setting
	 * it here is refused with {@code EDIT_LOCK_NOT_LOCK_OWNER}; the recipient endpoints modify a sent
	 * envelope directly and need no lock.
	 *
	 * @param envelopeId the ID of the envelope to correct
	 * @param recipients map from role name to the signer's desired email and name (both are
	 *        required for every role, as they are when creating an envelope)
	 * @param tabValues map from (roleName, tabLabel) to the desired text value to pre-fill
	 */
	public void correctEnvelope(String envelopeId, Map<String, RecipientInfo> recipients,
			Map<RoleLabelKey, String> tabValues) {
		ValidateArgument.required(envelopeId, "envelopeId");
		ValidateArgument.required(recipients, "recipients");
		ValidateArgument.required(tabValues, "tabValues");

		// Fetch the current recipients to obtain their recipientIds and per-signer status.
		Envelope existing = envelopesApi.getEnvelope(envelopeId);
		List<Signer> existingSigners = existing.getRecipients() == null ? List.of()
				: existing.getRecipients().getSigners();
		if (existingSigners == null) {
			existingSigners = List.of();
		}

		// Every change is computed before the envelope is touched, so that a failure to build one
		// leaves the envelope as it was rather than partially corrected.

		Recipients toDelete = buildRemovedRecipients(existingSigners, recipients);
		Recipients toUpdate = buildUpdatedRecipients(existingSigners, recipients, tabValues);
		
		// Now handle new recipients, not already in the envelope
		// In contrast to sending a new envelope, here we don't just add tab values,
		// but we add the entire recipient (a Signer object), including the tab definition (including where
		// it's placed in the document)
		Recipients toCreate = buildNewRecipients(existingSigners, () -> templateSignersByRole(envelopeId),
				recipients, tabValues);

		// Each call resends to the recipients it touches, so no envelope-level re-send is needed.
		if (hasSigners(toDelete)) {
			envelopesApi.deleteRecipients(envelopeId, toDelete);
		}
		if (hasSigners(toUpdate)) {
			envelopesApi.updateRecipients(envelopeId, toUpdate, true);
		}
		if (hasSigners(toCreate)) {
			envelopesApi.createRecipients(envelopeId, toCreate, true);
			// Adding a recipient ignores the tabs nested in it — it arrives with none — so each one's
			// tabs are created in a second request. They are left on the signers above as the record of
			// what belongs to whom.
			for (Signer created : toCreate.getSigners()) {
				if (created.getTabs() != null) {
					envelopesApi.createTabs(envelopeId, created.getRecipientId(), created.getTabs());
				}
			}
		}
	}

	// Existing (not-yet-signed) signers whose role is no longer desired are removed.
	static Recipients buildRemovedRecipients(List<Signer> existingSigners,
			Map<String, RecipientInfo> recipients) {
		List<Signer> removed = new ArrayList<>();
		for (Signer existing : existingSigners) {
			if (isCompleted(existing)) {
				continue;
			}
			if (!recipients.containsKey(existing.getRoleName())) {
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
			Map<String, RecipientInfo> recipients, Map<RoleLabelKey, String> tabValues) {
		List<Signer> updated = new ArrayList<>();
		for (Signer existing : existingSigners) {
			if (isCompleted(existing)) {
				continue;
			}
			String roleName = existing.getRoleName();
			if (!recipients.containsKey(roleName)) {
				continue;
			}
			RecipientInfo recipient = requireRecipient(recipients, roleName);
			Signer signer = new Signer();
			signer.setRecipientId(existing.getRecipientId());
			signer.setRoleName(roleName);
			signer.setEmail(recipient.email());
			signer.setName(recipient.name());
			Tabs tabs = new Tabs();
			signer.setTabs(tabs);
			fillTabsForRole(roleName, tabs, tabValues);
			updated.add(signer);
		}
		return toRecipients(updated);
	}

	// Used when a template role carries no routing order of its own. Only collaborators are ever
	// added (the principal investigator and signing official are always present), and a template
	// routes the collaborators together with the principal investigator and ahead of the signing
	// official, so the earliest possible order preserves that intent.
	private static final String EARLIEST_ROUTING_ORDER = "1";

	/**
	 * The signer roles, by role name, of the template the given envelope was created from. Deleting a
	 * recipient from an envelope removes its tabs and routing order along with it, so for a recipient
	 * that has to be added back the template is the only remaining source of those definitions.
	 * <p>
	 * The template is identified by asking the envelope rather than by taking an ID from the caller,
	 * so that a template cloned and repointed since the envelope was routed cannot be read in place of
	 * the one the envelope's other recipients were actually placed from.
	 */
	private Map<String, Signer> templateSignersByRole(String envelopeId) {
		EnvelopeTemplate template = templatesApi.getTemplate(templateIdOf(envelopeId));
		// Guarantees the roles and their tabs are present and well-formed before they are copied.
		DocuSignTemplateValidator.validate(template);
		Map<String, Signer> signersByRole = new HashMap<>();
		for (Signer signer : template.getRecipients().getSigners()) {
			signersByRole.put(signer.getRoleName(), signer);
		}
		return signersByRole;
	}

	/**
	 * Builds the desired roles that are not present on the envelope as new recipients, taking their
	 * tab definitions and routing order from the template role of the same name. In contrast to
	 * sending a new envelope, this does not just supply tab values: it adds the whole recipient,
	 * including each tab's definition and where it is placed in the document.
	 *
	 * @param templateSigners the template's signer roles, resolved only if a role actually has to be
	 *        added; most corrections add none, and reading the template costs a request to DocuSign
	 */
	static Recipients buildNewRecipients(List<Signer> existingSigners,
			Supplier<Map<String, Signer>> templateSigners, Map<String, RecipientInfo> recipients,
			Map<RoleLabelKey, String> tabValues) {
		int maxRecipientId = 0;
		for (Signer existing : existingSigners) {
			maxRecipientId = Math.max(maxRecipientId, parseRecipientId(existing.getRecipientId()));
		}

		List<Signer> created = new ArrayList<>();
		int nextRecipientId = maxRecipientId;
		Map<String, Signer> templateSignersByRole = null;
		for (String roleName : rolesToAdd(existingSigners, recipients)) {
			if (templateSignersByRole == null) {
				templateSignersByRole = templateSigners.get();
			}
			Signer templateSigner = templateSignersByRole.get(roleName);
			if (templateSigner == null) {
				throw new IllegalArgumentException("The template does not define the role '" + roleName + "'.");
			}
			RecipientInfo recipient = requireRecipient(recipients, roleName);
			// The recipient ID only has to be unique within the envelope. The routing order is a
			// separate concept and comes from the template, so it is not derived from it.
			nextRecipientId++;
			String recipientId = Integer.toString(nextRecipientId);
			Signer signer = new Signer();
			signer.setRecipientId(recipientId);
			signer.setRoutingOrder(routingOrderOf(templateSigner));
			signer.setRoleName(roleName);
			signer.setEmail(recipient.email());
			signer.setName(recipient.name());
			signer.setTabs(buildTabsFromTemplate(roleName, recipientId, templateSigner, tabValues));
			created.add(signer);
		}
		return toRecipients(created);
	}

	/**
	 * The ID of the single template the given envelope was created from. An envelope does not carry
	 * its template's ID as a field, so DocuSign is asked which templates were applied to it.
	 *
	 * @throws IllegalStateException if the envelope does not report exactly one template, rather than
	 *         risk placing a recipient's tabs from a template the envelope was not built from
	 */
	private String templateIdOf(String envelopeId) {
		TemplateInformation templateInformation = envelopesApi.listTemplates(envelopeId);
		List<TemplateSummary> templates = templateInformation == null ? null
				: templateInformation.getTemplates();
		if (templates == null || templates.size() != 1) {
			throw new IllegalStateException("Expected envelope " + envelopeId
					+ " to have been created from exactly one template but found "
					+ (templates == null ? 0 : templates.size()) + ".");
		}
		return templates.get(0).getTemplateId();
	}

	// The desired roles that the envelope does not currently have. Iterates in the order the given
	// recipients map does, so that recipient IDs are assigned deterministically.
	private static Set<String> rolesToAdd(List<Signer> existingSigners,
			Map<String, RecipientInfo> recipients) {
		Set<String> existingRoles = new HashSet<>();
		for (Signer existing : existingSigners) {
			existingRoles.add(existing.getRoleName());
		}
		Set<String> toAdd = new LinkedHashSet<>(recipients.keySet());
		toAdd.removeAll(existingRoles);
		return toAdd;
	}

	private static String routingOrderOf(Signer templateSigner) {
		String routingOrder = templateSigner.getRoutingOrder();
		return StringUtils.isBlank(routingOrder) ? EARLIEST_ROUTING_ORDER : routingOrder;
	}

	/**
	 * Builds the tabs of a recipient being added to an in-flight envelope from the template role's
	 * tab definitions, pointed at the given recipient of that envelope and with the desired values
	 * applied to them.
	 */
	private static Tabs buildTabsFromTemplate(String roleName, String recipientId, Signer templateSigner,
			Map<RoleLabelKey, String> tabValues) {
		// A tab only appears on the document if it carries a placement (a document, a page and
		// coordinates or an anchor), which a tab built from a label and a value alone does not have.
		// Reusing the template's definitions brings across every tab of the role, not only those this
		// code supplies values for: the signature and date tabs, which never carry a value, and any
		// tab of a type the template author chose that this code knows nothing about. The template is
		// read afresh for each correction, so its definitions can be modified in place.
		Tabs tabs = templateSigner.getTabs();
		if (tabs == null) {
			return new Tabs();
		}
		TabIdentifiers.assignToRecipient(tabs, recipientId);
		for (Map.Entry<RoleLabelKey, String> tabEntry : tabValues.entrySet()) {
			if (!tabEntry.getKey().roleName().equals(roleName)) {
				continue;
			}
			String tabLabel = tabEntry.getKey().tabLabel();
			TabType type = DocuSignTemplateValidator.typeforRoleAndLabel(roleName, tabLabel);
			type.applyValueToTabWithLabel(tabs, tabLabel, tabEntry.getValue());
		}
		return tabs;
	}

	/**
	 * The recipient desired for the given role. DocuSign requires every recipient to have both an
	 * email and a name before an envelope can be sent, so an incomplete role is rejected here, while
	 * the changes are still being assembled, rather than by DocuSign once some have been applied.
	 */
	private static RecipientInfo requireRecipient(Map<String, RecipientInfo> recipients, String roleName) {
		RecipientInfo recipient = recipients.get(roleName);
		String email = recipient == null ? null : recipient.email();
		String name = recipient == null ? null : recipient.name();
		ValidateArgument.requiredNotBlank(email, "email for role '" + roleName + "'");
		ValidateArgument.requiredNotBlank(name, "name for role '" + roleName + "'");
		return recipient;
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
			boolean isCollaborator = EDucTemplateRoles.isCollaborator(signer.getRoleName());
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

	/*
	 * This is used when populating a new envelope with recipients, including their tab
	 * values.  Docusign uses the TemplateRole list to match these recipients against the
	 * template and to fill in the relevant information.
	 */
	static List<TemplateRole> buildTemplateRoles(Map<String, RecipientInfo> recipients,
			Map<RoleLabelKey, String> tabValues) {
		List<TemplateRole> roles = new ArrayList<>();
		for (String roleName : recipients.keySet()) {
			RecipientInfo recipient = requireRecipient(recipients, roleName);

			TemplateRole role = new TemplateRole();
			role.setRoleName(roleName);
			role.setEmail(recipient.email());
			role.setName(recipient.name());

			Tabs tabs = new Tabs();
			role.setTabs(tabs);
			// here we create the tab values which will be matched against the tabs in the template
			fillTabsForRole(roleName, tabs, tabValues);

			roles.add(role);
		}
		return roles;
	}

	/**
	 * Populates the given {@code tabs} with the values for the given role. Any tab-level
	 * constraints (e.g. which tabs are locked/read-only for the signer) are defined on the
	 * template and carried forward by DocuSign, so they are not set here.
	 */
	private static void fillTabsForRole(String roleName, Tabs tabs, Map<RoleLabelKey, String> tabValues) {
		for (Map.Entry<RoleLabelKey, String> tabEntry : tabValues.entrySet()) {
			if (!tabEntry.getKey().roleName().equals(roleName)) {
				continue;
			}
			String tabLabel = tabEntry.getKey().tabLabel();
			TabType type = DocuSignTemplateValidator.typeforRoleAndLabel(roleName, tabLabel);
			type.addTabWithLabel(tabs, tabLabel, tabEntry.getValue());
		}
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
