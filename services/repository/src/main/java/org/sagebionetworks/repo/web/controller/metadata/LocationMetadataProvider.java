package org.sagebionetworks.repo.web.controller.metadata;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 */
public class LocationMetadataProvider implements
		TypeSpecificMetadataProvider<Location> {

	private static final Pattern MD5_REGEX = Pattern.compile("[0-9a-fA-F]{32}");
	private static final String DEFAULT_MIME_TYPE = "application/binary";
	private static final FileNameMap FILE_EXTENSION2MIME_TYPE_MAP = URLConnection
			.getFileNameMap();

	@Autowired
	LocationHelper locationHelper;

	@Autowired
	EntityManager entityManager;

	@Autowired
	NodeQueryDao nodeQueryDao;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Override
	public void addTypeSpecificMetadata(Location entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {

		// All location types should enforce the use agreement restriction
		checkUseAgreement(userInfo, entity);

		// Special handling for S3 locations
		if (entity.getType()
				.equals(Location.LocationTypeNames.awss3.toString())) {

			if (RequestMethod.GET.name().equals(request.getMethod())) {

				// See if the user wants a pre-signed GET, HEAD, or DELETE
				// request
				String signedPath = null;
				String method = request.getParameter(ServiceConstants.METHOD_PARAM);
				if ((null != method)
						&& (method.equals(RequestMethod.HEAD.name()))) {
					signedPath = locationHelper.getS3HeadUrl(request
							.getParameter(AuthorizationConstants.USER_ID_PARAM),
							entity.getPath());
				} else {
					signedPath = locationHelper.getS3Url(request
							.getParameter(AuthorizationConstants.USER_ID_PARAM),
							entity.getPath());
				}

				// Overwrite the path with a presigned S3 URL to use to get the
				// data from S3
				entity.setPath(signedPath);

			} else if (RequestMethod.POST.name().equals(request.getMethod())
					|| RequestMethod.PUT.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to PUT the
				// data to S3
				String signedPath = locationHelper
						.createS3Url(request
								.getParameter(AuthorizationConstants.USER_ID_PARAM),
								entity.getPath(), entity.getMd5sum(), entity
										.getContentType());
				entity.setPath(signedPath);
			}
		}
	}

	@Override
	public void validateEntity(Location entity, EntityEvent event)
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
		if(!MD5_REGEX.matcher(entity.getMd5sum()).matches()) {
			throw new InvalidModelException("md5sum is malformed, it must be a 32 digit hexadecimal string");
		}
		if (null == entity.getContentType()) {
			// We expect that users typically will not provide a mime type, we
			// look at the file extension here to pick one
			String mimeType = FILE_EXTENSION2MIME_TYPE_MAP
					.getContentTypeFor(entity.getPath());
			if (null == mimeType) {
				mimeType = DEFAULT_MIME_TYPE;
			}
			entity.setContentType(mimeType);
		}
		// Set the path prefix for this location.
		setPathPrefix(entity);
	}

	@Override
	public void entityDeleted(Location deleted) {
		// TODO Auto-generated method stub

	}

	private void setPathPrefix(Location location) {
		// Ensure that awss3 locations are unique by prepending the
		// user-supplied path with a system-controlled prefix
		// - location id (unique per synapse stack)
		// - location version (unique per location)
		// - user-supplied path
		if (location.getType().equals(
				Location.LocationTypeNames.awss3.toString())) {

			String versionLabel = location.getVersionLabel();
			if (versionLabel == null) {
				versionLabel = NodeConstants.DEFAULT_VERSION_LABEL;
			}

			String pathPrefix = "/" + location.getId() + "/" + versionLabel;

			// If this is an update, the user may have passed an S3 URL from a
			// prior GET of a location, scrub the S3 stuff out of the URL. This
			// will have no effect if the path is not an S3 URL
			String path = locationHelper.getS3KeyFromS3Url(location.getPath());

			if (path.startsWith(pathPrefix)) {
				location.setPath(path);
			}
			else {
				String s3Key = pathPrefix
						+ (location.getPath().startsWith("/") ? location
								.getPath() : "/" + location.getPath());
				location.setPath(s3Key);
			}
		}
	}

	private void checkUseAgreement(UserInfo userInfo, Location entity)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (authorizationManager.canAccess(userInfo, entity.getId(),
				ACCESS_TYPE.UPDATE)) {
			// Users that have sufficient permission to modify the location are
			// not subject to signing the eula because they are either the
			// dataset creator or someone the creator granted write access to
			return;
		}

		// Users without write access must have agreed to the use
		// agreement in order to see any location info about the dataset or
		// layer
		ObjectType type = entityManager.getEntityType(userInfo, entity
				.getParentId());
		Dataset dataset = null;
		if (ObjectType.layer == type) {
			Layer layer = (Layer) entityManager.getEntity(userInfo, entity
					.getParentId(), ObjectType.layer.getClassForType());
			dataset = (Dataset) entityManager.getEntity(userInfo, layer
					.getParentId(), ObjectType.dataset.getClassForType());
		} else {
			dataset = (Dataset) entityManager.getEntity(userInfo, entity
					.getParentId(), ObjectType.dataset.getClassForType());
		}

		if (null == dataset.getEulaId()) {
			// If the dataset has no Eula, there is nothing to enforce
			return;
		}

		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.agreement);
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
			throw new UnauthorizedException(
					"The end-user license agreement for dataset '"
							+ dataset.getName()
							+ "' has not yet been agreed to.");
		}
	}

}
