package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.util.LocationHelpersImpl;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 */
public class LayerLocationMetadataProvider implements
		TypeSpecificMetadataProvider<LayerLocation> {

	LocationHelper locationHelper = new LocationHelpersImpl();

	@Override
	public void addTypeSpecificMetadata(LayerLocation entity,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException {

		// Special handling for S3 locations
		if (entity.getType().equals(
				LayerLocation.LocationTypeNames.awss3.toString())) {

			// S3 URL Scheme:
			// - dataset id
			// - layer id
			// - layer version
			// - path
			String uniquePath = entity.getParentId() + "/" + entity.getId()
					+ "/"
					// TODO + entity.getVersion() + "/"
					+ entity.getPath();

			if (RequestMethod.GET.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to get the
				// data from S3
				String signedPath = locationHelper.getS3Url(request
						.getParameter(AuthUtilConstants.USER_ID_PARAM),
						uniquePath);
				entity.setPath(signedPath);
			} else if (RequestMethod.POST.name().equals(request.getMethod())
					|| RequestMethod.PUT.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to PUT the
				// data to S3
				String signedPath = locationHelper.createS3Url(request
						.getParameter(AuthUtilConstants.USER_ID_PARAM),
						uniquePath, entity.getMd5sum());
				entity.setPath(signedPath);
			}
		}

	}

	@Override
	public void validateEntity(LayerLocation entity)
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

}
