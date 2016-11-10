package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class CertifiedUserPassingRecordWriter implements ObjectRecordWriter {
	private static Logger log = LogManager.getLogger(CertifiedUserPassingRecordWriter.class);

	@Autowired
	private CertifiedUserManager certifiedUserManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;

	public static final long LIMIT = 10L;

	@Override
	public void buildAndWriteRecords(ProgressCallback<Void> progressCallback, List<ChangeMessage> messages) throws IOException {
		List<ObjectRecord> toWrite = new LinkedList<ObjectRecord>();
		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		for (ChangeMessage message : messages) {
			progressCallback.progressMade(null);
			if (message.getObjectType() != ObjectType.CERTIFIED_USER_PASSING_RECORD) {
				throw new IllegalArgumentException();
			}
			// skip delete messages
			if (message.getChangeType() == ChangeType.DELETE) {
				continue;
			}
			Long userId = Long.parseLong(message.getObjectId());
			try {
				long offset = 0L;
				PaginatedResults<PassingRecord> records = null;
				do {
					progressCallback.progressMade(null);
					records = certifiedUserManager.getPassingRecords(adminUser, userId, LIMIT , offset);
					for (PassingRecord record : records.getResults()) {
						toWrite.add(ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime()));
					}
					offset += LIMIT;
				} while (offset < records.getTotalNumberOfResults());
			} catch (NotFoundException e) {
				log.error("Cannot find certified user passing record for user " + message.getObjectId() + " message: " + message.toString()) ;
			}
		}
		if (!toWrite.isEmpty()) {
			progressCallback.progressMade(null);
			objectRecordDAO.saveBatch(toWrite, toWrite.get(0).getJsonClassName());
		}
	}

}
