package org.sagebionetworks.acl.worker;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.Worker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.audit.dao.AclRecordDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.AclRecord;
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
	DBOChangeDAO changeDAO;
	
	@Override
	public List<Message> call() throws Exception {
		List<Message> toDelete = new LinkedList<Message>();
		
		for(Message message: messages){
			try{
				Message returned = process(message);
				if(returned != null){
					toDelete.add(returned);
				}
			}catch(Throwable e){
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
		
		AclRecord record = new AclRecord();
		ChangeMessage change = MessageUtils.extractMessageBody(message);
		if (change.getObjectType() != ObjectType.ACCESS_CONTROL_LIST) {
			throw new IllegalArgumentException("ObjectType must be ACCESS_CONTROL_LIST");
		}
		record.setChangeType(change.getChangeType().name());
		record.setEtag(change.getObjectEtag());
		record.setObjectId(change.getObjectId());
		record.setTimestamp(change.getTimestamp().getTime());
		if (change.getChangeNumber() != null) {
			record.setChangeNumber(change.getChangeNumber().toString());
		} else {
			record.setChangeNumber("null");
		}
		aclRecordDao.write(record);
		return message;
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
