package org.sagebionetworks.repo.manager.doi;

import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.doi.DoiAsyncClient;
import org.sagebionetworks.doi.DxAsyncCallback;
import org.sagebionetworks.doi.DxAsyncClient;
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
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityDoiManagerImpl implements EntityDoiManager {

	private final Logger logger = LogManager.getLogger(EntityDoiManagerImpl.class);

	@Autowired private UserManager userManager;
	@Autowired private AuthorizationManager authorizationManager;
	@Autowired private DoiDao doiDao;
	@Autowired private NodeDAO nodeDao;;
	private final DoiAsyncClient ezidAsyncClient;
	private final DxAsyncClient dxAsyncClient;

	public EntityDoiManagerImpl() {
		ezidAsyncClient = new EzidAsyncClient();
		dxAsyncClient = new DxAsyncClient();
	}

	/**
	 * Limits the transaction boundary to within the DOI DAO and runs with a new transaction.
	 * DOI client creating the DOI is an asynchronous call and must happen outside the transaction to
	 * avoid race conditions.
	 */
	@Override
	public Doi createDoi(final Long userId, final String entityId, final Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}
		if (entityId == null) {
			throw new IllegalArgumentException("Entity ID cannot be null");
		}

		// Authorize
		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		if (!authorizationManager.canAccess(currentUser, entityId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException(userId + " lacks change access to the requested object.");
		}

		// If it already exists with no error, no need to create again.
		Doi doiDto = null;
		try {
			doiDto = doiDao.getDoi(entityId, ObjectType.ENTITY, versionNumber);
		} catch (NotFoundException e) {
			doiDto = null;
		}
		if (doiDto != null && !DoiStatus.ERROR.equals(doiDto.getDoiStatus())) {
			return doiDto;
		}

		// Find the node. Make sure the node exists. Node info will be used in DOI metadata.
		final Node node = getNode(entityId, versionNumber);

		// Record the attempt. This is where we draw the transaction boundary.
		if (doiDto == null) {
			String userGroupId = currentUser.getId().toString();
			doiDto = doiDao.createDoi(userGroupId, entityId, ObjectType.ENTITY, versionNumber, DoiStatus.IN_PROCESS);
		} else {
			doiDto = doiDao.updateDoiStatus(entityId, ObjectType.ENTITY, versionNumber, DoiStatus.IN_PROCESS, doiDto.getEtag());
		}

		// Create DOI string
		EzidDoi ezidDoi = new EzidDoi();
		ezidDoi.setDto(doiDto);
		String doi = EzidConstants.DOI_PREFIX + entityId;
		if (versionNumber != null) {
			doi = doi + "." + versionNumber;
		}
		ezidDoi.setDoi(doi);

		// Create DOI metadata.
		EzidMetadata metadata = new EzidMetadata();
		String creatorName = EzidConstants.DEFAULT_CREATOR;
		metadata.setCreator(creatorName);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);
		metadata.setPublisher(EzidConstants.PUBLISHER);
		String target = EzidConstants.TARGET_URL_PREFIX + entityId;
		if (versionNumber != null) {
			target = target + "/version/" + versionNumber;
		}
		metadata.setTarget(target);
		metadata.setTitle(node.getName());
		ezidDoi.setMetadata(metadata);

		// Call EZID to create the DOI
		ezidAsyncClient.create(ezidDoi, new EzidAsyncCallback() {

			@Override
			public void onSuccess(EzidDoi doi) {
				assert doi != null;
				try {
					Doi dto = doi.getDto();
					doiDao.updateDoiStatus(dto.getObjectId(), dto.getObjectType(),
							dto.getObjectVersion(), DoiStatus.CREATED, dto.getEtag());
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
					logger.error(e.getMessage(), e);
					Doi dto = doi.getDto();
					doiDao.updateDoiStatus(dto.getObjectId(), dto.getObjectType(),
							dto.getObjectVersion(), DoiStatus.ERROR, dto.getEtag());
				} catch (DatastoreException x) {
					logger.error(x.getMessage(), x);
				} catch (NotFoundException x) {
					logger.error(x.getMessage(), x);
				}
			}
		});

		// Now calls the DOI resolution service to check if the DOI is ready for use
		dxAsyncClient.resolve(ezidDoi, new DxAsyncCallback() {

			@Override
			public void onSuccess(EzidDoi ezidDoi) {
				try {
					Doi doiDto = ezidDoi.getDto();
					doiDto = doiDao.getDoi(doiDto.getObjectId(), doiDto.getObjectType(),
							doiDto.getObjectVersion());
					doiDao.updateDoiStatus(doiDto.getObjectId(),
							doiDto.getObjectType(), doiDto.getObjectVersion(),
							DoiStatus.READY, doiDto.getEtag());
				} catch (DatastoreException e) {
					logger.error(e.getMessage(), e);
				} catch (NotFoundException e) {
					logger.error(e.getMessage(), e);
				}
			}

			@Override
			public void onError(EzidDoi ezidDoi, Exception e) {
				logger.error(e.getMessage(), e);
			}
		});

		return doiDto;
	}

	@Override
	public Doi getDoi(Long userId, String entityId, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}
		if (entityId == null) {
			throw new IllegalArgumentException("Entity ID cannot be null");
		}

		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		if (!authorizationManager.canAccess(currentUser, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userId + " lacks change access to the requested object.");
		}

		return doiDao.getDoi(entityId, ObjectType.ENTITY, versionNumber);
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
