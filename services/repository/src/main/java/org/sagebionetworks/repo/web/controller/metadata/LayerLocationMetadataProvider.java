package org.sagebionetworks.repo.web.controller.metadata;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 */
public class LayerLocationMetadataProvider implements
		TypeSpecificMetadataProvider<LayerLocation> {

	@Autowired
	LocationHelper locationHelper;

	@Autowired
	EntityManager entityManager;

	@Autowired
	NodeQueryDao nodeQueryDao;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Override
	public void addTypeSpecificMetadata(LayerLocation entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws DatastoreException, NotFoundException, UnauthorizedException {

		// All location types should enforce the use agreement restriction
		checkUseAgreement(userInfo, entity);

		// Special handling for S3 locations
		if (entity.getType().equals(
				LayerLocation.LocationTypeNames.awss3.toString())) {

			if (RequestMethod.GET.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to get the
				// data from S3
				String signedPath = locationHelper.getS3Url(request
						.getParameter(AuthUtilConstants.USER_ID_PARAM), entity
						.getPath());
				entity.setPath(signedPath);
			} else if (RequestMethod.POST.name().equals(request.getMethod())
					|| RequestMethod.PUT.name().equals(request.getMethod())) {
				// Overwrite the path with a presigned S3 URL to use to PUT the
				// data to S3
				String signedPath = locationHelper.createS3Url(request
						.getParameter(AuthUtilConstants.USER_ID_PARAM), entity
						.getPath(), entity.getMd5sum());
				entity.setPath(signedPath);
			}
		}
	}

	@Override
	public void validateEntity(LayerLocation entity, UserInfo userInfo,
			EventType eventType) throws InvalidModelException {
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

	private void setPathPrefix(LayerLocation location) {
		// Ensure that awss3 locations are unique by prepending the
		// user-supplied path with a system-controlled prefix
		// - location id (unique per synapse stack)
		// - location version (unique per location)
		// - user-supplied path
		if (location.getType().equals(
				LayerLocation.LocationTypeNames.awss3.toString())) {

			String versionLabel = location.getVersionLabel();
			if (versionLabel == null) {
				versionLabel = NodeConstants.DEFAULT_VERSION_LABEL;
			}

			String pathPrefix = "/" + location.getId() + "/" + versionLabel;

			// If this is an update, the user may or may not have changed the
			// path member of this object
			if (!location.getPath().startsWith(pathPrefix)) {
				String s3Key = pathPrefix
						+ (location.getPath().startsWith("/") ? location
								.getPath() : "/" + location.getPath());
				location.setPath(s3Key);
			}
		}
	}

	private void checkUseAgreement(UserInfo userInfo, LayerLocation entity)
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
			InputDataLayer layer = (InputDataLayer) entityManager.getEntity(
					userInfo, entity.getParentId(), ObjectType.layer
							.getClassForType());
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
