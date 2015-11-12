package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
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

	AclObjectRecordWriter(){}
	
	// for test only
	AclObjectRecordWriter(AccessControlListDAO accessControlListDao, ObjectRecordDAO objectRecordDAO) {
		this.accessControlListDao = accessControlListDao;
		this.objectRecordDAO = objectRecordDAO;
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
		record.setUri(acl.getUri());
		return record;
	}

	@Override
	public void buildAndWriteRecord(ChangeMessage message) throws IOException {
		if (message.getObjectType() != ObjectType.ACCESS_CONTROL_LIST || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		try {
			AccessControlList acl = accessControlListDao.get(Long.parseLong(message.getObjectId()));
			if (!acl.getEtag().equals(message.getObjectEtag())) {
				log.info("Ignoring old message.");
			}
			AclRecord record = buildAclRecord(acl, accessControlListDao.getOwnerType(Long.parseLong(message.getObjectId())));
			ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime());
			objectRecordDAO.saveBatch(Arrays.asList(objectRecord), objectRecord.getJsonClassName());
		} catch (NotFoundException e) {
			log.error("Cannot find acl for a " + message.getChangeType() + " message: " + message.toString()) ;
		}
	}

}
