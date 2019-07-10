package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AclObjectRecordWriter implements ObjectRecordWriter {

	static private Logger log = LogManager.getLogger(AclObjectRecordWriter.class);
	@Autowired
	private AccessControlListDAO accessControlListDao;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;

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
			} catch (NotFoundException e) {
				log.error("Cannot find acl for a " + message.getChangeType() + " message: " + message.toString()) ;
			}
		}
		if (!toWrite.isEmpty()) {
			objectRecordDAO.saveBatch(toWrite, toWrite.get(0).getJsonClassName());
		}
	}

}
