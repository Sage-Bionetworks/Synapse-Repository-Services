package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.web.UrlHelpers;

public class DatasetMetadataProvider implements TypeSpecificMetadataProvider<Dataset>{

	/**
	 * This should add the url to this datasets annotations.  And a link to this datasets layers
	 */
	@Override
	public void addTypeSpecificMetadata(Dataset entity,	HttpServletRequest request) {
		// Add the annotations urls.
		entity.setAnnotations(UrlHelpers.makeEntityPropertyUri(entity, Annotations.class, request));
		
		entity.setLayer(UrlHelpers.makeEntityUri(entity, request) + "/layer");
	}

	/**
	 * Make sure version is not null
	 */
	@Override
	public void validateEntity(Dataset entity) {
		if(entity.getVersion() == null){
			entity.setVersion("1.0.0");
		}
	}

}
