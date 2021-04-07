package org.sagebionetworks.audit.kinesis;

import org.sagebionetworks.repo.model.audit.AclRecord;

public class AclKinesisLogRecordUtils {

	public static AclKinesisLogRecord buildAclKinesisLogRecord(AclRecord aclRecord, long timestamp) {
		AclKinesisLogRecord aclKinesisLogRecord = new AclKinesisLogRecord().withAclRecord(aclRecord).withTimestamp(timestamp);
		return aclKinesisLogRecord;
	}
}
