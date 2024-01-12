package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.verification.VerificationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerificationSubmissionObjectRecordWriter implements ObjectRecordWriter {
	private static final String KINESIS_STREAM = "verificationSubmissionSnapshots";
	public static final long LIMIT = 10L;
	
	private static Logger log = LogManager.getLogger(VerificationSubmissionObjectRecordWriter.class);

	private VerificationManager verificationManager;
	private UserManager userManager;
	private AwsKinesisFirehoseLogger kinesisLogger;

	@Autowired
	public VerificationSubmissionObjectRecordWriter(VerificationManager verificationManager, UserManager userManager, AwsKinesisFirehoseLogger kinesisLogger) {
		this.verificationManager = verificationManager;
		this.userManager = userManager;
		this.kinesisLogger = kinesisLogger;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException {
		List<KinesisObjectSnapshotRecord<VerificationSubmission>> kinesisVerificationRecords = new ArrayList<>();
		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		for (ChangeMessage message : messages) {
			if (message.getObjectType() != ObjectType.VERIFICATION_SUBMISSION) {
				throw new IllegalArgumentException();
			}
			// skip delete messages
			if (message.getChangeType() == ChangeType.DELETE) {
				continue;
			}
			Long userId = Long.parseLong(message.getObjectId());
			try {
				long offset = 0L;
				VerificationPagedResults records = null;
				do {
					records = verificationManager.listVerificationSubmissions(adminUser, null, userId, LIMIT , offset);
					for (VerificationSubmission record : records.getResults()) {
						kinesisVerificationRecords.add(KinesisObjectSnapshotRecord.map(message, record));
					}
					offset += LIMIT;
				} while (offset < records.getTotalNumberOfResults());
			} catch (NotFoundException e) {
				log.error("Cannot find verification submission for user " + message.getObjectId() + " message: " + message.toString()) ;
			}
		}
		if (!kinesisVerificationRecords.isEmpty()) {
			kinesisLogger.logBatch(KINESIS_STREAM, kinesisVerificationRecords);
		}
	}
	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.VERIFICATION_SUBMISSION;
	}
}
