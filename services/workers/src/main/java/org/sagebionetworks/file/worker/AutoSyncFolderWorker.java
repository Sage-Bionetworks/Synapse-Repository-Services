package org.sagebionetworks.file.worker;

import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.message.SyncFolderMessage;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.HasQueueUrl;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.progress.ProgressingRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class AutoSyncFolderWorker implements ProgressingRunner<Void> {

	static private Logger log = LogManager.getLogger(AutoSyncFolderWorker.class);

	@Autowired
	private ProjectSettingsManager projectSettingsManager;
	@Autowired
	AmazonSQSClient awsSQSClient;

	private HasQueueUrl messageQueue;

	@Required
	public void setFolderSyncQueue(HasQueueUrl messageQueue) {
		this.messageQueue = messageQueue;
	}

	/**
	 * This is where the real work happens
	 */
	@Override
	public void run(
			ProgressCallback<Void> callback)
			throws Exception {
		List<ExternalSyncSetting> externalSyncSettings = projectSettingsManager.getNodeSettingsByType(ProjectSettingsType.external_sync,
				ExternalSyncSetting.class);
		for (ExternalSyncSetting externalSyncSetting : externalSyncSettings) {
			if (BooleanUtils.isTrue(externalSyncSetting.getAutoSync())) {
				try {
					SyncFolderMessage syncFolderMessage = new SyncFolderMessage();
					syncFolderMessage.setEntityId(externalSyncSetting.getProjectId());
					String bodyJson = EntityFactory.createJSONStringForEntity(syncFolderMessage);
					// publish the message
					awsSQSClient.sendMessage(new SendMessageRequest(messageQueue.getQueueUrl(), bodyJson));
					callback.progressMade(null);
				} catch (Throwable t) {
					log.error("Failed to auto sync for setting id " + externalSyncSetting.getId() + ": " + t.getMessage(), t);
				}
			}
		}
	}

}
