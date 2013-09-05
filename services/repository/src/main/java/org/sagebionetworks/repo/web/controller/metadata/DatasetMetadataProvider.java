package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class DatasetMetadataProvider implements TypeSpecificMetadataProvider<Study>{
	

	/**
	 * This should add the url to this datasets annotations.  And a link to this datasets layers
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Override
	public void addTypeSpecificMetadata(Study entity,	HttpServletRequest request, UserInfo user, EventType eventType) throws DatastoreException, NotFoundException, UnauthorizedException {
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getId() == null) throw new IllegalArgumentException("Entity.id cannot be null");
	}
	
	/**
	 * Make sure version is not null
	 */
	@Override
	public void validateEntity(Study entity, EntityEvent event) {
		//Nothing to do
	}

	@Override
	public void entityDeleted(Study entity) {
	}

}
