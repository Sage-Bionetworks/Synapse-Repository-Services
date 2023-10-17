package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.audit.KinesisJsonEntityRecord;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CertifiedUserPassingRecordWriter implements ObjectRecordWriter {
	private static final String KINESIS_STREAM = "certifiedUserPassingSnapshots";
	public static final long LIMIT = 10L;

	private static Logger log = LogManager.getLogger(CertifiedUserPassingRecordWriter.class);

	private CertifiedUserManager certifiedUserManager;
	private UserManager userManager;
	private ObjectRecordDAO objectRecordDAO;
	private AwsKinesisFirehoseLogger kinesisLogger;
	private StackConfiguration stackConfiguration;
	
	
	@Autowired
	public CertifiedUserPassingRecordWriter(CertifiedUserManager certifiedUserManager, UserManager userManager,
			ObjectRecordDAO objectRecordDAO, AwsKinesisFirehoseLogger kinesisLogger, StackConfiguration stackConfiguration) {
		this.certifiedUserManager = certifiedUserManager;
		this.userManager = userManager;
		this.objectRecordDAO = objectRecordDAO;
		this.kinesisLogger = kinesisLogger;
		this.stackConfiguration = stackConfiguration;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException {
		List<ObjectRecord> toWrite = new LinkedList<ObjectRecord>();
		List<KinesisObjectSnapshotRecord<PassingRecord>> kinesisCertificationSnapshots = new ArrayList<>();
		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		for (ChangeMessage message : messages) {
			if (message.getObjectType() != ObjectType.CERTIFIED_USER_PASSING_RECORD) {
				throw new IllegalArgumentException();
			}
			// skip delete messages
			if (message.getChangeType() == ChangeType.DELETE) {
				continue;
			}
			Long userId = Long.parseLong(message.getObjectId());
			String stack = stackConfiguration.getStack();
			String instance = stackConfiguration.getStackInstance();
			try {
				long offset = 0L;
				PaginatedResults<PassingRecord> records = null;
				do {
					records = certifiedUserManager.getPassingRecords(adminUser, userId, LIMIT , offset);
					for (PassingRecord record : records.getResults()) {
						toWrite.add(ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime()));
						kinesisCertificationSnapshots.add(KinesisObjectSnapshotRecord.map(message, record));
					}
					offset += LIMIT;
				} while (offset < records.getTotalNumberOfResults());
			} catch (NotFoundException e) {
				log.error("Cannot find certified user passing record for user " + message.getObjectId() + " message: " + message.toString()) ;
			}
		}
		if (!toWrite.isEmpty()) {
			objectRecordDAO.saveBatch(toWrite, toWrite.get(0).getJsonClassName());
		}
		if (!kinesisCertificationSnapshots.isEmpty()) {
			kinesisLogger.logBatch(KINESIS_STREAM, kinesisCertificationSnapshots);
		}
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.CERTIFIED_USER_PASSING_RECORD;
	}
	
}
