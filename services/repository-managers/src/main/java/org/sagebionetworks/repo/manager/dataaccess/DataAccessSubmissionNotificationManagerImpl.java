package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.manager.message.PrincipalNameProvider;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class DataAccessSubmissionNotificationManagerImpl implements DataAccessSubmissionNotificationManager {

	private static final Logger LOG = LogManager.getLogger(DataAccessSubmissionNotificationManagerImpl.class);

	private final AccessControlListDAO aclDao;
	private final TemplatedMessageSender templatedMessageSender;
	private final UserManager userManager;
	private final SubmissionDAO submissionDao;
	private final PrincipalNameProvider userNameProvider;

	public DataAccessSubmissionNotificationManagerImpl(AccessControlListDAO aclDao,
			TemplatedMessageSender templatedMessageSender, UserManager userManager, SubmissionDAO submissionDao,
			PrincipalNameProvider userNameProvider) {
		super();
		this.aclDao = aclDao;
		this.templatedMessageSender = templatedMessageSender;
		this.userManager = userManager;
		this.submissionDao = submissionDao;
		this.userNameProvider = userNameProvider;
	}

	@WriteTransaction
	@Override
	public void sendNotificationToReviewers(String dataAccessSubmissionId) {
		ValidateArgument.required(dataAccessSubmissionId, "dataAccessSubmissionId");
		Submission submission = submissionDao.getSubmission(dataAccessSubmissionId);
		if (!SubmissionState.SUBMITTED.equals(submission.getState())) {
			LOG.info(String.format(
					"A notification message will not be sent for submission: %s because the submission state is: %s",
					dataAccessSubmissionId, submission.getState().name()));
			return;
		}
		/*
		 * We only send a message to non-ACT principals that have been explicitly
		 * granted the review permission on the AR associated with the access request.
		 */
		aclDao.getAcl(submission.getAccessRequirementId(), ObjectType.ACCESS_REQUIREMENT).ifPresent((acl) -> {
			acl.getResourceAccess().stream()
			.filter(r -> r.getAccessType().contains(ACCESS_TYPE.REVIEW_SUBMISSIONS))
					.map(r -> r.getPrincipalId()).filter(Predicate.not(p -> TeamConstants.ACT_TEAM_ID.equals(p)))
					.forEach((reviewerPrincialId) -> {
						sendNotificationMessageToReviewer(reviewerPrincialId, dataAccessSubmissionId,
								Long.parseLong(submission.getSubmittedBy()));
					});
		});
	}

	/**
	 * Sent a notification email to the provided to a reviewer.
	 * 
	 * @param reviewerPrincialId
	 * @param submissionId
	 * @param requesterPrincipalId
	 */
	void sendNotificationMessageToReviewer(Long reviewerPrincialId, String submissionId, Long submittedById) {

		UserInfo sender = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());

		Map<String, Object> context = Map.of(
				"reviewerName", userNameProvider.getPrincipalName(reviewerPrincialId),
				"dataAccessSubmissionId", submissionId,
				"submittedByName", userNameProvider.getPrincipalName(submittedById)				
		);
		String messageId = templatedMessageSender
				.sendMessage(MessageTemplate.builder().withSender(sender)
						.withRecipients(Collections.singleton(reviewerPrincialId.toString()))
						.withTemplateFile("message/DataAccessSubmissionNotificationTemplate.html.vtl")
						.withSubject("[Time Sensitive] Request for access to data").withContext(context).build())
				.getId();

		LOG.info(String.format("Sent a message with id: %s to reviewerId: %s ", messageId, reviewerPrincialId));
	}

}
