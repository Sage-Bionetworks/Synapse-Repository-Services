package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectSettingObjectRecordWriter implements ObjectRecordWriter {
	private static Logger log = LogManager.getLogger(ProjectSettingObjectRecordWriter.class);

	public static final String STREAM_NAME = "projectSettingSnapshots";

	private final ProjectSettingsDAO projectSettingsDao;
	private final AwsKinesisFirehoseLogger kinesisLogger;

	@Autowired
	public ProjectSettingObjectRecordWriter(ProjectSettingsDAO projectSettingsDao, AwsKinesisFirehoseLogger kinesisLogger) {
		this.projectSettingsDao = projectSettingsDao;
		this.kinesisLogger = kinesisLogger;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages)
			throws IOException {

		List<KinesisObjectSnapshotRecord<ProjectSetting>> records = new ArrayList<>(messages.size());
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
				records.add(KinesisObjectSnapshotRecord.map(message, projectSetting));
			} catch (NotFoundException e) {
				log.error("Cannot find ProjectSetting for a " + message.getChangeType() + " message: "
						+ message.toString());
			}
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
