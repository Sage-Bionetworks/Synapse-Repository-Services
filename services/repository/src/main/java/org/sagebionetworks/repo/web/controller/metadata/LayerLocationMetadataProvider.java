package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
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

			if (RequestMethod.GET.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to get the
				// data from S3
				String signedPath = locationHelper.getS3Url(request
						.getParameter(AuthUtilConstants.USER_ID_PARAM),
						entity.getPath());
				entity.setPath(signedPath);
			} else if (RequestMethod.POST.name().equals(request.getMethod())
					|| RequestMethod.PUT.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to PUT the
				// data to S3
				String signedPath = locationHelper.createS3Url(request
						.getParameter(AuthUtilConstants.USER_ID_PARAM),
						entity.getPath(), entity.getMd5sum());
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
			throw new InvalidModelException("md5sum cannot be null");
		}
		// Set the path prefix for this location.
		setPathPrefix(entity);
	}

	@Override
	public void entityDeleted(LayerLocation deleted) {
		// TODO Auto-generated method stub
		
	}
	// this code was moved from EntityManagerImpl
	private void setPathPrefix(LayerLocation location) {
		// Ensure that awss3 locations are unique by prepending the user-supplied path with a system-controlled prefix
		// - location id (unique per synapse stack)
		// - location version (unique per location)
		// - user-supplied path
		if(location.getType().equals(
				LayerLocation.LocationTypeNames.awss3.toString())) {
			
			String versionLabel = location.getVersionLabel();
			if(versionLabel == null){
				versionLabel = NodeConstants.DEFAULT_VERSION_LABEL;
			}

			String pathPrefix = "/" + location.getId() + "/" + versionLabel;

			// If this is an update, the user may or may not have changed the path member of this object
			if(!location.getPath().startsWith(pathPrefix)) {
				String s3Key = pathPrefix + (location.getPath().startsWith("/") ? location.getPath()
						: "/" + location.getPath());
				location.setPath(s3Key);
			}
		}
	}

}
