package org.sagebionetworks.repo.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityToMapUtil;
import org.sagebionetworks.repo.manager.EntityWithAnnotations;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.util.SchemaHelper;
import org.sagebionetworks.repo.web.controller.MetadataProviderFactory;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation for REST controller for CRUD operations on Base DTOs and Base
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
public class GenericEntityControllerImpl implements GenericEntityController {


	@Autowired
	NodeQueryDao nodeQueryDao;
	@Autowired
	EntityManager entityManager;
	@Autowired
	PermissionsManager permissionsManager;
	@Autowired
	UserManager userManager;
	@Autowired
	private MetadataProviderFactory metadataProviderFactory;
	
	public GenericEntityControllerImpl(){
		
	}
	

	/**
	 * Provided for tests
	 * @param entitiesAccessor
	 * @param entityManager
	 */
	GenericEntityControllerImpl(EntityManager entityManager) {
		super();
		this.entityManager = entityManager;
	}

	@Override
	public <T extends Nodeable> PaginatedResults<T> getEntities(String userId, PaginatedParameters paging,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException {
		ServiceConstants.validatePaginationParams(paging.getOffset(), paging.getLimit());
		UserInfo userInfo = userManager.getUserInfo(userId);
		ObjectType type =  ObjectType.getNodeTypeForClass(clazz);
		// First build the query that will be used
		BasicQuery query = QueryUtils.createFindPaginagedOfType(paging, type);
		// Execute the query and convert to entities.
		return executeQueryAndConvertToEntites(paging, request, clazz,
				userInfo, query);
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
	private <T extends Nodeable> PaginatedResults<T> executeQueryAndConvertToEntites(
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
			T entity = this.getEntity(userInfo, id, request, clazz);
			entityList.add(entity);
		}
		return new PaginatedResults<T>(request.getServletPath()
				+ UrlHelpers.getUrlForModel(clazz), entityList,
				nodeResults.getTotalNumberOfResults(), paging.getOffset(), paging.getLimit(), paging.getSortBy(), paging.getAscending());
	}

	@Override
	public <T extends Nodeable> T getEntity(String userId, String id, HttpServletRequest request, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);
		UserInfo userInfo = userManager.getUserInfo(userId);
		return getEntity(userInfo, entityId, request, clazz);
	}
	
