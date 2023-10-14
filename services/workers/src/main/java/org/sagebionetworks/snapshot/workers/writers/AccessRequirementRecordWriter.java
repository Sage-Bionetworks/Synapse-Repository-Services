package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.springframework.stereotype.Service;

@Service
public class AccessRequirementRecordWriter implements ObjectRecordWriter {
	
	private static final String STREAM_NAME = "accessRequirementSnapshots";
	
	private static Logger log = LogManager.getLogger(AccessRequirementRecordWriter.class);
	
	private AwsKinesisFirehoseLogger kinesisLogger;
	
	private AccessRequirementManager arManager;

	public AccessRequirementRecordWriter(AwsKinesisFirehoseLogger kinesisLogger, AccessRequirementManager arManager) {
		this.kinesisLogger = kinesisLogger;
		this.arManager = arManager;
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.ACCESS_REQUIREMENT;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException {
		
		List<KinesisObjectSnapshotRecord<AccessRequirement>> records = new ArrayList<>(messages.size());
		
		messages.forEach(message -> {
			if (ChangeType.DELETE == message.getChangeType()) {
				
				// On a delete we do not have the data, just log the id
				AccessRequirement ar = new ManagedACTAccessRequirement()
					.setId(Long.valueOf(message.getObjectId()))
					.setConcreteType(null);
				
				records.add(KinesisObjectSnapshotRecord.map(message, ar));
				
			} else {
				arManager.getAccessRequirementVersion(message.getObjectId(), message.getObjectVersion()).ifPresentOrElse(
					ar -> records.add(
						KinesisObjectSnapshotRecord.map(message, ar)
					), 
					() -> log.warn("Could not find an ar with id " + message.getObjectId() + " and version " + message.getObjectVersion() + " (changeType: " +message.getChangeType() + ")")
				);
			}
		});
		
		if (!records.isEmpty()) {
			kinesisLogger.logBatch(STREAM_NAME, records);
		}
	}

}
