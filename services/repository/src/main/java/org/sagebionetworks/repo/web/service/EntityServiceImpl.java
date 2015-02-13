package org.sagebionetworks.repo.web.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.EntityTypeConverter;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LocationableTypeConversionResult;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.PaginatedParameters;
import org.sagebionetworks.repo.web.QueryUtils;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.metadata.AllTypesValidator;
import org.sagebionetworks.repo.web.service.metadata.EntityEvent;
import org.sagebionetworks.repo.web.service.metadata.EntityProvider;
import org.sagebionetworks.repo.web.service.metadata.EntityValidator;
import org.sagebionetworks.repo.web.service.metadata.EventType;
import org.sagebionetworks.repo.web.service.metadata.MetadataProviderFactory;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificDeleteProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificMetadataProvider;
import org.sagebionetworks.repo.web.service.metadata.TypeSpecificVersionDeleteProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
@SuppressWarnings({"rawtypes","unchecked"})
public class EntityServiceImpl implements EntityService {
	
	@Autowired
	NodeQueryDao nodeQueryDao;
	@Autowired
	EntityManager entityManager;
	@Autowired
	EntityPermissionsManager entityPermissionsManager;
	@Autowired
	UserManager userManager;
	@Autowired
	private MetadataProviderFactory metadataProviderFactory;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private AllTypesValidator allTypesValidator;
	@Autowired
	FileHandleManager fileHandleManager;
	@Autowired
	EntityTypeConverter entityTypeConverter;
	
	public EntityServiceImpl(){}

	/**
	 * Provided for tests
	 * @param entitiesAccessor
	 * @param entityManager
	 */
	public EntityServiceImpl(EntityManager entityManager) {
		super();
		this.entityManager = entityManager;
	}

	@Override
	public <T extends Entity> PaginatedResults<T> getEntities(Long userId, PaginatedParameters paging,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException {
		ServiceConstants.validatePaginationParamsNoOffsetEqualsOne(paging.getOffset(), paging.getLimit());
		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType type =  EntityType.getNodeTypeForClass(clazz);
		// First build the query that will be used
		BasicQuery query = QueryUtils.createFindPaginagedOfType(paging, type);
		// Execute the query and convert to entities.
		return executeQueryAndConvertToEntites(paging, request, clazz,
				userInfo, query);
	}
	
	@Override
	public PaginatedResults<VersionInfo> getAllVersionsOfEntity(
			Long userId, Integer offset, Integer limit, String entityId,
			HttpServletRequest request)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		if(offset == null){
			offset = 1;
		}
		if(limit == null){
			limit = 10;
		}
		ServiceConstants.validatePaginationParamsNoOffsetEqualsOne((long)offset, (long)limit);
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<VersionInfo> versions = entityManager.getVersionsOfEntity(userInfo, entityId, (long)offset-1, (long)limit);

		String urlPath = request.getRequestURL()==null ? "" : request.getRequestURL().toString();
		return new PaginatedResults<VersionInfo>(urlPath, versions.getResults(), versions.getTotalNumberOfResults(), offset, limit, /*sort*/null, /*descending*/false);
	}

