package org.sagebionetworks.object.snapshot.worker.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AclObjectRecordBuilder implements ObjectRecordBuilder {

	static private Logger log = LogManager.getLogger(AclObjectRecordBuilder.class);
	@Autowired
	private AccessControlListDAO accessControlListDao;

	AclObjectRecordBuilder(){}
	
	// for test only
	AclObjectRecordBuilder(AccessControlListDAO accessControlListDao) {
		this.accessControlListDao = accessControlListDao;
	}

	@Override
	public ObjectRecord build(ChangeMessage message) {
		if (message.getObjectType() != ObjectType.ACCESS_CONTROL_LIST || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		try {
			AccessControlList acl = accessControlListDao.get(Long.parseLong(message.getObjectId()));
			if (!acl.getEtag().equals(message.getObjectEtag())) {
				log.info("Ignoring old message.");
				return null;
			}
			return ObjectRecordBuilderUtils.buildObjectRecord(acl, message.getTimestamp().getTime());
		} catch (NotFoundException e) {
			log.error("Cannot find acl for a " + message.getChangeType() + " message: " + message.toString()) ;
			return null;
		}
	}

}
