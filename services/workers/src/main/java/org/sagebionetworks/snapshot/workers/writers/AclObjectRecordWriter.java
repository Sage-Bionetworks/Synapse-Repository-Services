package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AclObjectRecordWriter implements ObjectRecordWriter {
	private static final String KINESIS_STREAM = "aclSnapshots";
	static private Logger log = LogManager.getLogger(AclObjectRecordWriter.class);
	
	private AccessControlListDAO accessControlListDao;
	private ObjectRecordDAO objectRecordDAO;
	private AwsKinesisFirehoseLogger kinesisLogger;
	
	@Autowired
	public AclObjectRecordWriter(AccessControlListDAO accessControlListDao, ObjectRecordDAO objectRecordDAO,
								 AwsKinesisFirehoseLogger kinesisLogger) {
		this.accessControlListDao = accessControlListDao;
		this.objectRecordDAO = objectRecordDAO;
		this.kinesisLogger = kinesisLogger;
	}

	/**
	 * Build an AclRecord that wrap around AccessControlList object and contains
	 * the type of the owner object.
	 * 
	 * @param acl
	 * @param ownerType
	 * @return
	 */
	public static AclRecord buildAclRecord(AccessControlList acl, ObjectType ownerType) {
		AclRecord record = new AclRecord();
		record.setCreatedBy(acl.getCreatedBy());
		record.setCreationDate(acl.getCreationDate());
		record.setEtag(acl.getEtag());
		record.setId(acl.getId());
		record.setModifiedBy(acl.getModifiedBy());
		record.setModifiedOn(acl.getModifiedOn());
		record.setOwnerType(ownerType);
		record.setResourceAccess(acl.getResourceAccess());
		return record;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException {
		List<ObjectRecord> toWrite = new LinkedList<ObjectRecord>();
		List<KinesisObjectSnapshotRecord<AclRecord>> kinesisAclRecords = new ArrayList<>();
		for (ChangeMessage message : messages) {
			if (message.getObjectType() != ObjectType.ACCESS_CONTROL_LIST) {
				throw new IllegalArgumentException();
			}
			// skip delete messages
			if (message.getChangeType() == ChangeType.DELETE) {
				continue;
			}
			try {
				AccessControlList acl = accessControlListDao.get(Long.parseLong(message.getObjectId()));
				AclRecord record = buildAclRecord(acl, accessControlListDao.getOwnerType(Long.parseLong(message.getObjectId())));
				ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime());
				toWrite.add(objectRecord);
				kinesisAclRecords.add(KinesisObjectSnapshotRecord.map(message, record));
			} catch (NotFoundException e) {
				log.error("Cannot find acl for a " + message.getChangeType() + " message: " + message.toString()) ;
			}
		}
		if (!toWrite.isEmpty()) {
			objectRecordDAO.saveBatch(toWrite, toWrite.get(0).getJsonClassName());
		}
		if (!kinesisAclRecords.isEmpty()) {
			kinesisLogger.logBatch(KINESIS_STREAM, kinesisAclRecords);
		}
	}
	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.ACCESS_CONTROL_LIST;
	}

}
