package org.sagebionetworks.docusign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;

class DocuSignTemplateValidator {

	// Named locally only to keep the declarations below readable; EDucTemplateRoles owns the values.
	private static final String SIGNING_OFFICIAL = EDucTemplateRoles.SIGNING_OFFICIAL;
	private static final String PRINCIPAL_INVESTIGATOR = EDucTemplateRoles.PRINCIPAL_INVESTIGATOR;

	record RequiredTab(String label, TabType type) {}

	static final List<RequiredTab> SIGNING_OFFICIAL_TABS = List.of(
			new RequiredTab(EDucTemplateRoles.institutionTab(SIGNING_OFFICIAL), TabType.TEXT),
			new RequiredTab(EDucTemplateRoles.nameTab(SIGNING_OFFICIAL), TabType.FULL_NAME),
			new RequiredTab(EDucTemplateRoles.emailTab(SIGNING_OFFICIAL), TabType.EMAIL_ADDRESS),
			new RequiredTab(EDucTemplateRoles.signatureTab(SIGNING_OFFICIAL), TabType.SIGN_HERE),
			new RequiredTab(EDucTemplateRoles.dateTab(SIGNING_OFFICIAL), TabType.DATE_SIGNED)
	);

	static final List<RequiredTab> PRINCIPAL_INVESTIGATOR_TABS = List.of(
			new RequiredTab(EDucTemplateRoles.nameTab(PRINCIPAL_INVESTIGATOR), TabType.FULL_NAME),
			new RequiredTab(EDucTemplateRoles.emailTab(PRINCIPAL_INVESTIGATOR), TabType.EMAIL_ADDRESS),
			new RequiredTab(EDucTemplateRoles.userNameTab(PRINCIPAL_INVESTIGATOR), TabType.TEXT),
			new RequiredTab(EDucTemplateRoles.signatureTab(PRINCIPAL_INVESTIGATOR), TabType.SIGN_HERE),
			new RequiredTab(EDucTemplateRoles.dateTab(PRINCIPAL_INVESTIGATOR), TabType.DATE_SIGNED)
	);
	
	static TabType typeforRoleAndLabel(String roleName, String label) {
		int collaboratorIndex;
		List<RequiredTab> requiredTabsForRole;
		if (SIGNING_OFFICIAL.equals(roleName)) {
			requiredTabsForRole = SIGNING_OFFICIAL_TABS;
		} else if (PRINCIPAL_INVESTIGATOR.equals(roleName)) {
			requiredTabsForRole = PRINCIPAL_INVESTIGATOR_TABS;
		} else if ((collaboratorIndex=EDucTemplateRoles.collaboratorIndex(roleName))>0) {
			requiredTabsForRole =requiredCollaboratorTabs(collaboratorIndex);
		} else {
			throw new IllegalArgumentException("Unexpected roleName "+roleName);
		}
		for (RequiredTab requiredTab : requiredTabsForRole) {
			if (requiredTab.label.equals(label)) {
				return requiredTab.type;
			}
		}
		throw new IllegalArgumentException("Unexpected label for "+roleName+": "+label);
	}

	static void validate(EnvelopeTemplate template) {
		Recipients recipients = template.getRecipients();
		if (recipients == null || recipients.getSigners() == null || recipients.getSigners().isEmpty()) {
			throw new IllegalArgumentException("Template has no signer roles defined.");
		}

		Map<String, Signer> signersByRole = recipients.getSigners().stream()
				.collect(Collectors.toMap(Signer::getRoleName, s -> s));

		validateRole(signersByRole, SIGNING_OFFICIAL, SIGNING_OFFICIAL_TABS);
		validateRole(signersByRole, PRINCIPAL_INVESTIGATOR, PRINCIPAL_INVESTIGATOR_TABS);
		validateCollaborators(signersByRole);
	}

	private static void validateRole(Map<String, Signer> signersByRole, String roleName,
			List<RequiredTab> requiredTabs) {
		Signer signer = signersByRole.get(roleName);
		if (signer == null) {
			throw new IllegalArgumentException("Template is missing required role: " + roleName);
		}
		validateTabs(roleName, signer.getTabs(), requiredTabs);
	}

	private static void validateTabs(String roleName, Tabs tabs, List<RequiredTab> requiredTabs) {
		List<String> missing = new ArrayList<>();
		for (RequiredTab required : requiredTabs) {
			if (!required.type.hasTabWithLabel(tabs, required.label())) {
				missing.add(required.label() + " (" + required.type().name() + ")");
			}
		}
		if (!missing.isEmpty()) {
			Collections.sort(missing);
			throw new IllegalArgumentException(
					"Role '" + roleName + "' is missing required tabs: " + missing);
		}
	}


	private static void validateCollaborators(Map<String, Signer> signersByRole) {
		TreeMap<Integer, Signer> collaborators = new TreeMap<>();
		for (Map.Entry<String, Signer> entry : signersByRole.entrySet()) {
			int collaboratorIndex = EDucTemplateRoles.collaboratorIndex(entry.getKey());
			if (collaboratorIndex>0) {
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
			validateTabs(EDucTemplateRoles.collaborator(i), signer.getTabs(), requiredCollaboratorTabs(i));
		}
	}

	static List<RequiredTab> requiredCollaboratorTabs(int index) {
		String role = EDucTemplateRoles.collaborator(index);
		return List.of(
				new RequiredTab(EDucTemplateRoles.userNameTab(role), TabType.TEXT),
				new RequiredTab(EDucTemplateRoles.nameTab(role), TabType.FULL_NAME),
				new RequiredTab(EDucTemplateRoles.signatureTab(role), TabType.SIGN_HERE),
				new RequiredTab(EDucTemplateRoles.dateTab(role), TabType.DATE_SIGNED)
		);
	}
}
