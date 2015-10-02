package org.sagebionetworks.object.snapshot.worker.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ProjectSettingObjectRecordBuilder implements ObjectRecordBuilder {
	private static Logger log = LogManager.getLogger(ProjectSettingObjectRecordBuilder.class);

	@Autowired
	private ProjectSettingsDAO projectSettingsDao;

	ProjectSettingObjectRecordBuilder() {}

	// for test only
	ProjectSettingObjectRecordBuilder(ProjectSettingsDAO projectSettingsDao) {
		this.projectSettingsDao = projectSettingsDao;
	}

	@Override
	public List<ObjectRecord> build(ChangeMessage message) {
		if (message.getObjectType() != ObjectType.PROJECT_SETTING || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		try {
			ProjectSetting projectSetting = projectSettingsDao.get(message.getObjectId());
			return Arrays.asList(ObjectRecordBuilderUtils.buildObjectRecord(projectSetting, message.getTimestamp().getTime()));
		} catch (NotFoundException e) {
			log.error("Cannot find ProjectSetting for a " + message.getChangeType() + " message: " + message.toString()) ;
			return null;
		}
	}

}
