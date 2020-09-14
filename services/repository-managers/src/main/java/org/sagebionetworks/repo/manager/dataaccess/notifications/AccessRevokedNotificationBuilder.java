package org.sagebionetworks.repo.manager.dataaccess.notifications;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessRevokedNotificationBuilder implements DataAccessNotificationBuilder {
	
	static final String TEMPLATE_FILE = "message/AccessApprovalRevokedTemplate.html.vtl";
	
	static final String PARAM_DISPLAY_NAME = "displayName";
	static final String PARAM_SUBMITTER_DISPLAY_NAME = "submitterDisplayName";
	static final String PARAM_REQUIREMENT_ID = "requirementId";
	static final String PARAM_REQUIREMENT_DESCRIPTION = "requirementDescription";

	private static final String SUBJECT_TEMPLATE = "%s Access Revoked";
	
	private UserProfileManager userProfileManager;
	private VelocityEngine velocityEngine;
	
	@Autowired
	public AccessRevokedNotificationBuilder(final UserProfileManager userProfileManager, final VelocityEngine velocityEngine) {
		this.userProfileManager = userProfileManager;
		this.velocityEngine = velocityEngine;
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
		 
		Template template = velocityEngine.getTemplate(TEMPLATE_FILE, StandardCharsets.UTF_8.name());
		VelocityContext context = buildContext(accessRequirement, approval, recipient);
		
		StringWriter writer = new StringWriter();

		template.merge(context, writer);
		
		return writer.toString();
	}
	
	VelocityContext buildContext(ManagedACTAccessRequirement accessRequirement, AccessApproval approval, UserInfo recipient) {
		VelocityContext context = new VelocityContext();
		
		final String displayName = getDisplayNameForUser(recipient.getId().toString());
		
		String submitterDisplayName = null;
		
		if (!approval.getSubmitterId().equals(recipient.getId().toString())) {
			submitterDisplayName = getDisplayNameForUser(approval.getSubmitterId());
		}
		
		context.put(PARAM_REQUIREMENT_ID, accessRequirement.getId());
		context.put(PARAM_REQUIREMENT_DESCRIPTION, StringUtils.trimToNull(accessRequirement.getDescription()));
		context.put(PARAM_DISPLAY_NAME, displayName);
		context.put(PARAM_SUBMITTER_DISPLAY_NAME, submitterDisplayName);
		
		return context;
	}
	
	private String getDisplayNameForUser(String userId) {
		final UserProfile profile = userProfileManager.getUserProfile(userId);
		return EmailUtils.getDisplayNameOrUsername(profile);
	}

}
