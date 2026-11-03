package org.sagebionetworks.docusign;

import java.util.ArrayList;
import java.util.List;

import com.docusign.esign.model.DateSigned;
import com.docusign.esign.model.Document;
import com.docusign.esign.model.EmailAddress;
import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.FullName;
import com.docusign.esign.model.PrefillTabs;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.SignHere;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.Text;

class TestTemplateHelper {

	/** The one document the fixture templates carry, which is where a sender field would be placed. */
	static final String DOCUMENT_ID = "1";

	/**
	 * A template meeting the eDUC contract, giving every field the first of the types allowed for it.
	 * <p>
	 * Use {@link #useTabType} or {@link #useSenderField} to move a field onto one of the other types it
	 * is allowed, which is what a template author is free to do.
	 */
	static EnvelopeTemplate buildValidTemplate(int numCollaborators) {
		EnvelopeTemplate template = new EnvelopeTemplate();
		Recipients recipients = new Recipients();
		List<Signer> signers = new ArrayList<>();
		signers.add(buildSigningOfficialSigner());
		signers.add(buildPrincipalInvestigatorSigner());
		for (int i = 1; i <= numCollaborators; i++) {
			signers.add(buildCollaboratorSigner(i));
		}
		recipients.setSigners(signers);
		template.setRecipients(recipients);
		Document document = new Document();
		document.setDocumentId(DOCUMENT_ID);
		template.setDocuments(new ArrayList<>(List.of(document)));
		return template;
	}

	static Signer buildSigningOfficialSigner() {
		Signer signer = new Signer();
		signer.setRoleName("signing_official");
		Tabs tabs = new Tabs();
		FullName name = new FullName();
		name.setTabLabel("signing_official_name");
		tabs.setFullNameTabs(mutable(name));
		Text institution = new Text();
		institution.setTabLabel("signing_official_institution");
		tabs.setTextTabs(mutable(institution));
		EmailAddress email = new EmailAddress();
		email.setTabLabel("signing_official_email");
		tabs.setEmailAddressTabs(mutable(email));
		SignHere sig = new SignHere();
		sig.setTabLabel("signing_official_signature");
		tabs.setSignHereTabs(mutable(sig));
		DateSigned date = new DateSigned();
		date.setTabLabel("signing_official_date");
		tabs.setDateSignedTabs(mutable(date));
		signer.setTabs(tabs);
		return signer;
	}

	static Signer buildPrincipalInvestigatorSigner() {
		Signer signer = new Signer();
		signer.setRoleName("principal_investigator");
		Tabs tabs = new Tabs();
		Text userName = new Text();
		userName.setTabLabel("principal_investigator_user_name");
		tabs.setTextTabs(mutable(userName));
		FullName name = new FullName();
		name.setTabLabel("principal_investigator_name");
		tabs.setFullNameTabs(mutable(name));
		EmailAddress email = new EmailAddress();
		email.setTabLabel("principal_investigator_email");
		tabs.setEmailAddressTabs(mutable(email));
		SignHere sig = new SignHere();
		sig.setTabLabel("principal_investigator_signature");
		tabs.setSignHereTabs(mutable(sig));
		DateSigned date = new DateSigned();
		date.setTabLabel("principal_investigator_date");
		tabs.setDateSignedTabs(mutable(date));
		signer.setTabs(tabs);
		return signer;
	}

	static Signer buildCollaboratorSigner(int index) {
		Signer signer = new Signer();
		signer.setRoleName("collaborator_" + index);
		String prefix = "collaborator_" + index + "_";
		Tabs tabs = new Tabs();
		Text userName = new Text();
		userName.setTabLabel(prefix + "user_name");
		tabs.setTextTabs(mutable(userName));
		FullName name = new FullName();
		name.setTabLabel(prefix + "name");
		tabs.setFullNameTabs(mutable(name));
		SignHere sig = new SignHere();
		sig.setTabLabel(prefix + "signature");
		tabs.setSignHereTabs(mutable(sig));
		DateSigned date = new DateSigned();
		date.setTabLabel(prefix + "date");
		tabs.setDateSignedTabs(mutable(date));
		signer.setTabs(tabs);
		return signer;
	}

	/**
	 * Re-declares one of a role's tabs under a different type, as a template author choosing among the
	 * types allowed for that field would.
	 */
	static void useTabType(EnvelopeTemplate template, String roleName, String tabLabel, TabType type) {
		Tabs tabs = findSigner(template, roleName).getTabs();
		removeTabWithLabel(tabs, tabLabel);
		// A template declares its tabs without values; the signer's or Synapse's value arrives later.
		type.addTabWithLabel(tabs, tabLabel, null);
	}

	/**
	 * Moves one of a role's tabs off the role and onto the template's document as a sender field, adding
	 * it to the given document tabs. Sender fields belong to no recipient, which is why they are returned
	 * separately rather than appearing anywhere on the template's recipients.
	 */
	static void useSenderField(EnvelopeTemplate template, String roleName, String tabLabel, Tabs documentTabs) {
		removeTabWithLabel(findSigner(template, roleName).getTabs(), tabLabel);
		addSenderField(documentTabs, tabLabel);
	}

	/** Document tabs declaring the given label as a sender field, placed as a template would place it. */
	static void addSenderField(Tabs documentTabs, String tabLabel) {
		Text tab = new Text();
		tab.setTabLabel(tabLabel);
		tab.setDocumentId(DOCUMENT_ID);
		tab.setPageNumber("1");
		tab.setXPosition("100");
		tab.setYPosition("200");
		tab.setTabId("template-tab-" + tabLabel);
		documentTabs.getPrefillTabs().addTextTabsItem(tab);
	}

	static Tabs emptyDocumentTabs() {
		Tabs tabs = new Tabs();
		tabs.setPrefillTabs(new PrefillTabs());
		return tabs;
	}

	static Signer findSigner(EnvelopeTemplate template, String roleName) {
		return template.getRecipients().getSigners().stream()
				.filter(s -> roleName.equals(s.getRoleName()))
				.findFirst()
				.orElseThrow();
	}

	// Clears the label from every type that can carry a value, so that the caller can re-declare it under
	// one of the others without leaving the template declaring it twice.
	private static void removeTabWithLabel(Tabs tabs, String tabLabel) {
		removeFrom(tabs.getTextTabs(), tabLabel);
		removeFrom(tabs.getFullNameTabs(), tabLabel);
		removeFrom(tabs.getEmailAddressTabs(), tabLabel);
	}

	private static void removeFrom(List<?> tabsOfOneType, String tabLabel) {
		if (tabsOfOneType == null) {
			return;
		}
		tabsOfOneType.removeIf(tab -> tabLabel.equals(labelOf(tab)));
	}

	// The tab types are unrelated classes with no common supertype, so the label is read reflectively
	// rather than by enumerating them here.
	private static String labelOf(Object tab) {
		try {
			return (String) tab.getClass().getMethod("getTabLabel").invoke(tab);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to read the label of a " + tab.getClass().getSimpleName(), e);
		}
	}

	@SafeVarargs
	private static <T> List<T> mutable(T... tabs) {
		return new ArrayList<>(List.of(tabs));
	}
}
