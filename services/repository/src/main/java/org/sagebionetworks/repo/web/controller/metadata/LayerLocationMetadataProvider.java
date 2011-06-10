package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 */
public class LayerLocationMetadataProvider implements
		TypeSpecificMetadataProvider<LayerLocation> {

	@Autowired
	LocationHelper locationHelper;

	@Override
	public void addTypeSpecificMetadata(LayerLocation entity,
			HttpServletRequest request, UserInfo user, EventType eventType) throws DatastoreException,
			NotFoundException {

		// Special handling for S3 locations
		if (entity.getType().equals(
				LayerLocation.LocationTypeNames.awss3.toString())) {

			// TODO PLFM-212
			// Potential unique S3 URL Scheme:
			// - dataset id
			// - layer id
			// - layer version
			// - path
			
			String s3Key = entity.getPath().startsWith("/") ? entity.getPath()
					: "/" + entity.getPath();

			if (RequestMethod.GET.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to get the
				// data from S3
				String signedPath = locationHelper.getS3Url(request
						.getParameter(AuthUtilConstants.USER_ID_PARAM),
						s3Key);
				entity.setPath(signedPath);
			} else if (RequestMethod.POST.name().equals(request.getMethod())
					|| RequestMethod.PUT.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to PUT the
				// data to S3
				String signedPath = locationHelper.createS3Url(request
						.getParameter(AuthUtilConstants.USER_ID_PARAM),
						s3Key, entity.getMd5sum());
				entity.setPath(signedPath);
			}
		}
	}

	@Override
	public void validateEntity(LayerLocation entity, EventType eventType)
			throws InvalidModelException {
		if (null == entity.getParentId()) {
			throw new InvalidModelException("parentId cannot be null");
		}
		if (null == entity.getType()) {
			throw new InvalidModelException("type cannot be null");
		}
		if (null == entity.getPath()) {
			throw new InvalidModelException("path cannot be null");
		}
		if (null == entity.getMd5sum()) {
			throw new InvalidModelException("md5sumcannot be null");
		}
	}

	@Override
	public void entityDeleted(LayerLocation deleted) {
		// TODO Auto-generated method stub
		
	}

}
