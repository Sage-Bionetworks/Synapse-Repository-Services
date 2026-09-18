package org.sagebionetworks.docusign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.PrefillTabs;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.Text;

class DocuSignTemplateValidator {

	// Named locally only to keep the declarations below readable; EDucTemplateRoles owns the values.
	private static final String SIGNING_OFFICIAL = EDucTemplateRoles.SIGNING_OFFICIAL;
	private static final String PRINCIPAL_INVESTIGATOR = EDucTemplateRoles.PRINCIPAL_INVESTIGATOR;

	/*
	 * The tab types each kind of field may be given. A template author picks among them for effect: a
	 * full name or email address tab is filled in from the recipient and cannot be edited by them, a
	 * text tab is filled in by Synapse but the signer may correct it, and a sender field is set only by
	 * Synapse — the one kind that also shows in the preview, because the others are not resolved until
	 * signing. A signature and its date are supplied by DocuSign itself, so they admit one type only.
	 *
	 * EnumSet is used for its ordinal iteration order, so that the types listed in a validation failure
	 * come out the same way every time.
	 */
	private static final Set<TabType> NAME_TYPES = unmodifiable(
			EnumSet.of(TabType.FULL_NAME, TabType.TEXT, TabType.PREFILL_TEXT));
	private static final Set<TabType> EMAIL_TYPES = unmodifiable(
			EnumSet.of(TabType.EMAIL_ADDRESS, TabType.TEXT, TabType.PREFILL_TEXT));
	private static final Set<TabType> USER_ENTERED_TEXT_TYPES = unmodifiable(
			EnumSet.of(TabType.TEXT, TabType.PREFILL_TEXT));
	private static final Set<TabType> SIGNATURE_TYPES = unmodifiable(EnumSet.of(TabType.SIGN_HERE));
	private static final Set<TabType> DATE_TYPES = unmodifiable(EnumSet.of(TabType.DATE_SIGNED));

	record RequiredTab(String label, Set<TabType> allowedTypes) {}

	static final List<RequiredTab> SIGNING_OFFICIAL_TABS = List.of(
			new RequiredTab(EDucTemplateRoles.institutionTab(SIGNING_OFFICIAL), USER_ENTERED_TEXT_TYPES),
			new RequiredTab(EDucTemplateRoles.nameTab(SIGNING_OFFICIAL), NAME_TYPES),
			new RequiredTab(EDucTemplateRoles.emailTab(SIGNING_OFFICIAL), EMAIL_TYPES),
			new RequiredTab(EDucTemplateRoles.signatureTab(SIGNING_OFFICIAL), SIGNATURE_TYPES),
			new RequiredTab(EDucTemplateRoles.dateTab(SIGNING_OFFICIAL), DATE_TYPES)
	);

	static final List<RequiredTab> PRINCIPAL_INVESTIGATOR_TABS = List.of(
			new RequiredTab(EDucTemplateRoles.nameTab(PRINCIPAL_INVESTIGATOR), NAME_TYPES),
			new RequiredTab(EDucTemplateRoles.emailTab(PRINCIPAL_INVESTIGATOR), EMAIL_TYPES),
			new RequiredTab(EDucTemplateRoles.userNameTab(PRINCIPAL_INVESTIGATOR), USER_ENTERED_TEXT_TYPES),
			new RequiredTab(EDucTemplateRoles.signatureTab(PRINCIPAL_INVESTIGATOR), SIGNATURE_TYPES),
			new RequiredTab(EDucTemplateRoles.dateTab(PRINCIPAL_INVESTIGATOR), DATE_TYPES)
	);

	static List<RequiredTab> requiredCollaboratorTabs(int index) {
		String role = EDucTemplateRoles.collaborator(index);
		return List.of(
				new RequiredTab(EDucTemplateRoles.userNameTab(role), USER_ENTERED_TEXT_TYPES),
				new RequiredTab(EDucTemplateRoles.nameTab(role), NAME_TYPES),
				new RequiredTab(EDucTemplateRoles.signatureTab(role), SIGNATURE_TYPES),
				new RequiredTab(EDucTemplateRoles.dateTab(role), DATE_TYPES)
		);
	}

