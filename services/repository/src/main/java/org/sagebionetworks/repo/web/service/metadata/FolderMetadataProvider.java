package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.sts.StsManager;
import org.sagebionetworks.repo.model.Folder;
import org.springframework.beans.factory.annotation.Autowired;

public class FolderMetadataProvider implements EntityValidator<Folder> {
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private StsManager stsManager;

	@Override
	public void validateEntity(Folder folder, EntityEvent event) {
		if (EventType.UPDATE == event.getType()) {
			// Fetch old folder to see if folder is being moved. Note that AllTypesValidator ensures that the old
			// folder exists and that both the new and old folder have parent IDs.
			Folder oldFolder = entityManager.getEntity(event.getUserInfo(), folder.getId(), Folder.class);
			if (!folder.getParentId().equals(oldFolder.getParentId())) {
				stsManager.validateCanMoveFolder(event.getUserInfo(), folder.getId(), oldFolder.getParentId(),
						folder.getParentId());
			}
		}
	}
}
