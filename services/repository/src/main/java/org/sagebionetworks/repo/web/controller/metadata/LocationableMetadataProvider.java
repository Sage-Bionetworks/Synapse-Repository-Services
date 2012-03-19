package org.sagebionetworks.repo.web.controller.metadata;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.model.Agreement;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationStatusNames;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.ServiceConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Provides metadata specific to locationable objects.
 * 
 * @author jmhill
 * 
 */
public class LocationableMetadataProvider implements
		TypeSpecificMetadataProvider<Entity> {

	public static Log log = LogFactory.getLog(LocationableMetadataProvider.class);

	private static final Pattern MD5_REGEX = Pattern.compile("[0-9a-fA-F]{32}");
	private static final String DEFAULT_MIME_TYPE = "application/binary";
	private static final FileNameMap FILE_EXTENSION2MIME_TYPE_MAP = URLConnection
			.getFileNameMap();

	@Autowired
	LocationHelper locationHelper;

	@Autowired
	NodeQueryDao nodeQueryDao;

	@Autowired
	PermissionsManager permissionsManager;

	@Autowired
	EntityManager entityManager;

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
			if (null == locationable.getMd5()) {
				throw new InvalidModelException("md5 cannot be null");
			}
			if (!MD5_REGEX.matcher(locationable.getMd5()).matches()) {
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

				String s3KeyPrefixPattern = "^/" + locationable.getId()
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

		if (needsToAgreeToEula(user, locationable)) {
			// We used to throw an exception, now we just change a field in
			// the Locationable and null out the locations
			locationable.setLocations(null);
			locationable.setLocationStatus(LocationStatusNames.pendingEula);
			return;
		}

		locationable.setLocationStatus(LocationStatusNames.available);
		// If any of the locations are awss3 locations, provide presigned urls
		String method = request.getParameter(ServiceConstants.METHOD_PARAM);

		List<LocationData> locations = locationable.getLocations();
		if (null != locations) {
			for (LocationData location : locations) {
				if (location.getType().equals(LocationTypeNames.awss3)) {
					String signedPath = null;
					if ((null != method)
							&& (method.equals(RequestMethod.HEAD.name()))) {
						signedPath = locationHelper
								.presignS3HEADUrl(
										request
												.getParameter(AuthorizationConstants.USER_ID_PARAM),
										location.getPath());
					} else {
						signedPath = locationHelper
								.presignS3GETUrl(
										request
												.getParameter(AuthorizationConstants.USER_ID_PARAM),
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

	@Override
	public void entityDeleted(Entity deleted) {
	}

	private boolean needsToAgreeToEula(UserInfo userInfo,
			Locationable locationable) throws NotFoundException,
			DatastoreException, UnauthorizedException {

		if (null == locationable.getLocations()
				|| 0 == locationable.getLocations().size()) {
			// There are not locations to protect
			return false;
		}

		if (permissionsManager.hasAccess(locationable.getId(),
				ACCESS_TYPE.UPDATE, userInfo)) {
			// Users that have sufficient permission to modify the location are
			// not subject to signing the eula because they are either the
			// dataset creator or someone the creator granted write access to
			return false;
		}

		// Users without write access must have agreed to the use
		// agreement in order to see any location info about the dataset or
		// layer
		Dataset dataset = null;
		if (locationable instanceof Layer) {

			// TODO is there a better way to escalate privilege? this is a major hack
			// Dev Note: this method will be nuked soon for the reimpl of governance
			User tempAdminUser = new User();
			tempAdminUser.setUserId("temp@foo.com");

			UserGroup tempAdminGroup = new UserGroup();
			tempAdminGroup.setId("123");
			tempAdminGroup.setName("fake");

			List<UserGroup> tempGroups = new ArrayList<UserGroup>();
			tempGroups.add(tempAdminGroup);

			UserInfo tempAdmin = new UserInfo(true);
			tempAdmin.setUser(tempAdminUser);
			tempAdmin.setGroups(tempGroups);
			tempAdmin.setIndividualGroup(tempAdminGroup);
			
			// Run this as an administrator
			dataset = entityManager.getEntity(tempAdmin, locationable
					.getParentId(), Dataset.class);
			
		} else if (locationable instanceof Dataset) {
			dataset = (Dataset) locationable;
		} else {
			// We do not enforce use agreements on other types of locationable
			// objects
			return false;
		}

		if (null == dataset.getEulaId()) {
			// If the dataset has no Eula, there is nothing to enforce
			return false;
		}

		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.getNodeTypeForClass(Agreement.class));
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(new Expression(new CompoundId(null, "datasetId"),
				Compartor.EQUALS, dataset.getId()));
		filters.add(new Expression(new CompoundId(null, "eulaId"),
				Compartor.EQUALS, dataset.getEulaId()));
		filters.add(new Expression(new CompoundId(null, "createdBy"),
				Compartor.EQUALS, userInfo.getUser().getId()));
		query.setFilters(filters);

		long numAgreements = nodeQueryDao.executeCountQuery(query, userInfo);
		if (1 > numAgreements) {
			// No agreements have been made, one is needed
			return true;
		}
		return false;
	}
}
