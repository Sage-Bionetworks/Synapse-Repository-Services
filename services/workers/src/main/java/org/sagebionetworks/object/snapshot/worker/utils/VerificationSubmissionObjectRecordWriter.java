package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.VerificationManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class VerificationSubmissionObjectRecordWriter implements ObjectRecordWriter {
	private static Logger log = LogManager.getLogger(VerificationSubmissionObjectRecordWriter.class);

	@Autowired
	private VerificationManager verificationManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;

	public static final long LIMIT = 10L;

	@Override
	public void buildAndWriteRecord(ChangeMessage message) throws IOException {
		if (message.getObjectType() != ObjectType.VERIFICATION_SUBMISSION || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		Long userId = Long.parseLong(message.getObjectId());
		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		try {
			long offset = 0L;
			List<ObjectRecord> toWrite = new ArrayList<ObjectRecord>();
			VerificationPagedResults records = null;
			do {
				records = verificationManager.listVerificationSubmissions(adminUser, null, userId, LIMIT , offset);
				for (VerificationSubmission record : records.getResults()) {
					toWrite.add(ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime()));
				}
				offset += LIMIT;
			} while (offset < records.getTotalNumberOfResults());
			if (!toWrite.isEmpty()) {
				objectRecordDAO.saveBatch(toWrite, toWrite.get(0).getJsonClassName());
			}
		} catch (NotFoundException e) {
			log.error("Cannot find verification submission for user " + message.getObjectId() + " message: " + message.toString()) ;
		}
	}

}
