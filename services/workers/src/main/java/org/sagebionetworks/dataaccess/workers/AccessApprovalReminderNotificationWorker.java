package org.sagebionetworks.dataaccess.workers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManager;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.feature.Feature;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Worker that periodically checks the approvals that are about to expire and sends a reminder to the user
 * 
 * @author Marco Marasca
 */
public class AccessApprovalReminderNotificationWorker implements ProgressingRunner {
	
	private static final List<DataAccessNotificationType> REMINDER_TYPES = Stream.of(DataAccessNotificationType.values())
			.filter(DataAccessNotificationType::isReminder).collect(Collectors.toList());
	
	protected static final int BATCH_SIZE = 200;

	private AccessApprovalNotificationManager notificationManager;
	
	private FeatureManager featureManager;
	
	private Logger logger;
	
	@Autowired
	public AccessApprovalReminderNotificationWorker(
			final AccessApprovalNotificationManager notificationManager,
			final FeatureManager featureManager) {
		this.notificationManager = notificationManager;
		this.featureManager = featureManager;
	}
	
	@Autowired
	void configureLogger(LoggerProvider loggerProvider) {
		this.logger = loggerProvider.getLogger(AccessApprovalReminderNotificationWorker.class.getName());
	}
	
	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		try {
			// Feature not yet enabled
			if (!featureManager.isFeatureEnabled(Feature.DATA_ACCESS_NOTIFICATIONS)) {
				return;
			}
			
			REMINDER_TYPES.forEach(this::processReminderType);
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
		
	}
	
	void processReminderType(DataAccessNotificationType notificationType) {
		
		List<Long> expiringApprovals = notificationManager.listSubmitterApprovalsForUnsentReminder(notificationType, BATCH_SIZE);
		
		long startTime = System.currentTimeMillis();
		int processedCount = 0;
		int errorsCount = 0;
		
		for (Long approvalId : expiringApprovals) {
			try {
				notificationManager.processAccessApproval(notificationType, approvalId);
				processedCount++;
			} catch (Throwable e) {
				errorsCount++;
				// Logs the error, but keeps going with the rest of the approvals
				logger.error(e.getMessage(),  e);
			}
		}
		
		logger.info("Sucessfully processed {} reminders (Type: {}, Errored: {}, Time: {} ms).", processedCount, notificationType, errorsCount, System.currentTimeMillis() - startTime);
	}

}
