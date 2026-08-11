package org.sagebionetworks.docusign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;

class DocuSignTemplateValidator {

	private static final String SIGNING_OFFICIAL = "signing_official";
	private static final String PRINCIPAL_INVESTIGATOR = "principal_investigator";
	private static final Pattern COLLABORATOR_PATTERN = Pattern.compile("collaborator_(\\d+)");
	private static final int MAX_COLLABORATORS = 98;

	record RequiredTab(String label, TabType type) {}

	static final List<RequiredTab> SIGNING_OFFICIAL_TABS = List.of(
			new RequiredTab("signing_official_name", TabType.FULL_NAME),
			new RequiredTab("signing_official_title", TabType.TITLE),
			new RequiredTab("signing_official_email", TabType.EMAIL_ADDRESS),
			new RequiredTab("signing_official_signature", TabType.SIGN_HERE),
			new RequiredTab("signing_official_date", TabType.DATE_SIGNED)
	);

	static final List<RequiredTab> PRINCIPAL_INVESTIGATOR_TABS = List.of(
			new RequiredTab("principal_investigator_institution", TabType.TEXT),
			new RequiredTab("principal_investigator_name", TabType.FULL_NAME),
			new RequiredTab("principal_investigator_title", TabType.TITLE),
			new RequiredTab("principal_investigator_email", TabType.EMAIL_ADDRESS),
			new RequiredTab("principal_investigator_user_name", TabType.TEXT),
			new RequiredTab("principal_investigator_signature", TabType.SIGN_HERE),
			new RequiredTab("principal_investigator_date", TabType.DATE_SIGNED)
	);
	
	static TabType typeforRoleAndLabel(String roleName, String label) {
		int collaboratorIndex;
		List<RequiredTab> requiredTabsForRole;
		if (SIGNING_OFFICIAL.equals(roleName)) {
			requiredTabsForRole = SIGNING_OFFICIAL_TABS;
		} else if (PRINCIPAL_INVESTIGATOR.equals(roleName)) {
			requiredTabsForRole = PRINCIPAL_INVESTIGATOR_TABS;
		} else if ((collaboratorIndex=collaboratorIndex(roleName))>0) {
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

	
	/**
	 * 
	 * @param roleName role name of the collaborator
	 * @return the index of the collaborator or -1 if not a collaborator
	 */
	static int collaboratorIndex(String roleName) {
		Matcher matcher = COLLABORATOR_PATTERN.matcher(roleName);
		if (matcher.matches()) {
			return Integer.parseInt(matcher.group(1));
		} else {
			return -1;
		}
	}

	private static void validateCollaborators(Map<String, Signer> signersByRole) {
		TreeMap<Integer, Signer> collaborators = new TreeMap<>();
		for (Map.Entry<String, Signer> entry : signersByRole.entrySet()) {
			int collaboratorIndex = collaboratorIndex(entry.getKey());
			if (collaboratorIndex>0) {
				collaborators.put(collaboratorIndex, entry.getValue());
			}
		}

		if (collaborators.isEmpty()) {
			return;
		}

		int maxIndex = collaborators.lastKey();
		if (maxIndex > MAX_COLLABORATORS) {
			throw new IllegalArgumentException(
					"Collaborator index " + maxIndex + " exceeds maximum of " + MAX_COLLABORATORS + ".");
		}

		for (int i = 1; i <= maxIndex; i++) {
			if (!collaborators.containsKey(i)) {
				throw new IllegalArgumentException(
						"Collaborator roles are not sequential: missing collaborator_" + i + ".");
			}
			Signer signer = collaborators.get(i);
			validateTabs("collaborator_" + i, signer.getTabs(), requiredCollaboratorTabs(i));
		}
	}

	static List<RequiredTab> requiredCollaboratorTabs(int index) {
		String prefix = "collaborator_" + index + "_";
		return List.of(
				new RequiredTab(prefix + "user_name", TabType.TEXT),
				new RequiredTab(prefix + "name", TabType.FULL_NAME),
				new RequiredTab(prefix + "signature", TabType.SIGN_HERE),
				new RequiredTab(prefix + "date", TabType.DATE_SIGNED)
		);
	}
}