	@Override
	public <T extends Entity> T getEntity(Long userId, String id, HttpServletRequest request, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);
		UserInfo userInfo = userManager.getUserInfo(userId);
		return getEntity(userInfo, entityId, request, clazz, EventType.GET);
	}
	
	@Override
	public Entity getEntity(Long userId, String id, HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityHeader header = entityManager.getEntityHeader(userInfo, id, null);
		EntityType type = EntityType.getEntityType(header.getType());
		return getEntity(userInfo, id, request, type.getClassForType(), EventType.GET);
	}
	/**
	 * Any time we fetch an entity we do so through this path.
	 * @param <T>
	 * @param info
	 * @param id
	 * @param request
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntity(UserInfo info, String id, HttpServletRequest request, Class<? extends T> clazz, EventType eventType) throws NotFoundException, DatastoreException, UnauthorizedException{
		// Determine the object type from the url.
		EntityType type = EntityType.getNodeTypeForClass(clazz);
		T entity = entityManager.getEntity(info, id, clazz);
		// Do all of the type specific stuff.
		this.doAddServiceSpecificMetadata(info, entity, type, request, eventType);
		return entity;
	}
	
	/**
	 * Do all type specific stuff to an entity
	 * @param <T>
	 * @param info
	 * @param entity
	 * @param type
	 * @param request
	 * @param eventType
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	private <T extends Entity> void doAddServiceSpecificMetadata(UserInfo info, T entity, EntityType type, HttpServletRequest request, EventType eventType) throws DatastoreException, NotFoundException, UnauthorizedException{
		// Fetch the provider that will validate this entity.
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(type);

		// Add the type specific metadata that is common to all objects.
		addServiceSpecificMetadata(entity, request);
		// Add the type specific metadata
		if(providers != null) {
			for (EntityProvider<Entity> provider : providers) {
				if (provider instanceof TypeSpecificMetadataProvider) {
					((TypeSpecificMetadataProvider) provider).addTypeSpecificMetadata(entity, request, info, eventType);
				}
			}
		}
	}
	
	@Override
	public <T extends Entity> T getEntityForVersion(Long userId,String id, Long versionNumber, HttpServletRequest request,
			Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return getEntityForVersion(userInfo, id, versionNumber, request, clazz);
	}
	
	@Override
	public <T extends Entity> T getEntityForVersion(UserInfo info, String id, Long versionNumber, HttpServletRequest request,
			Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Determine the object type from the url.
		EntityType type = EntityType.getNodeTypeForClass(clazz);
		T entity = entityManager.getEntityForVersion(info, id, versionNumber, clazz);
		// Do all of the type specific stuff.
		this.doAddServiceSpecificMetadata(info, entity, type, request, EventType.GET);
		return entity;
	}
	
	@Override
	public Entity getEntityForVersion(Long userId, String id,	Long versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType type = entityManager.getEntityType(userInfo, id);
		return getEntityForVersion(userId, id, versionNumber, request, type.getClassForType());
	}

	@Override
	public List<EntityHeader> getEntityHeaderByMd5(Long userId, String md5, HttpServletRequest request)
			throws NotFoundException, DatastoreException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<EntityHeader> entityHeaders = entityManager.getEntityHeaderByMd5(userInfo, md5);
		return entityHeaders;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Entity> T createEntity(Long userId, T newEntity, String activityId, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException {
		// Determine the object type from the url.
		Class<? extends T> clazz = (Class<? extends T>) newEntity.getClass();
		EntityType type = EntityType.getNodeTypeForClass(newEntity.getClass());
		// Fetch the provider that will validate this entity.
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Create a new id for this entity
		long newId = idGenerator.generateNewId();
		newEntity.setId(KeyFactory.keyToString(newId));
		EventType eventType = EventType.CREATE;
		// Fire the event
		fireValidateEvent(userInfo, eventType, newEntity, type);
		String id = entityManager.createEntity(userInfo, newEntity, activityId);
		// Return the resulting entity.
		return getEntity(userInfo, id, request, clazz, eventType);
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
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(type);
		// Validate the entity
		if(providers != null) {
			for (EntityProvider<Entity> provider : providers) {
				if (provider instanceof EntityValidator) {
					((EntityValidator) provider).validateEntity(entity, event);
				}
			}
		}
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Entity> T updateEntity(Long userId,
			T updatedEntity, boolean newVersion, String activityId, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {
		if(updatedEntity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(updatedEntity.getId() == null) throw new IllegalArgumentException("Updated Entity cannot have a null id");
		// Get the type for this entity.
		EntityType type = EntityType.getNodeTypeForClass(updatedEntity.getClass());
		Class<? extends T> clazz = (Class<? extends T>) updatedEntity.getClass();
		// Fetch the provider that will validate this entity.
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		EventType eventType = EventType.UPDATE;
		// Fire the event
		fireValidateEvent(userInfo, eventType, updatedEntity, type);
		// Keep the entity id
		String entityId = updatedEntity.getId();
		// Now do the update
		entityManager.updateEntity(userInfo, updatedEntity, newVersion, activityId);
		// Return the updated entity
		return getEntity(userInfo, entityId, request, clazz, eventType);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteEntity(Long userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType type = entityManager.getEntityType(userInfo, id);
		deleteEntity(userId, entityId, type.getClassForType());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Entity> void deleteEntity(Long userId, String id, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		// First get the entity we are deleting
		EntityType type = EntityType.getNodeTypeForClass(clazz);
		// Fetch the provider that will validate this entity.
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(type);
		entityManager.deleteEntity(userInfo, entityId);
		// Do extra cleanup as needed.
		if(providers != null) {
			for(EntityProvider<Entity> provider : providers) {
				if (provider instanceof TypeSpecificDeleteProvider) {
					((TypeSpecificDeleteProvider) provider).entityDeleted(entityId);
				}
			}
		}
		return;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteEntityVersion(Long userId, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType type = entityManager.getEntityType(userInfo, id);
		deleteEntityVersion(userId, entityId, versionNumber, type.getClassForType());
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Entity> void deleteEntityVersion(Long userId, String id,
			Long versionNumber, Class<? extends Entity> classForType) throws DatastoreException, NotFoundException, UnauthorizedException, ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// First get the entity we are deleting
		EntityType type = EntityType.getNodeTypeForClass(classForType);
		// Fetch the provider that will validate this entity.
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(type);
		entityManager.deleteEntityVersion(userInfo, id, versionNumber);
		// Do extra cleanup as needed.
		if(providers != null) {
			for (EntityProvider<Entity> provider : providers) {
				if (provider instanceof TypeSpecificVersionDeleteProvider) {
					((TypeSpecificVersionDeleteProvider) provider).entityVersionDeleted(id, versionNumber);
				}
			}
		}
	}

	private <T extends Entity> void addServiceSpecificMetadata(T entity, HttpServletRequest request) {
		UrlHelpers.setAllUrlsForEntity(entity, request);
	}

	private void addServiceSpecificMetadata(String id, Annotations annotations,
			HttpServletRequest request) {
		annotations.setId(id); // the NON url-encoded id
		annotations.setUri(UrlHelpers.makeEntityAnnotationsUri(id));
	}

	@Override
	public Annotations getEntityAnnotations(Long userId, String id,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return getEntityAnnotations(userInfo, id, request);
	}
	
	@Override
	public Annotations getEntityAnnotations(UserInfo info, String id,HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException {
		Annotations annotations = entityManager.getAnnotations(info, id);
		addServiceSpecificMetadata(id, annotations, request);
		return annotations;
	}
	
	@Override
	public Annotations getEntityAnnotationsForVersion(Long userId, String id,
			Long versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		Annotations annotations = entityManager.getAnnotationsForVersion(userInfo, id, versionNumber);
		addServiceSpecificMetadata(id, annotations, request);
		return annotations;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Annotations updateEntityAnnotations(Long userId, String entityId,
			Annotations updatedAnnotations, HttpServletRequest request) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Annotations must have a non-null id");
		UserInfo userInfo = userManager.getUserInfo(userId);
		entityManager.updateAnnotations(userInfo,entityId, updatedAnnotations);
		Annotations annos = entityManager.getAnnotations(userInfo, updatedAnnotations.getId());
		addServiceSpecificMetadata(updatedAnnotations.getId(), annos, request);
		return annos;
	}

	@Override
	public <T extends Entity> List<T> getEntityChildrenOfType(Long userId,
			String parentId, Class<? extends T> childClass, HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType childType =  EntityType.getNodeTypeForClass(childClass);
		// For this case we want all children so build up the paging as such
		PaginatedParameters paging = new PaginatedParameters(0, Long.MAX_VALUE, null, true);
		BasicQuery query = QueryUtils.createChildrenOfTypePaginated(parentId, paging, childType);
		PaginatedResults<T> pageResult = executeQueryAndConvertToEntites(paging, request, childClass, userInfo, query);
		return pageResult.getResults();
	}
	
	@Override
	public <T extends Entity> PaginatedResults<T> getEntityChildrenOfTypePaginated(
			Long userId, String parentId, Class<? extends T> clazz,
			PaginatedParameters paging, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		EntityType childType =  EntityType.getNodeTypeForClass(clazz);
		UserInfo userInfo = userManager.getUserInfo(userId);
		BasicQuery query = QueryUtils.createChildrenOfTypePaginated(parentId, paging, childType);
		return executeQueryAndConvertToEntites(paging, request, clazz, userInfo, query);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList createEntityACL(Long userId, AccessControlList newACL,
			HttpServletRequest request) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ConflictingUpdateException {

		UserInfo userInfo = userManager.getUserInfo(userId);		
		AccessControlList acl = entityPermissionsManager.overrideInheritance(newACL, userInfo);
		acl.setUri(request.getRequestURI());
		return acl;
	}

	@Override
	public  AccessControlList getEntityACL(String entityId, Long userId, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException {
		// First try the updated
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessControlList acl = entityPermissionsManager.getACL(entityId, userInfo);
		
		acl.setUri(UrlHelpers.makeEntityACLUri(entityId));

		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList updateEntityACL(Long userId,
			AccessControlList updated, String recursive, HttpServletRequest request) throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		// Resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessControlList acl = entityPermissionsManager.updateACL(updated, userInfo);
		if (recursive != null && recursive.equalsIgnoreCase("true"))
			entityPermissionsManager.applyInheritanceToChildren(updated.getId(), userInfo);
		acl.setUri(request.getRequestURI());
		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList createOrUpdateEntityACL(Long userId,
			AccessControlList acl, String recursive, HttpServletRequest request) throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String entityId = acl.getId();
		if (entityPermissionsManager.hasLocalACL(entityId)) {
			// Local ACL exists; update it
			return updateEntityACL(userId, acl, recursive, request);
		} else {
			// Local ACL does not exist; create it
			return createEntityACL(userId, acl, request);			
		}
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteEntityACL(Long userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		entityPermissionsManager.restoreInheritance(id, userInfo);
	}

	@Override
	public boolean hasAccess(String entityId, Long userId, HttpServletRequest request, String accessType) 
		throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityPermissionsManager.hasAccess(entityId, ACCESS_TYPE.valueOf(accessType), userInfo).getAuthorized();
	}

	@Override
	public List<EntityHeader> getEntityPath(Long userId, String entityId) throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.getEntityPath(userInfo, entityId);
	}

	@Override
	public EntityHeader getEntityHeader(Long userId, String entityId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.getEntityHeader(userInfo, entityId, versionNumber);
	}

	@Override
	public <T extends Entity> EntityHeader getEntityBenefactor(String entityId, Long userId, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, ACLInheritanceException {
		if(entityId == null) throw new IllegalArgumentException("EntityId cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// First get the permissions benefactor
		String benefactor = entityPermissionsManager.getPermissionBenefactor(entityId, userInfo);
		return getEntityHeader(userId, benefactor, null);
	}

	@Override
	public PaginatedResults<EntityHeader> getEntityReferences(Long userId, String entityId, Integer versionNumber, Integer offset, Integer limit, HttpServletRequest request)
			throws NotFoundException, DatastoreException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (offset==null) offset = 1;
		if (limit==null) limit = Integer.MAX_VALUE;
		ServiceConstants.validatePaginationParamsNoOffsetEqualsOne((long)offset, (long)limit);
		QueryResults<EntityHeader> results = entityManager.getEntityReferences(userInfo, entityId, versionNumber, offset-1, limit);
		String urlPath = request.getRequestURL()==null ? "" : request.getRequestURL().toString();
		return new PaginatedResults(urlPath,  results.getResults(), results.getTotalNumberOfResults(), offset, limit, /*sort*/null, /*ascending*/true);
	}

	@Override
	public UserEntityPermissions getUserEntityPermissions(Long userId, String entityId) throws NotFoundException, DatastoreException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityPermissionsManager.getUserPermissionsForEntity(userInfo, entityId);
	}
	
	@Override
	public S3AttachmentToken createS3AttachmentToken(Long userId, String entityId,
			S3AttachmentToken token) throws UnauthorizedException, NotFoundException, DatastoreException, InvalidModelException {
		return entityManager.createS3AttachmentToken(userId, entityId, token);
	}

	@Override
	public PresignedUrl getAttachmentUrl(Long userId, String entityId,
			String tokenId) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		return entityManager.getAttachmentUrl(userId, entityId, tokenId);
	}

	/**
	 * First, execute the given query to determine the nodes that match the criteria.
	 * Then, for each node id, fetch the entity and build up the paginated results.
	 * 
	 * @param <T>
	 * @param paging
	 * @param request
	 * @param clazz
	 * @param userInfo
	 * @param nodeResults
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	private <T extends Entity> PaginatedResults<T> executeQueryAndConvertToEntites(
			PaginatedParameters paging,
			HttpServletRequest request,
			Class<? extends T> clazz,
			UserInfo userInfo,
			BasicQuery query) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// First execute the query.
		NodeQueryResults nodeResults = nodeQueryDao.executeQuery(query, userInfo);
		// Fetch each entity
		List<T> entityList = new ArrayList<T>();
		for(String id: nodeResults.getResultIds()){
			T entity = this.getEntity(userInfo, id, request, clazz, EventType.GET);
			entityList.add(entity);
		}
		return new PaginatedResults<T>(request.getServletPath()
				+ UrlHelpers.ENTITY, entityList,
				nodeResults.getTotalNumberOfResults(), paging.getOffset(), paging.getLimit(), paging.getSortBy(), paging.getAscending());
	}

	@Override
	public boolean doesEntityHaveChildren(Long userId, String entityId,
			HttpServletRequest request) throws DatastoreException,
			ParseException, NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("EntityId cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.doesEntityHaveChildren(userInfo, entityId);
	}
	
	@Override
	public Activity getActivityForEntity(Long userId, String entityId,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.getActivityForEntity(userInfo, entityId, null);
	}
	
	@Override
	public Activity getActivityForEntity(Long userId, String entityId, Long versionNumber,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.getActivityForEntity(userInfo, entityId, versionNumber);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Activity setActivityForEntity(Long userId, String entityId,
			String activityId, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		if(activityId == null) throw new IllegalArgumentException("Activity Id can not be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityManager.setActivityForEntity(userInfo, entityId, activityId);		
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteActivityForEntity(Long userId, String entityId,
			HttpServletRequest request) throws DatastoreException,
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
		String fileHandleId =  entityManager.getFileHandleIdForCurrentVersion(userInfo, id);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
	}
	
	@Override
	public String getFilePreviewRedirectURLForCurrentVersion(Long userId, String entityId) throws DatastoreException, NotFoundException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId =  entityManager.getFileHandleIdForCurrentVersion(userInfo, entityId);
		// Look up the preview for this file.
		String previewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(previewId);
	}

	@Override
	public String getFileRedirectURLForVersion(Long userId, String id, Long versionNumber) throws DatastoreException, NotFoundException {
		if(id == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId =  entityManager.getFileHandleIdForVersion(userInfo, id, versionNumber);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
	}


	@Override
	public String getFilePreviewRedirectURLForVersion(Long userId, String id,
			Long versionNumber) throws DatastoreException, NotFoundException {
		if(id == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId =  entityManager.getFileHandleIdForVersion(userInfo, id, versionNumber);
		// Look up the preview for this file.
		String previewId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(previewId);
	}
	
	@Override
	public FileHandleResults getEntityFileHandlesForCurrentVersion(Long userId, String entityId) throws DatastoreException, NotFoundException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId =  entityManager.getFileHandleIdForCurrentVersion(userInfo, entityId);
		List<String> idsList = new LinkedList<String>();
		idsList.add(fileHandleId);
		return fileHandleManager.getAllFileHandles(idsList, true);
	}

	@Override
	public FileHandleResults getEntityFileHandlesForVersion(Long userId, String entityId, Long versionNumber) throws DatastoreException, NotFoundException {
		if(entityId == null) throw new IllegalArgumentException("Entity Id cannot be null");
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		if(versionNumber == null) throw new IllegalArgumentException("versionNumber cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Get the file handle.
		String fileHandleId =  entityManager.getFileHandleIdForVersion(userInfo, entityId, versionNumber);
		List<String> idsList = new LinkedList<String>();
		idsList.add(fileHandleId);
		return fileHandleManager.getAllFileHandles(idsList, true);
	}

	@Override
	public LocationableTypeConversionResult convertLocationable(Long userId, String entityId) throws NotFoundException {
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		if(entityId == null) throw new IllegalArgumentException("Entity cannot be null");
		UserInfo userInfo = userManager.getUserInfo(userId);
		return entityTypeConverter.convertOldTypeToNew(userInfo, entityId);
	}

}
