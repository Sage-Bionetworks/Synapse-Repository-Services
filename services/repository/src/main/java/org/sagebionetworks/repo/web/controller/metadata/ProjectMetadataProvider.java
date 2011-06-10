package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider.EventType;

public class ProjectMetadataProvider implements TypeSpecificMetadataProvider<Project> {

	@Override
	public void addTypeSpecificMetadata(Project entity,	HttpServletRequest request, UserInfo user, EventType eventType) {
		// Add the annotations urls.
		entity.setAnnotations(UrlHelpers.makeEntityPropertyUri(entity, Annotations.class, request));
	}

	@Override
	public void validateEntity(Project entity, EventType eventType) {

	}

	@Override
	public void entityDeleted(Project deleted) {
		// TODO Auto-generated method stub
		
	}

}
