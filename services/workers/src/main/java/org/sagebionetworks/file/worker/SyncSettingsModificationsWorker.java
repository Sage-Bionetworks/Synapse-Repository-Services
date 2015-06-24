package org.sagebionetworks.file.worker;

import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.message.ModificationMessage;
import org.sagebionetworks.repo.model.message.NodeSettingsModificationMessage;
import org.sagebionetworks.repo.model.message.SyncFolderMessage;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.HasQueueUrl;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.MessageQueue;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.TransientDataAccessException;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SyncSettingsModificationsWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(SyncSettingsModificationsWorker.class);

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	AmazonSQSClient awsSQSClient;

	private HasQueueUrl messageQueue;

	@Required
	public void setFolderSyncQueue(HasQueueUrl messageQueue) {
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
	public void run(ProgressCallback<Message> progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		try {
			NodeSettingsModificationMessage modificationMessage = extractStatus(message);
			if (modificationMessage != null && modificationMessage.getProjectSettingsType() == ProjectSettingsType.external_sync) {
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
								awsSQSClient.sendMessage(new SendMessageRequest(messageQueue.getQueueUrl(), bodyJson));
							}
						}
					}
				}
			}
		} catch (NotFoundException e) {
			// probably means the entity was deleted in the mean time. No biggy
			return;
		} catch (TransientDataAccessException e) {
			throw new RecoverableMessageException();
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

		ModificationMessage modificationMessage = MessageUtils.extractMessageBody(message, ModificationMessage.class);
		if (modificationMessage instanceof NodeSettingsModificationMessage) {
			NodeSettingsModificationMessage nodeSettingsModificationMessage = (NodeSettingsModificationMessage) modificationMessage;
			ValidateArgument.required(nodeSettingsModificationMessage.getObjectType(), "modificationMessage.objectType");
			ValidateArgument.required(nodeSettingsModificationMessage.getObjectId(), "modificationMessage.objectId");
			ValidateArgument.required(nodeSettingsModificationMessage.getProjectSettingsType(), "modificationMessage.projectSettingsType");

			return nodeSettingsModificationMessage;
		} else {
			return null;
		}
	}

}
