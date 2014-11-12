package org.sagebionetworks.repo.web.service.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;

/**
 *
 */
public class ProjectMetadataProvider implements TypeSpecificMetadataProvider<Project> {

	@Override
	public void addTypeSpecificMetadata(Project entity,	HttpServletRequest request, UserInfo user, EventType eventType) {
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getId() == null) throw new IllegalArgumentException("Entity.id cannot be null");
	}
}
