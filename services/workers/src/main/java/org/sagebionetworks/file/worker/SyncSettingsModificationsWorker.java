package org.sagebionetworks.file.worker;

import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.AbstractWorker;
import org.sagebionetworks.asynchronous.workers.sqs.MessageQueue;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.SingletonWorker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.message.NodeSettingsModificationMessage;
import org.sagebionetworks.repo.model.message.SyncFolderMessage;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.TransientDataAccessException;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SyncSettingsModificationsWorker extends SingletonWorker {

	static private Logger log = LogManager.getLogger(SyncSettingsModificationsWorker.class);

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	AmazonSQSClient awsSQSClient;

	private MessageQueue messageQueue;

	@Required
	public void setFolderSyncQueue(MessageQueue messageQueue) {
		this.messageQueue = messageQueue;
	}

	/**
	 * This is where the real work happens
	 * 
	 * @param message
	 * @return
	 * @throws Throwable
	 */
	@Override
	protected Message processMessage(Message message, WorkerProgress workerProgress) throws Throwable {
		try {
			NodeSettingsModificationMessage modificationMessage = extractStatus(message);
			if (modificationMessage.getProjectSettingsType() == ProjectSettingsType.external_sync) {
				EntityHeader entityHeader = nodeDao.getEntityHeader(modificationMessage.getObjectId(), null);
				if (entityHeader.getType().equals(Project.class.getName()) || entityHeader.getType().equals(Folder.class.getName())) {
					ProjectSetting projectSetting = projectSettingsDao.get(modificationMessage.getObjectId(),
							ProjectSettingsType.external_sync);
					if (projectSetting != null) {
						if (projectSetting instanceof ExternalSyncSetting) {
							ExternalSyncSetting externalSyncSetting = (ExternalSyncSetting) projectSetting;
							if (BooleanUtils.isTrue(externalSyncSetting.getAutoSync())) {
								SyncFolderMessage syncFolderMessage = new SyncFolderMessage();
								syncFolderMessage.setEntityId(modificationMessage.getObjectId());
								String bodyJson = EntityFactory.createJSONStringForEntity(syncFolderMessage);
								// publish the message
								awsSQSClient.sendMessage(new SendMessageRequest(messageQueue.getQueueName(), bodyJson));
							}
						}
					}
				}
			}
			return message;
		} catch (NotFoundException e) {
			// probably means the entity was deleted in the mean time. No biggy
			return message;
		} catch (TransientDataAccessException e) {
			return null;
		}
	}

	/**
	 * Extract the AsynchUploadRequestBody from the message.
	 * 
	 * @param message
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	NodeSettingsModificationMessage extractStatus(Message message) throws JSONObjectAdapterException {
		ValidateArgument.required(message, "message");

		NodeSettingsModificationMessage modificationMessage = MessageUtils.extractMessageBody(message, NodeSettingsModificationMessage.class);

		ValidateArgument.required(modificationMessage.getObjectType(), "modificationMessage.objectType");
		ValidateArgument.required(modificationMessage.getObjectId(), "modificationMessage.objectId");
		ValidateArgument.required(modificationMessage.getProjectSettingsType(), "modificationMessage.projectSettingsType");

		return modificationMessage;
	}
}
