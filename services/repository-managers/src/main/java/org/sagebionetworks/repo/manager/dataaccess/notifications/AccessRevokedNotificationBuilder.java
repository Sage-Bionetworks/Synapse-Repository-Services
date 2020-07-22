package org.sagebionetworks.repo.manager.dataaccess.notifications;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessRevokedNotificationBuilder implements DataAccessNotificationBuilder {
	
	private static final String TEMPLATE_FILE = "message/AccessApprovalRevokedTemplate.html";
	private static final String SUBJECT_TEMPLATE = "%s Access Revoked";
	private static final String REQUIREMENT_URL_TEMPLATE = "https://www.synapse.org/#!AccessRequirement:AR_ID=%s&TYPE=ENTITY&ID=syn%s";
	
	private static final String PARAM_DISPLAY_NAME = "#displayName#";
	private static final String PARAM_REQUIREMENT_DESCRIPTION = "#requirementDescription#";
	private static final String PARAM_REQUIREMENT_URL = "#requirementUrl#";
	
	private UserProfileManager userProfileManager;
	
	@Autowired
	public AccessRevokedNotificationBuilder(final UserProfileManager userProfileManager) {
		this.userProfileManager = userProfileManager;
	}

	@Override
	public List<DataAccessNotificationType> supportedTypes() {
		return Collections.singletonList(DataAccessNotificationType.REVOCATION);
	}

	@Override
	public String buildSubject(ManagedACTAccessRequirement accessRequirement, AccessApproval approval, UserInfo recipient) {
		String prefix = "Data";
		
		if (!StringUtils.isBlank(accessRequirement.getDescription())) {
			prefix = accessRequirement.getDescription();
		}
		
		return String.format(SUBJECT_TEMPLATE, prefix);
	}

	@Override
	public String buildMessageBody(ManagedACTAccessRequirement accessRequirement, AccessApproval approval, UserInfo recipient) {
		final Map<String, String> templateValues = new HashMap<>();
		
		if (accessRequirement.getSubjectIds() == null || accessRequirement.getSubjectIds().isEmpty()) {
			throw new IllegalStateException("The access requirement with id " + accessRequirement.getId() + " does not reference any subject.");
		}
		
		// We need an entity to reference the AR in the web client, since we cannot back to the original submission from an access approval
		// just take the first on the list
		final String referenceEntityId = accessRequirement.getSubjectIds().get(0).getId();
		
		final String requirementUrl = String.format(REQUIREMENT_URL_TEMPLATE, accessRequirement.getId(), referenceEntityId);
		
		templateValues.put(PARAM_REQUIREMENT_URL, requirementUrl);

		String description = accessRequirement.getDescription();
		
		StringBuilder descriptionBuidler = new StringBuilder();
		
		if (StringUtils.isBlank(description)) {
			descriptionBuidler.append("an <a href=\"");
			descriptionBuidler.append(requirementUrl);
			descriptionBuidler.append("\">");
		} else {
			descriptionBuidler.append("the <a href=\"");
			descriptionBuidler.append(requirementUrl);
			descriptionBuidler.append("\">");
			descriptionBuidler.append(description);
			descriptionBuidler.append(" ");
		}
		
		descriptionBuidler.append("access requirement (");
		descriptionBuidler.append(accessRequirement.getId());
		descriptionBuidler.append(")</a>");
		
		templateValues.put(PARAM_REQUIREMENT_DESCRIPTION, descriptionBuidler.toString());
		
		final String recipientId = recipient.getId().toString();
		
		UserProfile recipientProfile = userProfileManager.getUserProfile(recipientId);
		
		String displayName = EmailUtils.getDisplayNameWithUsername(recipientProfile);
		
		templateValues.put(PARAM_DISPLAY_NAME, displayName);
		
		return EmailUtils.readMailTemplate(TEMPLATE_FILE, templateValues);
	}

}
