package org.sagebionetworks.repo.web.service;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.sts.StsManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.metadata.AllTypesValidator;
import org.sagebionetworks.repo.web.service.metadata.EntityEvent;
import org.sagebionetworks.repo.web.service.metadata.EntityProvider;
import org.sagebionetworks.repo.web.service.metadata.EntityValidator;
import org.sagebionetworks.repo.web.service.metadata.EventType;
import org.sagebionetworks.repo.web.service.metadata.MetadataProviderFactory;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificCreateProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificDeleteProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificMetadataProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificUpdateProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificVersionDeleteProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation for REST controller for CRUD operations on Entity DTOs and Entity
 * DAOs
 * <p>
 * 
 * This class performs the basic CRUD operations for all our DAO-backed model
 * objects. See controllers specific to particular models for any special
 * handling.
 * 
 * @author deflaux
 * @param <T>
 *            the particular type of entity the controller is managing
 */
public class EntityServiceImpl implements EntityService {
	public static final Integer DEFAULT_LIMIT = 10;
	public static final Integer DEFAULT_OFFSET = 0;
	
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	private StsManager stsManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private MetadataProviderFactory metadataProviderFactory;
	@Autowired
	private AllTypesValidator allTypesValidator;
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Override
	public PaginatedResults<VersionInfo> getAllVersionsOfEntity(
			Long userId, Integer offset, Integer limit, String entityId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		if(offset == null){
			offset = DEFAULT_OFFSET;
		}
		if(limit == null){
			limit = DEFAULT_LIMIT;
		}
		ServiceConstants.validatePaginationParams((long)offset, (long)limit);
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<VersionInfo> versions = entityManager.getVersionsOfEntity(userInfo, entityId, (long)offset, (long)limit);
		return PaginatedResults.createWithLimitAndOffset(versions, (long)limit, (long)offset);
	}