	/**
	 * The tab types the given field of the given role is allowed to be given.
	 *
	 * @throws IllegalArgumentException if the role or the label is not one an eDUC template defines
	 */
	static Set<TabType> allowedTypesForRoleAndLabel(String roleName, String label) {
		List<RequiredTab> requiredTabsForRole = requiredTabsForRole(roleName);
		for (RequiredTab requiredTab : requiredTabsForRole) {
			if (requiredTab.label().equals(label)) {
				return requiredTab.allowedTypes();
			}
		}
		throw new IllegalArgumentException("Unexpected label for " + roleName + ": " + label);
	}

	private static List<RequiredTab> requiredTabsForRole(String roleName) {
		int collaboratorIndex;
		if (SIGNING_OFFICIAL.equals(roleName)) {
			return SIGNING_OFFICIAL_TABS;
		} else if (PRINCIPAL_INVESTIGATOR.equals(roleName)) {
			return PRINCIPAL_INVESTIGATOR_TABS;
		} else if ((collaboratorIndex = EDucTemplateRoles.collaboratorIndex(roleName)) > 0) {
			return requiredCollaboratorTabs(collaboratorIndex);
		} else {
			throw new IllegalArgumentException("Unexpected roleName " + roleName);
		}
	}

	/**
	 * Checks that the template carries every tab an eDUC needs, and reports which type it gave each of
	 * them.
	 *
	 * @param template     the template, with its recipients and their tabs populated
	 * @param documentTabs the tabs of each of the template's documents, which is where sender fields
	 *                     live — they belong to no recipient and so appear nowhere in {@code template}'s
	 *                     recipients
	 * @throws IllegalArgumentException if a required tab is missing, or is declared in more than one of
	 *                                  the types allowed for it
	 */
	static EDucTemplateLayout validate(EnvelopeTemplate template, List<Tabs> documentTabs) {
		Recipients recipients = template.getRecipients();
		if (recipients == null || recipients.getSigners() == null || recipients.getSigners().isEmpty()) {
			throw new IllegalArgumentException("Template has no signer roles defined.");
		}

		Map<String, Signer> signersByRole = recipients.getSigners().stream()
				.collect(Collectors.toMap(Signer::getRoleName, s -> s));

		List<Text> senderFieldDefinitions = mergeSenderFields(documentTabs);
		Tabs senderFields = asTabs(senderFieldDefinitions);
		Map<RoleLabelKey, TabType> resolved = new LinkedHashMap<>();

		validateRole(signersByRole, SIGNING_OFFICIAL, SIGNING_OFFICIAL_TABS, senderFields, resolved);
		validateRole(signersByRole, PRINCIPAL_INVESTIGATOR, PRINCIPAL_INVESTIGATOR_TABS, senderFields, resolved);
		validateCollaborators(signersByRole, senderFields, resolved);

		return new EDucTemplateLayout(resolved, senderFieldDefinitions);
	}

	/**
	 * Every sender field the template declares, gathered from all of its documents so that a label can be
	 * resolved without regard to which document declares it. Each tab keeps the document ID and placement
	 * it was authored with.
	 */
	private static List<Text> mergeSenderFields(List<Tabs> documentTabs) {
		List<Text> merged = new ArrayList<>();
		if (documentTabs != null) {
			for (Tabs tabs : documentTabs) {
				if (tabs == null || tabs.getPrefillTabs() == null || tabs.getPrefillTabs().getTextTabs() == null) {
					continue;
				}
				merged.addAll(tabs.getPrefillTabs().getTextTabs());
			}
		}
		return merged;
	}

	// Wrapped back up as Tabs so that TabType can look a label up in them the same way it does for a
	// signer's tabs, rather than this class reaching into the prefill container itself.
	private static Tabs asTabs(List<Text> senderFieldDefinitions) {
		PrefillTabs prefillTabs = new PrefillTabs();
		prefillTabs.setTextTabs(senderFieldDefinitions);
		Tabs tabs = new Tabs();
		tabs.setPrefillTabs(prefillTabs);
		return tabs;
	}

