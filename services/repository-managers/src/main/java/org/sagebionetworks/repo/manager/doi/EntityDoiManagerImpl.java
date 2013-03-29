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
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.NotFoundException;
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
					Doi dto = doi.getDto();
					doiDao.updateDoiStatus(dto.getObjectId(), dto.getDoiObjectType(),
							dto.getObjectVersion(), DoiStatus.READY, dto.getEtag());
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
					Doi dto = doi.getDto();
					doiDao.updateDoiStatus(dto.getObjectId(), dto.getDoiObjectType(),
							dto.getObjectVersion(), DoiStatus.ERROR, dto.getEtag());
				} catch (DatastoreException x) {
					logger.error(x.getMessage(), x);
				} catch (NotFoundException x) {
					logger.error(x.getMessage(), x);
				}
			}
		});
	}

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

		// Find the node. Info will be used in DOI metadata.
		final Node node = getNode(entityId, versionNumber);

		// Record the attempt. This is where we draw the transaction boundary.
		String userGroupId = currentUser.getIndividualGroup().getId();
		doiDto = doCreateTransaction(userGroupId, entityId, versionNumber);

		// Create DOI string
		EzidDoi ezidDoi = new EzidDoi();
		ezidDoi.setDto(doiDto);
		final String doi = EzidConstants.DOI_PREFIX + entityId;
		ezidDoi.setDoi(doi);

		// Create DOI metadata.
		EzidMetadata metadata = new EzidMetadata();
		Long principalId = node.getCreatedByPrincipalId();
		String creatorName = userManager.getDisplayName(principalId);
		// Display name is optional
		if (creatorName == null || creatorName.isEmpty()) {
			creatorName = EzidConstants.DEFAULT_CREATOR;
		}
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

	/**
	 * Limits the transaction boundary to within the DOI DAO and never run me within another transaction.
	 * DOI client creating the DOI is an asynchronous call and must happen outside the transaction to
	 * avoid race conditions.
	 */
	@Transactional(readOnly = false, propagation = Propagation.NEVER)
	private Doi doCreateTransaction(String userGroupId, String entityId, Long versionNumber) {
		return doiDao.createDoi(userGroupId, entityId, DoiObjectType.ENTITY, versionNumber, DoiStatus.IN_PROCESS);
	}

	/** Gets the node whose information will be used in DOI metadata. */
	private Node getNode(String entityId, Long versionNumber) throws NotFoundException {
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
		return node;
	}
}