	@Override
	public <T extends Entity> T getEntity(Long userId, String id, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);
		UserInfo userInfo = userManager.getUserInfo(userId);
		return getEntity(userInfo, entityId, clazz, EventType.GET);
	}
	
	@Override
	public Entity getEntity(UserInfo userInfo, String id) throws NotFoundException, DatastoreException, UnauthorizedException {
		EntityHeader header = entityManager.getEntityHeader(userInfo, id);
		EntityType type = EntityTypeUtils.getEntityTypeForClassName(header.getType());
		return getEntity(userInfo, id, EntityTypeUtils.getClassForType(type), EventType.GET);
	}
	/**
	 * Any time we fetch an entity we do so through this path.
	 * @param <T>
	 * @param info
	 * @param id
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntity(UserInfo info, String id, Class<? extends T> clazz, EventType eventType) throws NotFoundException, DatastoreException, UnauthorizedException{
		// Determine the object type from the url.
		EntityType type = EntityTypeUtils.getEntityTypeForClass(clazz);
		T entity = entityManager.getEntity(info, id, clazz);
		// Do all of the type specific stuff.
		this.doAddServiceSpecificMetadata(info, entity, type, eventType);
		return entity;
	}

	/**
	 * Do all type specific stuff to an entity
	 * @param <T>
	 * @param info
	 * @param entity
	 * @param type
	 * @param eventType
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	private <T extends Entity> void doAddServiceSpecificMetadata(UserInfo info, T entity, EntityType type, EventType eventType) throws DatastoreException, NotFoundException, UnauthorizedException{
		// Fetch the provider that will validate this entity.
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(type);

		// Add the type specific metadata
		if(providers != null) {
			for (EntityProvider<? extends Entity> provider : providers) {
				if (provider instanceof TypeSpecificMetadataProvider) {
					((TypeSpecificMetadataProvider) provider).addTypeSpecificMetadata(entity, info, eventType);
				}
			}
		}
	}
	
	public <T extends Entity> T getEntityForVersion(UserInfo info, String id, Long versionNumber,
													Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Determine the object type from the url.
		EntityType type = EntityTypeUtils.getEntityTypeForClass(clazz);
		T entity = entityManager.getEntityForVersion(info, id, versionNumber, clazz);
		// Do all of the type specific stuff.
		this.doAddServiceSpecificMetadata(info, entity, type, EventType.GET);
		return entity;
	}
	
	@Override
	public Entity getEntityForVersion(UserInfo userInfo, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		EntityType type = entityManager.getEntityType(userInfo, id);
		return getEntityForVersion(userInfo, id, versionNumber, EntityTypeUtils.getClassForType(type));
	}

	@Override
	public List<EntityHeader> getEntityHeaderByMd5(Long userId, String md5)
			throws NotFoundException, DatastoreException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<EntityHeader> entityHeaders = entityManager.getEntityHeaderByMd5(userInfo, md5);
		return entityHeaders;
	}

	@WriteTransaction
	@Override
	public <T extends Entity> T createEntity(UserInfo userInfo, T newEntity, String activityId)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException {
		// Determine the object type from the url.
		Class<? extends T> clazz = (Class<? extends T>) newEntity.getClass();
		EntityType type = EntityTypeUtils.getEntityTypeForClass(newEntity.getClass());
		// Fetch the provider that will validate this entity.
		// Get the user
		EventType eventType = EventType.CREATE;
		// Fire the event
		fireValidateEvent(userInfo, eventType, newEntity, type);
		String id = entityManager.createEntity(userInfo, newEntity, activityId);
		newEntity.setId(id);
		fireAfterCreateEntityEvent(userInfo, newEntity, type);
		// Return the resulting entity.
		return getEntity(userInfo, id, clazz, eventType);
	}

	/**
	 * Fire an after a create event.
	 * @param userInfo
	 * @param eventType
	 * @param entity
	 * @param type
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	private void fireAfterCreateEntityEvent(UserInfo userInfo, Entity entity, EntityType type) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(type);
		if(providers != null) {
			for (EntityProvider<? extends Entity> provider : providers) {
				if (provider instanceof TypeSpecificCreateProvider) {
					((TypeSpecificCreateProvider) provider).entityCreated(userInfo, entity);
				}
			}
		}
	}
	
	/**
	 * Fire an after an update event.
	 * @param userInfo
	 * @param eventType
	 * @param entity
	 * @param type
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	private void fireAfterUpdateEntityEvent(UserInfo userInfo, Entity entity, EntityType type, boolean wasNewVersionCreated) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(type);
		if(providers != null) {
			for (EntityProvider<? extends Entity> provider : providers) {
				if (provider instanceof TypeSpecificUpdateProvider) {
					((TypeSpecificUpdateProvider) provider).entityUpdated(userInfo, entity, wasNewVersionCreated);
				}
			}
		}
	}
	
	/**
	 * Fire a validate event.  
	 * @param userInfo
	 * @param eventType
	 * @param entity
	 * @param type
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	private void fireValidateEvent(UserInfo userInfo, EventType eventType, Entity entity, EntityType type) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		List<EntityHeader> newPath = null;
		if (entity.getParentId() != null) {
			newPath = entityManager.getEntityPathAsAdmin(entity.getParentId());
		}
		EntityEvent event = new EntityEvent(eventType, newPath, userInfo);
		
		// First apply validation that is common to all types.
		allTypesValidator.validateEntity(entity, event);
		// Now validate for a specific type.
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(type);
		// Validate the entity
		if(providers != null) {
			for (EntityProvider<? extends Entity> provider : providers) {
				if (provider instanceof EntityValidator) {
					((EntityValidator) provider).validateEntity(entity, event);
				}
			}
		}
	}
	
	@WriteTransaction
	@Override
	public <T extends Entity> T updateEntity(UserInfo userInfo,
											 T updatedEntity, boolean newVersion, String activityId)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {
		if(updatedEntity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(updatedEntity.getId() == null) throw new IllegalArgumentException("Updated Entity cannot have a null id");
		// Get the type for this entity.
		EntityType type = EntityTypeUtils.getEntityTypeForClass(updatedEntity.getClass());
		Class<? extends T> clazz = (Class<? extends T>) updatedEntity.getClass();
		// Fetch the provider that will validate this entity.
		// Get the user
		EventType eventType = EventType.UPDATE;
		// Fire the event
		fireValidateEvent(userInfo, eventType, updatedEntity, type);
		// Keep the entity id
		String entityId = updatedEntity.getId();
		// Now do the update
		boolean wasNewVersionCrated = entityManager.updateEntity(userInfo, updatedEntity, newVersion, activityId);
		fireAfterUpdateEntityEvent(userInfo, updatedEntity, type, wasNewVersionCrated);
		// Return the updated entity
		return getEntity(userInfo, entityId, clazz, eventType);
	}
	
	@WriteTransaction
	@Override
	public void deleteEntity(Long userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType type = entityManager.getEntityType(userInfo, id);
		deleteEntity(userId, entityId, EntityTypeUtils.getClassForType(type));
	}

	@WriteTransaction
	@Override
	public <T extends Entity> void deleteEntity(Long userId, String id, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		// First get the entity we are deleting
		EntityType type = EntityTypeUtils.getEntityTypeForClass(clazz);
		// Fetch the provider that will validate this entity.
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(type);
		entityManager.deleteEntity(userInfo, entityId);
		// Do extra cleanup as needed.
		if(providers != null) {
			for(EntityProvider<? extends Entity> provider : providers) {
				if (provider instanceof TypeSpecificDeleteProvider) {
					((TypeSpecificDeleteProvider) provider).entityDeleted(entityId);
				}
			}
		}
		return;
	}
	
	@WriteTransaction
	@Override
	public void deleteEntityVersion(Long userId, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType type = entityManager.getEntityType(userInfo, id);
		deleteEntityVersion(userId, entityId, versionNumber, EntityTypeUtils.getClassForType(type));
	}
	
	@WriteTransaction
	@Override
	public <T extends Entity> void deleteEntityVersion(Long userId, String id,
			Long versionNumber, Class<? extends Entity> classForType) throws DatastoreException, NotFoundException, UnauthorizedException, ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// First get the entity we are deleting
		EntityType type = EntityTypeUtils.getEntityTypeForClass(classForType);
		// Fetch the provider that will validate this entity.
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(type);
		entityManager.deleteEntityVersion(userInfo, id, versionNumber);
		// Do extra cleanup as needed.
		if(providers != null) {
			for (EntityProvider<? extends Entity> provider : providers) {
				if (provider instanceof TypeSpecificVersionDeleteProvider) {
					((TypeSpecificVersionDeleteProvider) provider).entityVersionDeleted(id, versionNumber);
				}
			}
		}
	}

	@Override
	public Annotations getEntityAnnotations(UserInfo userInfo, String id) throws NotFoundException, DatastoreException, UnauthorizedException {
		return entityManager.getAnnotations(userInfo, id);
	}
	
	@Override
	public Annotations getEntityAnnotationsForVersion(UserInfo userInfo, String id,
													  Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return entityManager.getAnnotationsForVersion(userInfo, id, versionNumber);
	}

	@WriteTransaction
	@Override
	public Annotations updateEntityAnnotations(UserInfo userInfo, String entityId,
											   Annotations updatedAnnotations) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Annotations must have a non-null id");
		entityManager.updateAnnotations(userInfo,entityId, updatedAnnotations);
		return entityManager.getAnnotations(userInfo, updatedAnnotations.getId());
	}

	@WriteTransaction
	@Override
	public AccessControlList createEntityACL(UserInfo userInfo, AccessControlList newACL) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ConflictingUpdateException {
		AccessControlList acl = entityPermissionsManager.overrideInheritance(newACL, userInfo);
		return acl;
	}

	@Override
	public  AccessControlList getEntityACL(String entityId, UserInfo userInfo)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException {
		// First try the updated
		AccessControlList acl = entityPermissionsManager.getACL(entityId, userInfo);
		

		return acl;
	}

	@WriteTransaction
	@Override
	public AccessControlList updateEntityACL(UserInfo userInfo, AccessControlList updated) throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		return entityPermissionsManager.updateACL(updated, userInfo);
	}

	@WriteTransaction
	@Override
	public AccessControlList createOrUpdateEntityACL(UserInfo userInfo, AccessControlList acl) throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String entityId = acl.getId();
		if (entityPermissionsManager.hasLocalACL(entityId)) {
			// Local ACL exists; update it
			return updateEntityACL(userInfo, acl);
		} else {
			// Local ACL does not exist; create it
			return createEntityACL(userInfo, acl);
		}
	}
	
	@WriteTransaction
	@Override
	public void deleteEntityACL(Long userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		entityPermissionsManager.restoreInheritance(id, userInfo);
	}

	@Override
	public boolean hasAccess(String entityId, Long userId, String accessType)
		throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityPermissionsManager.hasAccess(entityId, ACCESS_TYPE.valueOf(accessType), userInfo).isAuthorized();
	}

	@Override
	public List<EntityHeader> getEntityPath(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException, UnauthorizedException {
		return entityManager.getEntityPath(userInfo, entityId);
	}

	@Override
	public EntityHeader getEntityHeader(Long userId, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.getEntityHeader(userInfo, entityId);
	}
	
	@Override
	public PaginatedResults<EntityHeader> getEntityHeader(Long userId,
			List<Reference> references) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<EntityHeader> headers = entityManager.getEntityHeader(userInfo, references);
		return PaginatedResults.createMisusedPaginatedResults(headers);
	}

	@Override
	public <T extends Entity> EntityHeader getEntityBenefactor(String entityId, Long userId) throws NotFoundException,
			DatastoreException, UnauthorizedException, ACLInheritanceException {
		if(entityId == null) throw new IllegalArgumentException("EntityId cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// First get the permissions benefactor
		String benefactor = entityPermissionsManager.getPermissionBenefactor(entityId, userInfo);
		return getEntityHeader(userId, benefactor);
	}

	@Override
	public UserEntityPermissions getUserEntityPermissions(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException {
		return entityPermissionsManager.getUserPermissionsForEntity(userInfo, entityId);
	}

	@Override
	public boolean doesEntityHaveChildren(UserInfo userInfo, String entityId) throws DatastoreException,
			ParseException, NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("EntityId cannot be null");
		if(userInfo == null) throw new IllegalArgumentException("userInfo cannot be null");
		return entityManager.doesEntityHaveChildren(userInfo, entityId);
	}
	
	@Override
	public Activity getActivityForEntity(Long userId, String entityId) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.getActivityForEntity(userInfo, entityId, null);
	}
	
	@Override
	public Activity getActivityForEntity(Long userId, String entityId, Long versionNumber) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.getActivityForEntity(userInfo, entityId, versionNumber);
	}

	@WriteTransaction
	@Override
	public Activity setActivityForEntity(Long userId, String entityId,
										 String activityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		if(activityId == null) throw new IllegalArgumentException("Activity Id can not be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.setActivityForEntity(userInfo, entityId, activityId);
	}


	@WriteTransaction
	@Override
	public void deleteActivityForEntity(Long userId, String entityId) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		entityManager.deleteActivityForEntity(userInfo, entityId);
	}

	@Override
	public String getFileRedirectURLForCurrentVersion(Long userId, String id) throws DatastoreException, NotFoundException {
		if(id == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId = entityManager.getFileHandleIdForVersion(userInfo, id, null);
		
		// Use the FileHandle ID to get the URL
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(FileHandleAssociateType.FileEntity, id);
		
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}
	
	@Override
	public String getFilePreviewRedirectURLForCurrentVersion(Long userId, String entityId) throws DatastoreException, NotFoundException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId = entityManager.getFileHandleIdForVersion(userInfo, entityId, null);
		// Look up the preview for this file.
		String previewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);

		// Use the FileHandle ID to get the URL
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, previewId)
				.withAssociation(FileHandleAssociateType.FileEntity, entityId);
		
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}

	@Override
	public String getFileRedirectURLForVersion(Long userId, String id, Long versionNumber) throws DatastoreException, NotFoundException {
		ValidateArgument.required(id, "Entity Id");
		ValidateArgument.required(userId, "UserId");
		ValidateArgument.required(versionNumber, "versionNumber");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId = entityManager.getFileHandleIdForVersion(userInfo, id, versionNumber);
		// Use the FileHandle ID to get the URL
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(FileHandleAssociateType.FileEntity, id);
				
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}


	@Override
	public String getFilePreviewRedirectURLForVersion(Long userId, String id, Long versionNumber) throws DatastoreException, NotFoundException {
		ValidateArgument.required(id, "Entity Id");
		ValidateArgument.required(userId, "UserId");
		ValidateArgument.required(versionNumber, "versionNumber");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId = entityManager.getFileHandleIdForVersion(userInfo, id, versionNumber);
		// Look up the preview for this file.
		String previewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		// Use the FileHandle ID to get the URL
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, previewId)
				.withAssociation(FileHandleAssociateType.FileEntity, id);
				
		return fileHandleManager.getRedirectURLForFileHandle(urlRequest);
	}
	
	@Override
	public FileHandleResults getEntityFileHandlesForCurrentVersion(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userInfo == null) throw new IllegalArgumentException("userInfo cannot be null");
		// Get the file handle.
		String fileHandleId = entityManager.getFileHandleIdForVersion(userInfo, entityId, null);
		List<String> idsList = new LinkedList<String>();
		idsList.add(fileHandleId);
		return fileHandleManager.getAllFileHandles(idsList, true);
	}

	@Override
	public FileHandleResults getEntityFileHandlesForVersion(UserInfo userInfo, String entityId, Long versionNumber) throws DatastoreException, NotFoundException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userInfo == null) throw new IllegalArgumentException("userInfo cannot be null");
		if(versionNumber == null) throw new IllegalArgumentException("versionNumber cannot be null");
		// Get the file handle.
		String fileHandleId = entityManager.getFileHandleIdForVersion(userInfo, entityId, versionNumber);
		List<String> idsList = new LinkedList<String>();
		idsList.add(fileHandleId);
		return fileHandleManager.getAllFileHandles(idsList, true);
	}

	@Override
	public EntityId getEntityIdForAlias(String alias) {
		String entityId = entityManager.getEntityIdForAlias(alias);
		EntityId id = new EntityId();
		id.setId(entityId);
		return id;
	}

	@Override
	public EntityChildrenResponse getChildren(Long userId,
			EntityChildrenRequest request) {
		ValidateArgument.required(userId, "userId");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.getChildren(userInfo, request);
	}

	@Override
	public EntityId lookupChild(Long userId, EntityLookupRequest request) {
		ValidateArgument.required(userId, "userId");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.lookupChild(userInfo, request);
	}

	@Override
	public DataTypeResponse changeEntityDataType(Long userId, String id, DataType dataType) {
		ValidateArgument.required(userId, "userId");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.changeEntityDataType(userInfo, id, dataType);
	}

	@Override
	public StsCredentials getTemporaryCredentialsForEntity(Long userId, String entityId, StsPermission permission) {
		ValidateArgument.required(userId, "userId");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return stsManager.getTemporaryCredentials(userInfo, entityId, permission);
	}
}
