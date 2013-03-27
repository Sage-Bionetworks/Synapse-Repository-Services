package org.sagebionetworks.repo.manager.doi;

import java.util.Calendar;

import org.apache.log4j.Logger;
import org.sagebionetworks.doi.DoiClient;
import org.sagebionetworks.doi.EzidAsyncCallback;
import org.sagebionetworks.doi.EzidAsyncClient;
import org.sagebionetworks.doi.EzidConstants;
import org.sagebionetworks.doi.EzidDoi;
import org.sagebionetworks.doi.EzidMetadata;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EntityDoiManagerImpl implements EntityDoiManager {

	private final Logger logger = Logger.getLogger(EntityDoiManagerImpl.class);

	@Autowired private UserManager userManager;
	@Autowired private AuthorizationManager authorizationManager;
	@Autowired private DoiDao doiDao;
	@Autowired private NodeDAO nodeDao;
	@Autowired private UserProfileDAO userProfileDAO;
	private final DoiClient ezidAsyncClient;

	public EntityDoiManagerImpl() {
		ezidAsyncClient = new EzidAsyncClient(new EzidAsyncCallback() {

			@Override
			public void onSuccess(EzidDoi doi) {
				assert doi != null;
				try {
					doiDao.updateDoiStatus(doi.getObjectId(), doi.getDoiObjectType(),
							doi.getObjectVersion(), DoiStatus.READY);
				} catch (DatastoreException e) {
					logger.error(e.getMessage(), e);
				} catch (NotFoundException e) {
					logger.error(e.getMessage(), e);
				}
			}

			@Override
			public void onError(EzidDoi doi, Exception e) {
				assert doi != null;
				try {
					doiDao.updateDoiStatus(doi.getObjectId(), doi.getDoiObjectType(),
							doi.getObjectVersion(), DoiStatus.ERROR);
				} catch (DatastoreException x) {
					logger.error(x.getMessage(), x);
				} catch (NotFoundException x) {
					logger.error(x.getMessage(), x);
				}
			}
		});
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Doi createDoi(final String currentUserName, final String entityId, final Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {

		if (currentUserName == null || currentUserName.isEmpty()) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}
		if (entityId == null) {
			throw new IllegalArgumentException("Entity ID cannot be null");
		}

		// Authorize
		UserInfo currentUser = userManager.getUserInfo(currentUserName);
		UserInfo.validateUserInfo(currentUser);
		String userId = currentUser.getUser().getUserId();
		if (!authorizationManager.canAccess(currentUser, entityId, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException(userId + " lacks change access to the requested object.");
		}

		// If it already exists, no need to create again.
		Doi doiDto = doiDao.getDoi(entityId, DoiObjectType.ENTITY, versionNumber);
		if (doiDto != null) {
			return doiDto;
		}

		// Record the attempt
		String userGroupId = currentUser.getIndividualGroup().getId();
		doiDto = doiDao.createDoi(userGroupId, entityId, DoiObjectType.ENTITY, versionNumber,
				DoiStatus.IN_PROCESS);

		// Create DOI
		EzidDoi ezidDoi = new EzidDoi();
		final String doi = EzidConstants.DOI_PREFIX + entityId;
		ezidDoi.setDoi(doi);
		ezidDoi.setObjectId(entityId);
		ezidDoi.setDoiObjectType(DoiObjectType.ENTITY);
		ezidDoi.setObjectVersion(versionNumber);

		// Find the node. Info will be used in DOI metadata.
		Node node = null;
		if (versionNumber == null) {
			node = nodeDao.getNode(entityId);
		} else {
			node = nodeDao.getNodeForVersion(entityId, versionNumber);
		}
		if (node == null) {
			String error = "Cannot find entity " + entityId;
			if (versionNumber != null) {
				error = error + " for version " + versionNumber;
			}
			throw new NotFoundException(error);
		}

		// Create DOI metadata.
		EzidMetadata metadata = new EzidMetadata();
		Long createdBy = node.getCreatedByPrincipalId();
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		UserProfile userProfile = userProfileDAO.get(createdBy.toString(), schema);
		String creatorName = userProfile.getDisplayName();
		if (creatorName == null) {
			creatorName = userProfile.getOwnerId();
		}
		// TODO: Creator name?
//		System.out.println(userProfile.getDisplayName());
//		System.out.println(userProfile.getEmail());
//		System.out.println(userProfile.getFirstName());
//		System.out.println(userProfile.getLastName());
//		System.out.println(userProfile.getUserName());
//		System.out.println(userProfile.getOwnerId());
		metadata.setCreator(creatorName);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);
		metadata.setPublisher(EzidConstants.PUBLISHER);
		String target = EzidConstants.TARGET_URL_PREFIX + "#!Synapse:" + entityId;
		if (versionNumber != null) {
			target = target + "/version/" + versionNumber;
		}
		metadata.setTarget(target);
		metadata.setTitle(node.getName());
		ezidDoi.setMetadata(metadata);

		// Call EZID to create the DOI
		ezidAsyncClient.create(ezidDoi);

		return doiDto;
	}

	@Override
	public Doi getDoi(String currentUserId, String entityId, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {

		if (currentUserId == null || currentUserId.isEmpty()) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}
		if (entityId == null) {
			throw new IllegalArgumentException("Entity ID cannot be null");
		}

		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		UserInfo.validateUserInfo(currentUser);
		String userName = currentUser.getUser().getUserId();
		if (!authorizationManager.canAccess(currentUser, entityId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName + " lacks change access to the requested object.");
		}

		return doiDao.getDoi(entityId, DoiObjectType.ENTITY, versionNumber);
	}
}
