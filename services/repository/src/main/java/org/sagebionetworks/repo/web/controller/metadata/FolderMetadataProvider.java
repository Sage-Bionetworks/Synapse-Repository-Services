package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class FolderMetadataProvider implements TypeSpecificMetadataProvider<Folder> {

	@Override
	public void validateEntity(Folder entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTypeSpecificMetadata(Folder entity,
			HttpServletRequest request, UserInfo user, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entityDeleted(Folder deleted) {
		// TODO Auto-generated method stub
		
	}

}
