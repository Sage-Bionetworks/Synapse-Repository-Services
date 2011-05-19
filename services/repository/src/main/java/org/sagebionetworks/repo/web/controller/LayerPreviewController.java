package org.sagebionetworks.repo.web.controller;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerPreview;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.StoredLayerPreview;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.util.SchemaHelper;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
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
 * REST controller for RU operations on LayerPreview objects
 * <p>
 * 
 * Note that any controller logic common to all dependent objects belongs in the
 * implementation of {@link DependentEntityController} that this wraps. Only
 * functionality specific to LayerPreview objects belongs in this controller.
 * 
 * @author deflaux
 */
@Controller
public class LayerPreviewController extends BaseController {

	@Autowired
	GenericEntityController entityController;

	/*******************************************************************************
	 * Layer Preview handlers
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	LayerPreview getDependentEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		StoredLayerPreview stored = getStoredPreviewForLayer(userId, id,request);

		return createFromStoredPreview(request, stored, id);
	}

	/**
	 * Fetch the stored preview for for a given layer.
	 * @param userId
	 * @param id
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	private StoredLayerPreview getStoredPreviewForLayer(String userId,
			String id, HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		List<StoredLayerPreview> list = entityController.getEntityChildrenOfType(userId, id, StoredLayerPreview.class, request);
		if(list == null) throw new DatastoreException("All layers should have one LayerPreview Child");
		if(list.size() != 1)  throw new DatastoreException("All layers should have only onle LayerPreview Child.  Found: "+list.size());
		StoredLayerPreview stored = list.get(0);
		return stored;
	}

	/**
	 * Create a LayerPreview from a StoredLayerPreview.
	 * @param request
	 * @param stored
	 * @param layerId
	 * @return
	 * @throws DatastoreException
	 */
	private LayerPreview createFromStoredPreview(HttpServletRequest request,
			StoredLayerPreview stored, String layerId) throws DatastoreException {
		LayerPreview preview = new LayerPreview();
		preview.setCreationDate(stored.getCreationDate());
		preview.setEtag(stored.getEtag());
		preview.setId(layerId);
		try {
			preview.setPreview(stored.getPreviewAsString());
		} catch (UnsupportedEncodingException e) {
			throw new DatastoreException(e);
		}
		addServiceSpecificMetadata(preview, request);
		return preview;
	}

	private void addServiceSpecificMetadata(LayerPreview entity,
			HttpServletRequest request) {
		entity.setUri(UrlHelpers.makeEntityPropertyUri(request));
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.PREVIEW, method = RequestMethod.PUT)
	public @ResponseBody
	LayerPreview updateDependentEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody LayerPreview updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {

		// First get the stored preview
		StoredLayerPreview stored = getStoredPreviewForLayer(userId, id, request);
		updateStoredLayerPreviewFromLayerPreview(stored, updatedEntity);
		stored = entityController.updateEntity(userId, stored.getId(), stored, request);
		return createFromStoredPreview(request, stored, id);
	}
	
	private void updateStoredLayerPreviewFromLayerPreview(StoredLayerPreview stored, LayerPreview preview) throws DatastoreException{
		stored.setEtag(preview.getEtag());
		try {
			stored.setPreviewAsString(preview.getPreview());
		} catch (UnsupportedEncodingException e) {
			throw new DatastoreException(e);
		}
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}" + UrlHelpers.LAYER
			+ "/{id}" + UrlHelpers.PREVIEW + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getDependentEntitySchema() throws DatastoreException {

		return SchemaHelper.getSchema(LayerPreview.class);
	}

	/**
	 * This controller method attempts to interpret the preview data as tab
	 * delimited text and return it in map format. If it is unable to transform
	 * the preview data to a map, it returns an error.
	 * 
	 * @param userId
	 * @param id
	 * @param request
	 * @return preview data
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.PREVIEW_MAP, method = RequestMethod.GET)
	public @ResponseBody
	QueryResults getPreviewAsMap(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		// checkAuthorization(userId, true);
		LayerPreview preview = this.getDependentEntity(userId, id, request);

		String rawPreview = preview.getPreview();
		String lines[] = rawPreview.split("(?m)\n");
		String header[] = lines[0].split("\t");

		// Confirm that we are able to interpret this as a tab-delimited file
		if ((4 > header.length) || (4 > lines.length)) {
			throw new DatastoreException(
					"Unable to convert preview data to map format");
		}

		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for (int row = 1; row < lines.length; row++) {
			Map<String, Object> result = new HashMap<String, Object>();
			String values[] = lines[row].split("\t");

			// Confirm that the tab-delimited data is well-formed
			if (header.length != values.length) {
				throw new DatastoreException(
						"Unable to convert preview data to map format");
			}

			for (int column = 0; column < values.length; column++) {
				result.put(header[column], values[column]);
			}
			results.add(result);
		}

		return new QueryResults(results, results.size());
	}

	/**
	 * @return the schema
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.PREVIEW_MAP
			+ UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getPreviewAsMapSchema() throws DatastoreException {
		return SchemaHelper.getSchema(QueryResults.class);
	}

	/*******************************************************************************
	 * Helpers
	 */

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
			+ UrlHelpers.LAYER + UrlHelpers.PREVIEW + "/test", method = RequestMethod.GET)
	public String sanityCheck(ModelMap modelMap) {
		modelMap.put("hello", "REST for Dataset Layer Previews rocks");
		return ""; // use the default view
	}

}
