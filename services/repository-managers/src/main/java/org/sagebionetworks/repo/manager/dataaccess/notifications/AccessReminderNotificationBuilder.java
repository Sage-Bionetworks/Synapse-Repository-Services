package org.sagebionetworks.repo.manager.dataaccess.notifications;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessReminderNotificationBuilder implements DataAccessNotificationBuilder {
	
	static final String TEMPLATE_FILE = "message/AccessApprovalReminderTemplate.html.vtl";
	
	static final String PARAM_DISPLAY_NAME = "displayName";
	static final String PARAM_REQUIREMENT_ID = "requirementId";
	static final String PARAM_REQUIREMENT_DESCRIPTION = "requirementDescription";
	static final String PARAM_IRB_APPROVAL_REQUIRED = "irbApprovalRequired";
	static final String PARAM_DUC_REQUIRED = "ducRequired";
	static final String PARAM_RENEWAL_DATE = "renewalDate";
	
	private static final String SUBJECT_TEMPLATE = "%s Access Renewal Reminder";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

	// All the reminders types
	private static final List<DataAccessNotificationType> SUPPORTED_TYPES = Stream.of(DataAccessNotificationType.values())
			.filter(DataAccessNotificationType::isReminder)
			.collect(Collectors.toList());
	
	private UserProfileManager userProfileManager;
	private VelocityEngine velocityEngine;
	
	@Autowired
	public AccessReminderNotificationBuilder(final UserProfileManager userProfileManager, final VelocityEngine velocityEngine) {
		this.userProfileManager = userProfileManager;
		this.velocityEngine = velocityEngine;
	}
	
	@Override
	public List<DataAccessNotificationType> supportedTypes() {
		return SUPPORTED_TYPES;
	}

	@Override
	public String buildSubject(ManagedACTAccessRequirement accessRequirement, AccessApproval approval,
			UserInfo recipient) {
		String prefix = "Data";
		
		if (!StringUtils.isBlank(accessRequirement.getDescription())) {
			prefix = accessRequirement.getDescription();
		}
		
		return String.format(SUBJECT_TEMPLATE, prefix);
	}

	@Override
	public String buildMessageBody(ManagedACTAccessRequirement accessRequirement, AccessApproval approval,
			UserInfo recipient) {
		
		Template template = velocityEngine.getTemplate(TEMPLATE_FILE, StandardCharsets.UTF_8.name());
		VelocityContext context = buildContext(accessRequirement, approval, recipient);
		
		StringWriter writer = new StringWriter();

		template.merge(context, writer);
		
		return writer.toString();
	}
	
	VelocityContext buildContext(ManagedACTAccessRequirement accessRequirement, AccessApproval approval, UserInfo recipient) {
		VelocityContext context = new VelocityContext();
		
		final UserProfile profile = userProfileManager.getUserProfile(recipient.getId().toString());
		
		final String displayName = EmailUtils.getDisplayNameOrUsername(profile);
		
		context.put(PARAM_REQUIREMENT_ID, accessRequirement.getId());
		context.put(PARAM_REQUIREMENT_DESCRIPTION, StringUtils.trimToNull(accessRequirement.getDescription()));
		context.put(PARAM_DISPLAY_NAME, displayName);
		context.put(PARAM_DUC_REQUIRED, accessRequirement.getIsDUCRequired());
		context.put(PARAM_IRB_APPROVAL_REQUIRED, accessRequirement.getIsIRBApprovalRequired());
		context.put(PARAM_RENEWAL_DATE, getFormattedDate(approval.getExpiredOn()));
		
		return context;
	}
	
	static String getFormattedDate(Date date) {
		ValidateArgument.required(date, "date");
		LocalDate localDate = date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
		return DATE_FORMATTER.format(localDate);
	}

}
