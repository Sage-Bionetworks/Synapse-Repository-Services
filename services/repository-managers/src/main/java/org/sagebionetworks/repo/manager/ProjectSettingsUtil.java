package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UploadDestinationLocationDAO;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.util.CollectionUtils;

public class ProjectSettingsUtil {

	public static void validateProjectSetting(ProjectSetting setting, UploadDestinationLocationDAO uploadDestinationLocationDAO) {
		ValidateArgument.required(setting.getProjectId(), "projectId");
		ValidateArgument.required(setting.getSettingsType(), "settingsType");
		if (setting instanceof UploadDestinationListSetting) {
			ProjectSettingsUtil.validateUploadDestinationListSetting((UploadDestinationListSetting) setting, uploadDestinationLocationDAO);
		} else {
			ValidateArgument.failRequirement("Cannot handle project setting of type " + setting.getClass().getName());
		}
	}

	private static void validateUploadDestinationListSetting(UploadDestinationListSetting setting,
			UploadDestinationLocationDAO uploadDestinationLocationDAO) {
		ValidateArgument.requirement(CollectionUtils.isEmpty(setting.getDestinations()), "setting.getDestinations() cannot have a value.");
		ValidateArgument.required(setting.getLocations(), "settings.locations");
		ValidateArgument.requirement(setting.getLocations().size() >= 1, "settings.locations must at least have one entry");
		for (Long uploadId : setting.getLocations()) {
			try {
				uploadDestinationLocationDAO.get(uploadId);
			} catch (NotFoundException e) {
				ValidateArgument.failRequirement("uploadId " + uploadId + " is not a valid upload destination location");
			}
		}
	}
}
