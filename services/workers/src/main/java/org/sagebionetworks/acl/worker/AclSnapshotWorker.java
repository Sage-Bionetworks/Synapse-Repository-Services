package org.sagebionetworks.acl.worker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.Worker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.audit.dao.AclRecordDAO;
import org.sagebionetworks.audit.dao.ResourceAccessRecordDAO;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.ResourceAccessRecord;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker writes ACL change messages to a file, and put the file to S3 
 */
public class AclSnapshotWorker implements Worker{

	static private Logger log = LogManager.getLogger(AclSnapshotWorker.class);
	private List<Message> messages;
	private WorkerProgress workerProgress;
	@Autowired
	private AclRecordDAO aclRecordDao;
	@Autowired
	private ResourceAccessRecordDAO resourceAccessRecordDao;
	@Autowired
	private DBOChangeDAO changeDao;
	@Autowired
	private AccessControlListDAO accessControlListDao;

	public AclSnapshotWorker() {
	}

	// for unit test only
	AclSnapshotWorker(AclRecordDAO aclRecordDao, ResourceAccessRecordDAO resourceAccessRecordDao,
			DBOChangeDAO changeDao, AccessControlListDAO accessControlListDao) {
		this.aclRecordDao = aclRecordDao;
		this.resourceAccessRecordDao = resourceAccessRecordDao;
		this.changeDao = changeDao;
		this.accessControlListDao = accessControlListDao;
	}
	
	@Override
	public List<Message> call() throws Exception {
		List<Message> toDelete = new LinkedList<Message>();
		
		for(Message message: messages){
			try{
				Message returned = process(message);
				if(returned != null){
					toDelete.add(returned);
				}
			} catch(Throwable e){
				// Treat unknown errors as unrecoverable and return them
				toDelete.add(message);
				log.error("Worker Failed", e);
			}
		}
		return toDelete;
	}

	private Message process(Message message) throws Throwable {
		// Keep this message invisible
		workerProgress.progressMadeForMessage(message);
		
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		if (changeMessage.getObjectType() != ObjectType.ACCESS_CONTROL_LIST) {
			throw new IllegalArgumentException("ObjectType must be ACCESS_CONTROL_LIST");
		}

		AccessControlList acl = null;
		try {
			acl = accessControlListDao.get(Long.parseLong(changeMessage.getObjectId()));
		} catch (Throwable e) {
			// do nothing, other methods need to check for null acl
		}

		AclRecord aclRecord = buildAclRecord(changeMessage, acl);
		List<ResourceAccessRecord> resourceAccessRecords = buildResourceAccessRecordList(changeMessage, acl);

		aclRecordDao.saveBatch(Arrays.asList(aclRecord));
		if (!resourceAccessRecords.isEmpty()) {
			resourceAccessRecordDao.saveBatch(resourceAccessRecords);
		}
		return message;
	}

	protected List<ResourceAccessRecord> buildResourceAccessRecordList(ChangeMessage message, AccessControlList acl) {
		List<ResourceAccessRecord> records = new ArrayList<ResourceAccessRecord>();
		if (acl == null) {
			return records;
		}
		Set<ResourceAccess> resourceAccessSet = acl.getResourceAccess();
		if (resourceAccessSet == null) {
			return records;
		}
		for (ResourceAccess resourceAccess : resourceAccessSet) {
			Set<ACCESS_TYPE> accessTypeSet = resourceAccess.getAccessType();
			if (accessTypeSet != null) {
				for (ACCESS_TYPE accessType : accessTypeSet) {
					ResourceAccessRecord record = new ResourceAccessRecord();
					record.setChangeNumber(message.getChangeNumber());
					record.setPrincipalId(resourceAccess.getPrincipalId());
					record.setAccessType(accessType);
					records.add(record);
				}
			}
		}
		return records;
	}

	protected AclRecord buildAclRecord(ChangeMessage message, AccessControlList acl) {
		AclRecord record = new AclRecord();
		record.setAclId(message.getObjectId());
		record.setChangeNumber(message.getChangeNumber());
		record.setChangeType(message.getChangeType());
		record.setTimestamp(message.getTimestamp().getTime());
		record.setEtag(message.getObjectEtag());

		if (acl == null) {
			return record;
		}
		record.setOwnerId(acl.getId());
		record.setCreationDate(acl.getCreationDate());
		try {
			record.setOwnerType(accessControlListDao.getOwnerType(Long.parseLong(message.getObjectId())));
		} catch (Exception e) {
			// do nothing, the ownerType field remains null
		}

		return record;
	}

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
	}
	
}
