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
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectSettingObjectRecordWriter implements ObjectRecordWriter {
	private static Logger log = LogManager.getLogger(ProjectSettingObjectRecordWriter.class);

	public static final String STREAM_NAME = "projectSettingSnapshots";

	private final ProjectSettingsDAO projectSettingsDao;
	private final ObjectRecordDAO objectRecordDAO;
	private final AwsKinesisFirehoseLogger kinesisLogger;

	@Autowired
	public ProjectSettingObjectRecordWriter(ProjectSettingsDAO projectSettingsDao, ObjectRecordDAO objectRecordDAO,
			AwsKinesisFirehoseLogger kinesisLogger) {
		this.projectSettingsDao = projectSettingsDao;
		this.objectRecordDAO = objectRecordDAO;
		this.kinesisLogger = kinesisLogger;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages)
			throws IOException {

		List<KinesisObjectSnapshotRecord<ProjectSetting>> records = new ArrayList<>(messages.size());
		List<ObjectRecord> toWrite = new LinkedList<ObjectRecord>();
		for (ChangeMessage message : messages) {
			if (message.getObjectType() != ObjectType.PROJECT_SETTING) {
				throw new IllegalArgumentException();
			}
			// skip delete messages
			if (message.getChangeType() == ChangeType.DELETE) {
				continue;
			}
			try {
				ProjectSetting projectSetting = projectSettingsDao.get(message.getObjectId());
				ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(projectSetting,
						message.getTimestamp().getTime());
				toWrite.add(objectRecord);

				records.add(KinesisObjectSnapshotRecord.map(message, projectSetting));
			} catch (NotFoundException e) {
				log.error("Cannot find ProjectSetting for a " + message.getChangeType() + " message: "
						+ message.toString());
			}
		}
		if (!toWrite.isEmpty()) {
			objectRecordDAO.saveBatch(toWrite, toWrite.get(0).getJsonClassName());
		}

		if (!records.isEmpty()) {
			kinesisLogger.logBatch(STREAM_NAME, records);
		}
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.PROJECT_SETTING;
	}
}
