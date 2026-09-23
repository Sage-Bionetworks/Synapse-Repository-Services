package org.sagebionetworks.docusign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.docusign.esign.model.DateSigned;
import com.docusign.esign.model.EmailAddress;
import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.FullName;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.SignHere;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.Text;

class TestTemplateHelper {

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
		return template;
	}

	static Signer buildSigningOfficialSigner() {
		Signer signer = new Signer();
		signer.setRoleName("signing_official");
		Tabs tabs = new Tabs();
		FullName name = new FullName();
		name.setTabLabel("signing_official_name");
		tabs.setFullNameTabs(List.of(name));
		Text institution = new Text();
		institution.setTabLabel("signing_official_institution");
		tabs.setTextTabs(List.of(institution));
		EmailAddress email = new EmailAddress();
		email.setTabLabel("signing_official_email");
		tabs.setEmailAddressTabs(List.of(email));
		SignHere sig = new SignHere();
		sig.setTabLabel("signing_official_signature");
		tabs.setSignHereTabs(List.of(sig));
		DateSigned date = new DateSigned();
		date.setTabLabel("signing_official_date");
		tabs.setDateSignedTabs(List.of(date));
		signer.setTabs(tabs);
		return signer;
	}

	static Signer buildPrincipalInvestigatorSigner() {
		Signer signer = new Signer();
		signer.setRoleName("principal_investigator");
		Tabs tabs = new Tabs();
		Text userName = new Text();
		userName.setTabLabel("principal_investigator_user_name");
		tabs.setTextTabs(List.of(userName));
		FullName name = new FullName();
		name.setTabLabel("principal_investigator_name");
		tabs.setFullNameTabs(List.of(name));
		EmailAddress email = new EmailAddress();
		email.setTabLabel("principal_investigator_email");
		tabs.setEmailAddressTabs(List.of(email));
		SignHere sig = new SignHere();
		sig.setTabLabel("principal_investigator_signature");
		tabs.setSignHereTabs(List.of(sig));
		DateSigned date = new DateSigned();
		date.setTabLabel("principal_investigator_date");
		tabs.setDateSignedTabs(List.of(date));
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
		tabs.setTextTabs(List.of(userName));
		FullName name = new FullName();
		name.setTabLabel(prefix + "name");
		tabs.setFullNameTabs(List.of(name));
		SignHere sig = new SignHere();
		sig.setTabLabel(prefix + "signature");
		tabs.setSignHereTabs(List.of(sig));
		DateSigned date = new DateSigned();
		date.setTabLabel(prefix + "date");
		tabs.setDateSignedTabs(List.of(date));
		signer.setTabs(tabs);
		return signer;
	}

	static Signer findSigner(EnvelopeTemplate template, String roleName) {
		return template.getRecipients().getSigners().stream()
				.filter(s -> roleName.equals(s.getRoleName()))
				.findFirst()
				.orElseThrow();
	}
}
