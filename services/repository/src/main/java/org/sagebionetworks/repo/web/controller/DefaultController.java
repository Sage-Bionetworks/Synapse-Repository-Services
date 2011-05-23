package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST controller for CRUD operations on any object
 * 
 * @author jhill
 */
@Controller
public class DefaultController extends BaseController {

	@Autowired
	GenericEntityController entityController;
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;
	
	/**
	 * 
	 * @param <T>
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.PROJECT }, method = RequestMethod.POST)
	public @ResponseBody
	<T extends Base> T createEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException {
		// Determine the object type from the url.
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		// Fetch the provider that will validate this entity.
		TypeSpecificMetadataProvider<T> provider = TypeSpecificMetadataProviderFactory.getProvider(type);
		@SuppressWarnings("unchecked")
		T entity = (T) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		// Validate the entity before we create it
		provider.validateEntity(entity);
		// Now create the entity
		T createdEntity = (T) entityController.createEntity(userId, entity, request);
		// Finally, add the type specific metadata.
		provider.addTypeSpecificMetadata(createdEntity, request);
		return createdEntity;
	}
	
	

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECT+"/{id}" }, method = RequestMethod.GET)
	public @ResponseBody
	<T extends Base> T getEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the object type
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		// Fetch the provider that will validate this entity.
		TypeSpecificMetadataProvider<T> provider = TypeSpecificMetadataProviderFactory.getProvider(type);
		T updatedEntity = (T) entityController.getEntity(userId, id, request, type.getClassForType());
		provider.addTypeSpecificMetadata(updatedEntity, request);
		return updatedEntity;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECT+"/{id}" }, method = RequestMethod.PUT)
	public @ResponseBody
	<T extends Base> T updateEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		// Validate the object type
		// Determine the object type from the url.
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		// Fetch the provider that will validate this entity.
		TypeSpecificMetadataProvider<T> provider = TypeSpecificMetadataProviderFactory.getProvider(type);
		@SuppressWarnings("unchecked")
		T entity = (T) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		if(etag != null){
			entity.setEtag(etag.toString());
		}
		// validate the entity
		provider.validateEntity(entity);
		entity = entityController.updateEntity(userId, id,	entity, request);
		provider.addTypeSpecificMetadata(entity, request);
		return entity;
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.PROJECT+"/{id}" }, method = RequestMethod.DELETE)
	public void deleteEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Determine the object type from the url.
		entityController.deleteEntity(userId, id);
		return;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECT }, method = RequestMethod.GET)
	public @ResponseBody
	<T extends Base> PaginatedResults<T> getEntities(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException {

		if(ServiceConstants.DEFAULT_SORT_BY_PARAM.equals(sort)){
			sort = null;
		}
		// Determine the object type from the url.
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		// Fetch the provider that will validate this entity.
		TypeSpecificMetadataProvider<T> provider = TypeSpecificMetadataProviderFactory.getProvider(type);
		PaginatedResults<T> results = (PaginatedResults<T>) entityController.getEntities(
				userId, offset, limit, sort, ascending, request, type.getClassForType());

		for (T entity : results.getResults()) {
			provider.addTypeSpecificMetadata(entity, request);
		}

		return results;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ={ UrlHelpers.PROJECT+"/{id}" + UrlHelpers.SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEntitySchema(@PathVariable String id, HttpServletRequest request) throws DatastoreException {
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		return entityController.getEntitySchema(type.getClassForType());
	}
	

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECT + UrlHelpers.SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEntitiesSchema(HttpServletRequest request) throws DatastoreException {
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		return entityController.getEntitiesSchema(type.getClassForType());
	}
	
}
