package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ProjectSettingObjectRecordWriter implements ObjectRecordWriter {
	private static Logger log = LogManager.getLogger(ProjectSettingObjectRecordWriter.class);

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;

	ProjectSettingObjectRecordWriter() {}

	// for test only
	ProjectSettingObjectRecordWriter(ProjectSettingsDAO projectSettingsDao, ObjectRecordDAO objectRecordDAO) {
		this.projectSettingsDao = projectSettingsDao;
		this.objectRecordDAO = objectRecordDAO;
	}

	@Override
	public void buildAndWriteRecord(ChangeMessage message) throws IOException {
		if (message.getObjectType() != ObjectType.PROJECT_SETTING || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		try {
			ProjectSetting projectSetting = projectSettingsDao.get(message.getObjectId());
			ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(projectSetting, message.getTimestamp().getTime());
			objectRecordDAO.saveBatch(Arrays.asList(objectRecord), objectRecord.getJsonClassName());
		} catch (NotFoundException e) {
			log.error("Cannot find ProjectSetting for a " + message.getChangeType() + " message: " + message.toString()) ;
		}
	}

}
