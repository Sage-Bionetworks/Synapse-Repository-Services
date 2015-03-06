package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UploadDestinationLocationDAO {

	public Long create(UploadDestinationLocationSetting setting);

	public <T extends UploadDestinationLocationSetting> T update(T settings) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException;

	public UploadDestinationLocationSetting get(Long id) throws DatastoreException, NotFoundException;

	public List<UploadDestinationLocation> getUploadDestinationLocations(List<Long> locations);

	public List<UploadDestinationLocationSetting> getAllUploadDestinationLocationSettings();
}
