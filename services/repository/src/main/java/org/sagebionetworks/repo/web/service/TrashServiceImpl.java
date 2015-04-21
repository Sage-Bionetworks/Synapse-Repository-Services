package org.sagebionetworks.repo.web.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.manager.trash.TrashManager.PurgeCallback;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.metadata.EntityProvider;
import org.sagebionetworks.repo.web.service.metadata.MetadataProviderFactory;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificDeleteProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.sagebionetworks.repo.transactions.WriteTransaction;

public class TrashServiceImpl implements TrashService {

	@Autowired
	private UserManager userManager;

	@Autowired
	private TrashManager trashManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private MetadataProviderFactory metadataProviderFactory;

	@Autowired
	private NodeManager nodeManager;

	@Override
	public void moveToTrash(Long currentUserId, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		trashManager.moveToTrash(currentUser, entityId);
	}

	@Override
	public void restoreFromTrash(Long currentUserId, String entityId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		trashManager.restoreFromTrash(currentUser, entityId, newParentId);
	}

	@Override
	public PaginatedResults<TrashedEntity> viewTrashForUser(Long currentUserId, Long userId,
			Long offset, Long limit, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException {

		if (offset == null){
			offset = Long.valueOf(0L);
		}
		if (limit == null){
			limit = Long.valueOf(10L);
		}
		if (offset < 0) {
			throw new IllegalArgumentException(
					"pagination offset must be 0 or greater");
		}
		if (limit < 0) {
			throw new IllegalArgumentException(
					"pagination limit must be 0 or greater");
		}

		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		UserInfo user = userManager.getUserInfo(userId);
		QueryResults<TrashedEntity> trashEntities = trashManager.viewTrashForUser(
				currentUser, user, offset, limit);
		String url = request.getRequestURL() == null ? "" : request.getRequestURL().toString();
		return new PaginatedResults<TrashedEntity>(url, trashEntities.getResults(),
				trashEntities.getTotalNumberOfResults(), offset, limit, null, false);
	}

	@Override
	public PaginatedResults<TrashedEntity> viewTrash(Long currentUserId,
			Long offset, Long limit, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		QueryResults<TrashedEntity> trashEntities = trashManager.viewTrash(
				currentUser, offset, limit);
		String url = request.getRequestURL() == null ? "" : request.getRequestURL().toString();
		return new PaginatedResults<TrashedEntity>(url, trashEntities.getResults(),
				trashEntities.getTotalNumberOfResults(), offset, limit, null, false);
	}

	private class TrashPurgeCallback implements PurgeCallback {
		private List<EntityProvider<Entity>> providers = null;
		private String entityId = null;

		public TrashPurgeCallback() {
		}

		@Override
		public void startPurge(String entityId) {
			this.entityId = entityId;
			providers = null;
			try {
				EntityType type = entityManager.getEntityTypeForDeletion(entityId);

				// Fetch the provider that will validate this entity.
				providers = metadataProviderFactory.getMetadataProvider(type);
			} catch (NotFoundException e) {
				// it's ok if it doesn't exist
			}
		}

		@Override
		public void endPurge() {
			// Do extra cleanup as needed.
			if (entityId != null && providers != null) {
				for (EntityProvider<Entity> provider : providers) {
					if (provider instanceof TypeSpecificDeleteProvider) {
						((TypeSpecificDeleteProvider<Entity>) provider).entityDeleted(entityId);
					}
				}
			}
		}
	}

	@WriteTransaction
	@Override
	public void purgeTrashForUser(Long currentUserId, String entityId) throws DatastoreException, NotFoundException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);

		trashManager.purgeTrashForUser(currentUser, entityId, new TrashPurgeCallback());
	}

	@WriteTransaction
	@Override
	public void purgeTrashForUser(Long currentUserId) throws DatastoreException, NotFoundException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		trashManager.purgeTrashForUser(currentUser, new TrashPurgeCallback());
	}

	@WriteTransaction
	@Override
	public void purgeTrash(Long currentUserId) throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo currentUser = userManager.getUserInfo(currentUserId);
		if (!currentUser.isAdmin()) {
			String currUserId = currentUser.getId().toString();
			throw new UnauthorizedException("Current user " + currUserId + " does not have the permission.");
		}

		trashManager.purgeTrash(currentUser, new TrashPurgeCallback());
	}
}
