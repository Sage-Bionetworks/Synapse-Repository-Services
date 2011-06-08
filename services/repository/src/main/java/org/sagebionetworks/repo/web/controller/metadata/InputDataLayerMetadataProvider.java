package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.UserInfo;

public class InputDataLayerMetadataProvider implements TypeSpecificMetadataProvider<InputDataLayer> {

	@Override
	public void addTypeSpecificMetadata(InputDataLayer entity,	HttpServletRequest request, UserInfo user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void validateEntity(InputDataLayer entity) {
		if(entity.getVersion() == null){
			entity.setVersion("1.0.0");
		}
	}

}