	/**
	 * Anytime we fetch an entity we do so through this path.
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
	public <T extends Nodeable> T getEntity(UserInfo info, String id, HttpServletRequest request, Class<? extends T> clazz) throws NotFoundException, DatastoreException, UnauthorizedException{
		// Determine the object type from the url.
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		// Fetch the provider that will validate this entity.
		@SuppressWarnings("unchecked")
		TypeSpecificMetadataProvider<T> provider = (TypeSpecificMetadataProvider<T>)metadataProviderFactory.getMetadataProvider(type);
		T entity = entityManager.getEntity(info, id, clazz);
		if (null == entity) {
			throw new NotFoundException("no entity with id " + id + " exists");
		}
		// Add the type specific metadata that is common to all objects.
		addServiceSpecificMetadata(entity, request);
		// Add the type specific metadata.
		provider.addTypeSpecificMetadata(entity, request, info);
		return entity;
	}

	@Override
	public <T extends Nodeable> T createEntity(String userId, T newEntity, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException {
		// Determine the object type from the url.
		Class<? extends T> clazz = (Class<? extends T>) newEntity.getClass();
		ObjectType type = ObjectType.getNodeTypeForClass(newEntity.getClass());
		// Fetch the provider that will validate this entity.
		@SuppressWarnings("unchecked")
		TypeSpecificMetadataProvider<T> provider = (TypeSpecificMetadataProvider<T>)metadataProviderFactory.getMetadataProvider(type);
		// Validate the entity
		provider.validateEntity(newEntity);
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		String id = entityManager.createEntity(userInfo, newEntity);
		// Return the resulting entity.
		return getEntity(userInfo, id, request, clazz);
	}

	@Override
	public <T extends Nodeable> T updateEntity(String userId,
			T updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {
		if(updatedEntity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(updatedEntity.getId() == null) throw new IllegalArgumentException("Updated Entity cannot have a null id");
		// Get the type for this entity.
		ObjectType type = ObjectType.getNodeTypeForClass(updatedEntity.getClass());
		Class<? extends T> clazz = (Class<? extends T>) updatedEntity.getClass();
		// Fetch the provider that will validate this entity.
		@SuppressWarnings("unchecked")
		TypeSpecificMetadataProvider<T> provider = (TypeSpecificMetadataProvider<T>)metadataProviderFactory.getMetadataProvider(type);
		// First validate this change
		provider.validateEntity(updatedEntity);
		// Keep the entity id
		String entityId = updatedEntity.getId();
		// Get the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		// Now do the update
		entityManager.updateEntity(userInfo, updatedEntity);
		// Return the udpated entity
		return getEntity(userInfo, entityId, request, clazz);
	}

	@Override
	public void deleteEntity(String userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		entityManager.deleteEntity(userInfo, entityId);

		return;
	}

	@Override
	public <T extends Nodeable> JsonSchema getEntitySchema(Class<? extends T> clazz) throws DatastoreException {
		return SchemaHelper.getSchema(clazz);
	}
	
	@Override
	public <T extends Nodeable> JsonSchema getEntitiesSchema(Class<? extends T> clazz) throws DatastoreException {
		// TODO is there a better way to pass this class?
		PaginatedResults<T> empty = new PaginatedResults<T>();
		return SchemaHelper.getSchema(empty.getClass());
	}

	private <T extends Nodeable> void addServiceSpecificMetadata(T entity, HttpServletRequest request) {
		UrlHelpers.setAllUrlsForEntity(entity, request);
	}

	private void addServiceSpecificMetadata(String id, Annotations annotations,
			HttpServletRequest request) {
		annotations.setId(id); // the NON url-encoded id
		annotations.setUri(UrlHelpers.makeEntityPropertyUri(request));
	}

	@Override
	public Annotations getEntityAnnotations(String userId, String id,
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
	public Annotations updateEntityAnnotations(String userId, String entityId,
			Annotations updatedAnnotations, HttpServletRequest request) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Annotations must have a non-null id");
		UserInfo userInfo = userManager.getUserInfo(userId);
		entityManager.updateAnnotations(userInfo,entityId, updatedAnnotations);
		Annotations annos = entityManager.getAnnotations(userInfo, updatedAnnotations.getId());
		addServiceSpecificMetadata(updatedAnnotations.getId(), annos, request);
		return annos;
	}

	@Override
	public JsonSchema getEntityAnnotationsSchema() throws DatastoreException {
		return SchemaHelper.getSchema(Annotations.class);
	}

	@Override
	public <T extends BaseChild> List<T> getEntityChildrenOfType(String userId,
			String parentId, Class<? extends T> childClass, HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		ObjectType childType =  ObjectType.getNodeTypeForClass(childClass);
		// For this case we want all children so build up the paging as such
		PaginatedParameters paging = new PaginatedParameters(0, Long.MAX_VALUE, null, true);
		BasicQuery query = QueryUtils.createChildrenOfTypePaginated(parentId, paging, childType);
		PaginatedResults<T> pageResult = executeQueryAndConvertToEntites(paging, request, childClass, userInfo, query);
		return pageResult.getResults();
	}
	
	@Override
	public <T extends BaseChild> PaginatedResults<T> getEntityChildrenOfTypePaginated(
			String userId, String parentId, Class<? extends T> clazz,
			PaginatedParameters paging, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		ObjectType childType =  ObjectType.getNodeTypeForClass(clazz);
		UserInfo userInfo = userManager.getUserInfo(userId);
		BasicQuery query = QueryUtils.createChildrenOfTypePaginated(parentId, paging, childType);
		return executeQueryAndConvertToEntites(paging, request, clazz, userInfo, query);
	}
	

	@Override
	public <T extends BaseChild> Collection<T> aggregateEntityUpdate(String userId, String parentId, Collection<T> update,	HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException {
		if(update == null) return null;
		if(update.isEmpty()) return update;
		// First try the updated
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<String> updatedIds = entityManager.aggregateEntityUpdate(userInfo, parentId, update);
		// Now create the update object
		List<T> newList = new ArrayList<T>();
		Class tClass = update.iterator().next().getClass();
		for(int i=0; i<updatedIds.size(); i++){
			newList.add((T) entityManager.getEntity(userInfo, updatedIds.get(i), tClass));
		}
		return newList;
	}

	/**
	 * Create a new entity
	 * <p>
	 * 
	 * @param userId
	 * @param newACL
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the newly created entity
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	@Override
	public <T extends Base> AccessControlList createEntityACL(String userId, AccessControlList newACL,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);		
		AccessControlList acl = permissionsManager.overrideInheritance(newACL, userInfo);
		acl.setUri(request.getRequestURI());

		return acl;
	}

	

	@Override
	public  <T extends Base> AccessControlList getEntityACL(String entityId, String userId, HttpServletRequest request, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// First try the updated
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessControlList acl = permissionsManager.getACL(entityId, userInfo);
		
		acl.setUri(request.getRequestURI());

		return acl;
	}


	@Override
	public AccessControlList updateEntityACL(String userId,
			AccessControlList updated) throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException {
		// Resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return permissionsManager.updateACL(updated, userInfo);
	}

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id the id of the node whose inheritance is to be restored
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@Override
	public  void deleteEntityACL(String userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		permissionsManager.restoreInheritance(id, userInfo);
	}

	@Override
	public <T extends Base> JsonSchema getAclSchema() throws DatastoreException {
		return SchemaHelper.getSchema(AccessControlList.class);
	}


	@Override
	public <T extends Nodeable> QueryResults executeQueryWithAnnotations(String userId, BasicQuery query, Class<? extends T> clazz,
			HttpServletRequest request) throws DatastoreException, NotFoundException {
		if(query == null) throw new IllegalArgumentException("Query cannot be null");
		if(query.getFrom() == null) throw new IllegalArgumentException("Query.getFrom() cannot be null");
		// Lookup the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		NodeQueryResults nodeResults = nodeQueryDao.executeQuery(query, userInfo);
		List<String> ids = nodeResults.getResultIds();
		// Convert the list of ids to entities.
		List<Map<String, Object>> allRows = new ArrayList<Map<String, Object>>();
		for(String id: ids){
			EntityWithAnnotations<T> entityWithAnnos;
			try {
				T entity = this.getEntity(userInfo, id, request, clazz);
				Annotations annos = this.getEntityAnnotations(userInfo, id, request);
				entityWithAnnos = new EntityWithAnnotations<T>();
				entityWithAnnos.setEntity(entity);
				entityWithAnnos.setAnnotations(annos);
				Map<String, Object> row = EntityToMapUtil.createMapFromEntity(entityWithAnnos);
				// Add this row
				allRows.add(row);
			} catch (NotFoundException e) {
				// This should never occur
				throw new DatastoreException("Node query returned node id: "+id+" but we failed to load this node: "+e.getMessage(), e);
			} catch (UnauthorizedException e) {
				// This should never occur
				throw new DatastoreException("Node query returned node id: "+id+" but the user was not authorized to see this node: "+e.getMessage(), e);
			}
		}
		// done
		return new QueryResults(allRows, nodeResults.getTotalNumberOfResults());
	}

}
