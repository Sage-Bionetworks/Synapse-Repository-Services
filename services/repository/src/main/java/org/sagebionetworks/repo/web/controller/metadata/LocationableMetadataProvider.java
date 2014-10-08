package org.sagebionetworks.repo.web.controller.metadata;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Provides metadata specific to locationable objects.
 * 
 * @author jmhill
 * 
 */
public class LocationableMetadataProvider implements TypeSpecificMetadataProvider<Entity>, EntityValidator<Entity> {

	public static Log log = LogFactory.getLog(LocationableMetadataProvider.class);

	private static final Pattern MD5_REGEX = Pattern.compile("[0-9a-fA-F]{32}");
	private static final String DEFAULT_MIME_TYPE = "application/binary";
	private static final FileNameMap FILE_EXTENSION2MIME_TYPE_MAP = URLConnection
			.getFileNameMap();

	@Autowired
	LocationHelper locationHelper;

	@Autowired
	EntityManager entityManager;
	
	@Autowired
	AuthorizationManager authorizationManager;

	@Override
	public void validateEntity(Entity entity, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		
		if (!(entity instanceof Locationable)) {
			return;
		}

		// Dev Note: logic for auto-versioning of Locationables when their md5
		// changes can be found in EntityManagerImpl.updateEntity

		Locationable locationable = (Locationable) entity;
		List<LocationData> locations = locationable.getLocations();
		if (null == locations) {
			return;
		}

		int numAwsS3Locations = 0;
		for (LocationData location : locations) {

			if (null == location.getType()) {
				throw new InvalidModelException("type cannot be null");
			}
			if (null == location.getPath()) {
				throw new InvalidModelException("path cannot be null");
			}
			
			//fail if md5 is null (if it's not a link to an external file)
			if (null == locationable.getMd5() && !location.getType().equals(LocationTypeNames.external)) {
				throw new InvalidModelException("md5 cannot be null");
			}
			//also fail if the md5 is set (not null) and it doesn't match the md5 regular expression
			if (null != locationable.getMd5() && !MD5_REGEX.matcher(locationable.getMd5()).matches()) {
				throw new InvalidModelException(
						"md5sum is malformed, it must be a 32 digit hexadecimal string");
			}	
			
			if (null == locationable.getContentType()) {
				// We expect that users typically will not provide a mime
				// type, we look at the file extension here to pick one
				// Note that this is a best effort, if the user has files
				// with different file extensions (but the same md5) this
				// will pick the content type for the last locationLata
				String mimeType = FILE_EXTENSION2MIME_TYPE_MAP
						.getContentTypeFor(location.getPath());
				if (null == mimeType) {
					mimeType = DEFAULT_MIME_TYPE;
				}
				locationable.setContentType(mimeType);
			}

			//
			// If the user passed in a presigned URL in the path, clean it
			// up
			//
			if (location.getType().equals(LocationTypeNames.awss3)) {

				numAwsS3Locations++;

				location.setPath(locationHelper.getS3KeyFromS3Url(location
						.getPath()));

				/*
				 * VERY IMPORTANT, ensure the S3 key starts with the id of this
				 * entity. The reason why this is important is to enforce a
				 * relationship between objects in our S3 bucket and entities in
				 * the repository service for the purposes of authorization. If
				 * there was no relationship, a user could toss the S3 key to
				 * data to which he does not have access into an entity to which
				 * he does have access, and then get a presigned url for that S3
				 * key even though we meant to disallow his access to that data
				 * for that user.
				 */

				// To avoid having to move all of our s3 data, use the entity id without the prefix
				String s3KeyPrefixPattern = "^/" + KeyFactory.stringToKey(locationable.getId())
						+ "/\\d+/.*$";
				if (!location.getPath().matches(s3KeyPrefixPattern)) {
					throw new InvalidModelException(
							"path is malformed, it must match pattern "
									+ s3KeyPrefixPattern);
				}
			}
		}
		if(1 < numAwsS3Locations) {
			throw new InvalidModelException("Only one AWS S3 location is allowed per entity");
		}
	}

	@Override
	public void addTypeSpecificMetadata(Entity entity,
			HttpServletRequest request, UserInfo user, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (!(entity instanceof Locationable)) {
			return;
		}

		Locationable locationable = (Locationable) entity;

		if (!authorizationManager.canAccess(user, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).getAuthorized()) {
			// We used to throw an exception, now we just change a field in
			// the Locationable and null out the locations
			locationable.setLocations(null);
			return;
		}

		// If any of the locations are awss3 locations, provide presigned urls
		String method = request.getParameter(ServiceConstants.METHOD_PARAM);
		Long userId = Long.parseLong(request.getParameter(AuthorizationConstants.USER_ID_PARAM));

		List<LocationData> locations = locationable.getLocations();
		if (null != locations) {
			for (LocationData location : locations) {
				if (location.getType().equals(LocationTypeNames.awss3)) {
					String signedPath = null;
					if ((null != method)
							&& (method.equals(RequestMethod.HEAD.name()))) {
						signedPath = locationHelper.presignS3HEADUrl(userId,
								location.getPath());
					} else {
						signedPath = locationHelper.presignS3GETUrl(userId,
								location.getPath());
					}

					// Overwrite the path with a presigned S3 URL to use to
					// get the
					// data from S3
					location.setPath(signedPath);
				}
			}
		}
	}
}
