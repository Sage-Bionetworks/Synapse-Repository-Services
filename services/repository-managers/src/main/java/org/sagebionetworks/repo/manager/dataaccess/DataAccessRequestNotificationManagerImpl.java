package org.sagebionetworks.repo.manager.dataaccess;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.UserNameProvider;
import org.sagebionetworks.repo.manager.message.MessageTemplateBuilder;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestNotificationDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Repository;

@Repository
public class DataAccessRequestNotificationManagerImpl implements DataAccessRequestNotificationManager {

	private static final Logger LOG = LogManager.getLogger(DataAccessRequestNotificationManagerImpl.class);
	
	private final RequestManager dataAccessRequestManager;
	private final AccessControlListDAO aclDao;
	private final DataAccessRequestNotificationDao dataAccessRequestNotificationDao;
	private final TemplatedMessageSender templatedMessageSender;
	private final UserManager userManager;

	public DataAccessRequestNotificationManagerImpl(RequestManager dataAccessRequestManager,
			AccessControlListDAO aclDao, DataAccessRequestNotificationDao dataAccessRequestNotificationDao,
			TemplatedMessageSender templatedMessageSender, UserManager userManager) {
		super();
		this.dataAccessRequestManager = dataAccessRequestManager;
		this.aclDao = aclDao;
		this.dataAccessRequestNotificationDao = dataAccessRequestNotificationDao;
		this.templatedMessageSender = templatedMessageSender;
		this.userManager = userManager;
	}

	@WriteTransaction
	@Override
	public void sendNotificationToReviewers(String dataAccessRequestId) {
		ValidateArgument.required(dataAccessRequestId, "dataAccessRequestId");
		RequestInterface request = dataAccessRequestManager.getRequestForSubmission(dataAccessRequestId);
		/*
		 * We only send a message to non-ACT principals that have been explicitly
		 * granted the review permission on the AR associated with the access request.
		 */
		aclDao.getAcl(request.getAccessRequirementId(), ObjectType.ACCESS_REQUIREMENT).ifPresent((acl) -> {
			List<Long> nonActReviewerPrincipalIds = acl.getResourceAccess().stream()
					.filter(r -> r.getAccessType().contains(ACCESS_TYPE.REVIEW_SUBMISSIONS))
					.map(r -> r.getPrincipalId()).filter(p -> !TeamConstants.ACT_TEAM_ID.equals(p))
					.collect(Collectors.toList());
			nonActReviewerPrincipalIds.stream().forEach((reviewerPrincialId) -> {
				sendNotificationMessageToReviewers(reviewerPrincialId, dataAccessRequestId, Long.parseLong(request.getCreatedBy()));
			});
		});
	}

	/**
	 * Sent a notification email to the provided to a reviewer.
	 * 
	 * @param reviewerPrincialId  Reviewer can be either an individual or a team.
	 * @param dataAccessRequestId The ID of the data access request.
	 */
	void sendNotificationMessageToReviewers(Long reviewerPrincialId, String dataAccessRequestId, Long requesterPrincipalId) {

		Optional<Instant> sentOn = dataAccessRequestNotificationDao.getSentOn(dataAccessRequestId, reviewerPrincialId);
		if (sentOn.isPresent()) {
			if (sentOn.get().isAfter((Instant.now().minus(24, ChronoUnit.HOURS)))) {
				LOG.info(String.format(
						"A message was already sent in the past 24 hours on: '%s'. A new message will not be sent.",
						sentOn.get()));
				return;
			}
		}
		UserInfo sender = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());

		String messageId = templatedMessageSender.sendMessage(new MessageTemplateBuilder()
				.withSender(sender)
				.withRecipients(Collections.singleton(reviewerPrincialId.toString()))
				.withTemplateFile("message/DataAccessRequestNotificationTemplate.html.vtl")
				.withTemplateContextProvider((UserNameProvider displayNameProvider) -> {
					Map<String, Object> c = new HashMap<String, Object>();
					c.put("reviewerName", displayNameProvider.getPrincipaleName(reviewerPrincialId));
					c.put("dataAccessRequestId", dataAccessRequestId);
					c.put("requesterName", displayNameProvider.getPrincipaleName(requesterPrincipalId));
					return c;
				}).build()).getId();

		dataAccessRequestNotificationDao.messageSentOn(reviewerPrincialId, dataAccessRequestId, messageId,
				Instant.now());
	}

}