	private static void validateRole(Map<String, Signer> signersByRole, String roleName,
			List<RequiredTab> requiredTabs, Tabs senderFields, Map<RoleLabelKey, TabType> resolved) {
		Signer signer = signersByRole.get(roleName);
		if (signer == null) {
			throw new IllegalArgumentException("Template is missing required role: " + roleName);
		}
		validateTabs(roleName, signer.getTabs(), requiredTabs, senderFields, resolved);
	}

	private static void validateTabs(String roleName, Tabs recipientTabs, List<RequiredTab> requiredTabs,
			Tabs senderFields, Map<RoleLabelKey, TabType> resolved) {
		List<String> missing = new ArrayList<>();
		for (RequiredTab required : requiredTabs) {
			TabType type = resolveType(roleName, required, recipientTabs, senderFields);
			if (type == null) {
				missing.add(required.label() + " (one of " + required.allowedTypes() + ")");
			} else {
				resolved.put(new RoleLabelKey(roleName, required.label()), type);
			}
		}
		if (!missing.isEmpty()) {
			Collections.sort(missing);
			throw new IllegalArgumentException(
					"Role '" + roleName + "' is missing required tabs: " + missing);
		}
	}

	/**
	 * Which of the types allowed for a required tab the template actually gave it, or null if the
	 * template declares it under none of them.
	 * <p>
	 * A tab declared under two different allowed types leaves no single place to write the value, so it
	 * is rejected. Repeating a label within one type is left alone: that is how a template shows the
	 * same value in more than one place, and DocuSign fills all of them from the one value supplied.
	 */
	private static TabType resolveType(String roleName, RequiredTab required, Tabs recipientTabs,
			Tabs senderFields) {
		List<TabType> declaredAs = new ArrayList<>();
		for (TabType candidate : required.allowedTypes()) {
			// A sender field belongs to the document, so it is never found among a signer's tabs.
			Tabs declaredIn = TabType.PREFILL_TEXT == candidate ? senderFields : recipientTabs;
			if (declaredIn != null && candidate.hasTabWithLabel(declaredIn, required.label())) {
				declaredAs.add(candidate);
			}
		}
		if (declaredAs.isEmpty()) {
			return null;
		}
		if (declaredAs.size() > 1) {
			throw new IllegalArgumentException("Role '" + roleName + "' has tab '" + required.label()
					+ "' declared as more than one type: " + declaredAs
					+ ". Each tab must be declared as exactly one type.");
		}
		return declaredAs.get(0);
	}

	private static void validateCollaborators(Map<String, Signer> signersByRole, Tabs senderFields,
			Map<RoleLabelKey, TabType> resolved) {
		TreeMap<Integer, Signer> collaborators = new TreeMap<>();
		for (Map.Entry<String, Signer> entry : signersByRole.entrySet()) {
			int collaboratorIndex = EDucTemplateRoles.collaboratorIndex(entry.getKey());
			if (collaboratorIndex > 0) {
				collaborators.put(collaboratorIndex, entry.getValue());
			}
		}

		if (collaborators.isEmpty()) {
			return;
		}

		int maxIndex = collaborators.lastKey();
		if (maxIndex > EDucTemplateRoles.MAX_COLLABORATORS) {
			throw new IllegalArgumentException(
					"Collaborator index " + maxIndex + " exceeds maximum of " + EDucTemplateRoles.MAX_COLLABORATORS + ".");
		}

		for (int i = 1; i <= maxIndex; i++) {
			if (!collaborators.containsKey(i)) {
				throw new IllegalArgumentException(
						"Collaborator roles are not sequential: missing " + EDucTemplateRoles.collaborator(i) + ".");
			}
			Signer signer = collaborators.get(i);
			validateTabs(EDucTemplateRoles.collaborator(i), signer.getTabs(), requiredCollaboratorTabs(i),
					senderFields, resolved);
		}
	}

	private static Set<TabType> unmodifiable(EnumSet<TabType> types) {
		return Collections.unmodifiableSet(types);
	}
}
