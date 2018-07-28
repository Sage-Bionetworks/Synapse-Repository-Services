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
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityDoiManagerImpl implements EntityDoiManager {

	private final Logger logger = LogManager.getLogger(EntityDoiManagerImpl.class);

	@Autowired private UserManager userManager;
	@Autowired private AuthorizationManager authorizationManager;
	@Autowired private DoiDao doiDao;
	@Autowired private NodeDAO nodeDao;
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
	@NewWriteTransaction
	@Override
	public Doi createDoi(final Long userId, final String entityId, final Long versionNumber)
			throws NotFoundException, UnauthorizedException {

		if (userId == null) {
			throw new IllegalArgumentException("User name cannot be null or empty.");
		}
		if (entityId == null) {
			throw new IllegalArgumentException("Entity ID cannot be null");
		}

		// Authorize
		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(currentUser, entityId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE));

		Doi doiDto;
		try {		// If it already exists with no error, no need to create again.
			doiDto = doiDao.getDoi(entityId, ObjectType.ENTITY, versionNumber);
		} catch (NotFoundException e) { // TODO: Switch behavior to not swallow exception?
			doiDto = null;
		}
		if (doiDto != null && !DoiStatus.ERROR.equals(doiDto.getDoiStatus())) {
			return doiDto;
		}

		// Find the node. Make sure the node exists. Node info will be used in DOI metadata.
		final Node node = getNode(entityId, versionNumber);

		// Record the attempt. This is where we draw the transaction boundary.
		if (doiDto == null) { // Create a new entry
			String userGroupId = currentUser.getId().toString();
			doiDto = new Doi();
			doiDto.setCreatedBy(userGroupId);
			doiDto.setObjectId(entityId);
			doiDto.setObjectType(ObjectType.ENTITY);
			doiDto.setObjectVersion(versionNumber);
			doiDto.setDoiStatus(DoiStatus.IN_PROCESS);
			doiDto = doiDao.createDoi(doiDto);
		} else { // Attempt to reregister an "Error" entry.
			if (!doiDao.getEtagForUpdate(doiDto.getObjectId(), doiDto.getObjectType(), doiDto.getObjectVersion()).equals(doiDto.getEtag())) {
				throw new ConflictingUpdateException("Etags do not match.");
			}
			doiDto = doiDao.updateDoiStatus(doiDto.getId(), DoiStatus.IN_PROCESS);
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
					if (!doiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()).equals(dto.getEtag())) {
						throw new ConflictingUpdateException("Etags do not match.");
					}
					doiDao.updateDoiStatus(dto.getId(), DoiStatus.CREATED);
				} catch (DatastoreException | NotFoundException e) {
					logger.error(e.getMessage(), e);
				}
			}

			@Override
			public void onError(EzidDoi doi, Exception e) {
				assert doi != null;
				try {
					logger.error(e.getMessage(), e);
					Doi dto = doi.getDto();
					if (!doiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()).equals(dto.getEtag())) {
						throw new ConflictingUpdateException("Etags do not match.");
					}
					doiDao.updateDoiStatus(dto.getId(), DoiStatus.ERROR);
				} catch (DatastoreException | NotFoundException x) {
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
					doiDto = doiDao.getDoi(doiDto.getId());
					if (!doiDao.getEtagForUpdate(doiDto.getObjectId(), doiDto.getObjectType(), doiDto.getObjectVersion()).equals(doiDto.getEtag())) {
						throw new ConflictingUpdateException("Etags do not match.");
					}
					doiDao.updateDoiStatus(doiDto.getId(), DoiStatus.READY);
				} catch (DatastoreException | NotFoundException e) {
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
	public Doi getDoiForVersion(Long userId, String entityId, Long versionNumber)
			throws NotFoundException, UnauthorizedException {

		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}
		if (entityId == null) {
			throw new IllegalArgumentException("Entity ID cannot be null");
		}

		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(currentUser, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ));

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

	@Override
	public Doi getDoiForCurrentVersion(Long userId, String entityId)
			throws NotFoundException, UnauthorizedException {

		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}
		if (entityId == null) {
			throw new IllegalArgumentException("Entity ID cannot be null");
		}

		UserInfo currentUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(currentUser);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(currentUser, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		Node node = getNode(entityId, null);
		Long versionNumber = null;
		// Versionables such as files should have the null versionNumber converted into non-null versionNumber
		if (node.getNodeType() == EntityType.file || node.getNodeType() == EntityType.table) { // TODO: Are these the only versionables?
			versionNumber = getNode(entityId, null).getVersionNumber();
		}
		return doiDao.getDoi(entityId, ObjectType.ENTITY, versionNumber);
	}
}
