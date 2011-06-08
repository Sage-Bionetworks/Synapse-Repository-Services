package org.sagebionetworks.repo.web.controller.metadata;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.StoredLayerPreview;
import org.sagebionetworks.repo.model.UserInfo;

public class StoredLayerPreviewMetadataProvider implements TypeSpecificMetadataProvider<StoredLayerPreview>{

	@Override
	public void addTypeSpecificMetadata(StoredLayerPreview entity,	HttpServletRequest request, UserInfo user) {
		// Clear the blob and set the string
		if(entity.getPreviewBlob() != null){
			try {
				entity.setPreviewString(new String(entity.getPreviewBlob(), "UTF-8"));
				entity.setPreviewBlob(null);
			} catch (UnsupportedEncodingException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	@Override
	public void validateEntity(StoredLayerPreview entity) {
		// Convert the blob value to the string value
		if(entity.getPreviewString() != null){
			try {
				entity.setPreviewBlob(entity.getPreviewString().getBytes("UTF-8"));
				entity.setPreviewString(null);
			} catch (UnsupportedEncodingException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

}
