package org.sagebionetworks.repo.web.controller;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.LayerLocations;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.util.LocationHelper2;
import org.sagebionetworks.repo.util.SchemaHelper;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.DependentEntityController;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST controller for RU operations on LayerLocations and LayerLocation objects
 * <p>
 * 
 * Note that any controller logic common to all dependent objects belongs in the
 * implementation of {@link DependentEntityController} that this wraps. Only
 * functionality specific to LayerLocation(s) objects belongs in this
 * controller.
 * 
 * @author deflaux
 */
@Controller
public class LayerLocationsController extends BaseController2
//implements
//		DependentEntityController<LayerLocations, InputDataLayer> 
{
	
	@Autowired
	GenericEntityController entityController;
	@Autowired
	LocationHelper2 locationHelper;

//	private DependentEntityController<LayerLocations, InputDataLayer> controller;

//	LayerLocationsController() {
//		controller = new DependentEntityControllerImp<LayerLocations, InputDataLayer>(
//				LayerLocations.class);
//	}
//
//	private void checkAuthorization(String userId, Boolean readOnly) {
//		DependentPropertyDAO<LayerLocations, InputDataLayer> dao = getDaoFactory()
//				.getLayerLocationsDAO(userId);
//		setDao(dao);
//	}
//
//	@Override
//	public void setDao(DependentPropertyDAO<LayerLocations, InputDataLayer> dao) {
//		controller.setDao(dao);
//	}

	/*******************************************************************************
	 * LayerLocations RU handlers
	 */

//	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.LOCATIONS, method = RequestMethod.GET)
	public @ResponseBody
	LayerLocations getDependentEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		// TODO only curators can call this
//		checkAuthorization(userId, true);
		List<LayerLocation> list = entityController.getEntityChildrenOfType(userId, id, LayerLocation.class);
		LayerLocations locations = new LayerLocations();
		Iterator<LayerLocation> it = list.iterator();
		while(it.hasNext()){
			addServiceSpecificMetadata(it.next(), request);
		}
		// Use the parent id as the locations id
		locations.setId(id);
		locations.setLocations(list);
		// this eTag is not used.
		locations.setEtag("0");
		locations.setUri(UrlHelpers.makeEntityPropertyUri(request));
		return locations;
	}

//	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.LOCATIONS, method = RequestMethod.PUT)
	public @ResponseBody
	LayerLocations updateDependentEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody LayerLocations updatedEntity,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException {
		
		if(updatedEntity == null)throw new IllegalArgumentException("LayerLocations cannot be null");
		if(updatedEntity.getLocations() == null)throw new IllegalArgumentException("LayerLocations.getLocations() cannot be null");
		// Make sure each has its parent set
		Iterator<LayerLocation> it = updatedEntity.getLocations().iterator();
		while(it.hasNext()){
			LayerLocation loc = it.next();
			loc.setParentId(id);
		}
		// Convert the passed locations to an aggregate update
		Collection<LayerLocation> result = entityController.aggregateEntityUpdate(userId, id, updatedEntity.getLocations(), request);
		updatedEntity.setLocations(result);
		long oldEtag = Long.parseLong(updatedEntity.getEtag());
		updatedEntity.setEtag(new Long(++oldEtag).toString());
		return updatedEntity;
	}

//	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}" + UrlHelpers.LAYER
			+ "/{id}" + UrlHelpers.LOCATIONS + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getDependentEntitySchema() throws DatastoreException {

		return SchemaHelper.getSchema(LayerLocations.class);
	}

	/*******************************************************************************
	 * Layer Location handlers
	 */

	/**
	 * @param userId
	 * @param id
	 * @param request
	 * @return the requested layer
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.S3_LOCATION, method = RequestMethod.GET)
	public @ResponseBody
	LayerLocation getS3Location(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		// TODO only authorized users can receive this info
//		checkAuthorization(userId, true);

		LayerLocations locations = this.getDependentEntity(userId, id, request);
		LayerLocation location = getLocationForLayer(locations,
				LayerLocation.LocationTypeNames.awss3);
		if (null == location) {
			throw new NotFoundException("No AWS S3 location exists for layer "
					+ id);
		}

//		LocationHelpers locationHelper = LocationHelpers.getHelper(getDaoFactory());
		String signedPath = locationHelper.getS3Url(userId, location.getPath());

		location.setPath(signedPath);

		return location;
	}

	/**
	 * @return the schema
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.S3_LOCATION + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getS3LocationSchema() throws DatastoreException {
		return SchemaHelper.getSchema(LayerLocation.class);
	}
	
	/**
	 * @param userId
	 * @param id
	 * @param request
	 * @return the requested layer
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.EBS_LOCATION, method = RequestMethod.GET)
	public @ResponseBody
	LayerLocation getEbsLocation(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		// TODO only authorized users can receive this info
//		checkAuthorization(userId, true);
		LayerLocations locations = this.getDependentEntity(userId, id, request);
		LayerLocation location = getLocationForLayer(locations,
				LayerLocation.LocationTypeNames.awsebs);
		if (null == location) {
			throw new NotFoundException("No AWS EBS location exists for layer "
					+ id);
		}

		return location;
	}

	/**
	 * @return the schema
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.EBS_LOCATION + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEBSLocationSchema() throws DatastoreException {
		return SchemaHelper.getSchema(LayerLocation.class);
	}

	/**
	 * @param userId
	 * @param id
	 * @param request
	 * @return the requested layer
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.SAGE_LOCATION, method = RequestMethod.GET)
	public @ResponseBody
	LayerLocation getSageLocation(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		// TODO only authorized users can receive this info
//		checkAuthorization(userId, true);
		LayerLocations locations = this.getDependentEntity(userId, id, request);
		LayerLocation location = getLocationForLayer(locations,
				LayerLocation.LocationTypeNames.sage);
		if (null == location) {
			throw new NotFoundException("No Sage location exists for layer "
					+ id);
		}

		return location;
	}
	
	/**
	 * @return the schema
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.SAGE_LOCATION + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getSageLocationSchema() throws DatastoreException {
		return SchemaHelper.getSchema(LayerLocation.class);
	}



	/**
	 * Simple sanity check test request, using the default view
	 * <p>
	 * 
	 * @param modelMap
	 *            the parameter into which output data is to be stored
	 * @return a dummy hard-coded response
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + UrlHelpers.LOCATIONS + "/test", method = RequestMethod.GET)
	public String sanityCheck(ModelMap modelMap) {
		modelMap.put("hello", "REST for Dataset Layer Locations rocks");
		return ""; // use the default view
	}

	/*******************************************************************************
	 * Helpers
	 */
	private LayerLocation getLocationForLayer(LayerLocations layer,
			LayerLocation.LocationTypeNames type) {
		// We assume N is small here, a single digit number
		for (LayerLocation location : layer.getLocations()) {
			if (location.getType().equals(type.name())) {
				return location;
			}
		}
		return null;
	}
	
	private void addServiceSpecificMetadata(LayerLocation entity, HttpServletRequest request) {
		entity.setUri(UrlHelpers.makeEntityPropertyUri(request));
	}

}
