package org.sagebionetworks.repo.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.HasLayers;
import org.sagebionetworks.repo.model.HasLocations;
import org.sagebionetworks.repo.model.HasPreviews;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.LayerLocations;
import org.sagebionetworks.repo.model.LayerPreview;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Versionable;

/**
 * UrlHelpers is responsible for the formatting of all URLs exposed by the
 * service.
 * 
 * The various controllers should not be formatting URLs. They should instead
 * call methods in this helper. Its important to keep URL formulation logic in
 * one place to ensure consistency across the space of URLs that this service
 * supports.
 * 
 * @author deflaux
 * 
 */

@SuppressWarnings("rawtypes")
public class UrlHelpers {

	private static final Logger log = Logger.getLogger(UrlHelpers.class
			.getName());
	
	public static final String ACCESS 			= "/access";

	public static final String ACCESS_TYPE_PARAM	= "accessType";

	/**
	 * URL prefix for all objects that are referenced by their ID.
	 * 
	 */
	public static final String ID = "/{id}";
	
	public static final String PARENT_TYPE 		= "/{parentType}";
	public static final String PARENT_ID 		= "/{parentId}";
	public static final String VERSION_NUMBER 	= "/{versionNumber}";
	public static final String PARENT_TYPE_ID 	= PARENT_TYPE+PARENT_ID;
	public static final String OBJECT_TYPE		= "/{objectType}";
	public static final String OBJECT_TYPE_ID	= OBJECT_TYPE+ID;
	/**
	 * The URL prefix for all object's Access Control List (ACL).
	 */
	public static final String ACL = "/acl";
	public static final String OBJECT_TYPE_ID_ACL = OBJECT_TYPE_ID+ACL;
	
	/**
	 * URL suffix for entity annotations
	 * 
	 */
	public static final String ANNOTATIONS = "/annotations";
	
	/**
	 * URL suffix for entity schemas
	 */
	public static final String SCHEMA = "/schema";
	
	public static final String VERSION = "/version";
	
	/**
	 * All of the base URLs for Synapse objects
	 */
	public static final String DATASET 	= "/dataset";
	public static final String LAYER 	= "/layer";
	public static final String PREVIEW 	= "/preview";
	public static final String LOCATION = "/location";
	public static final String PROJECT	= "/project";
	/**
	 * All of the base URLs for Synapse objects with ID.
	 */
	public static final String DATASET_ID	= DATASET+ID;
	public static final String LAYER_ID 	= LAYER+ID;
	public static final String PREVIEW_ID 	= PREVIEW+ID;
	public static final String LOCATION_ID	= LOCATION+ID;
	public static final String PROJECT_ID 	= PROJECT+ID;
	/**
	 * All of the base URLs for Synapse objects's Annotations.
	 */
	public static final String DATASET_ANNOTATIONS	= DATASET_ID+ANNOTATIONS;
	public static final String LAYER_ANNOTATIONS 	= LAYER_ID+ANNOTATIONS;
	public static final String PREVIEW_ANNOTATIONS 	= PREVIEW_ID+ANNOTATIONS;
	public static final String LOCATION_ANNOTATIONS = LOCATION_ID+ANNOTATIONS;
	public static final String PROJECT_ANNOTATIONS	= PROJECT_ID+ANNOTATIONS;
	/**
	 * All of the base URLs for Synapse object's versions.
	 */
	public static final String LOCATION_VERSION		= LOCATION_ID+VERSION;
	/**
	 * For Synapse objects that have children, these urls list all children of that type.
	 */
	public static final String DATASET_CHILDREN 	= PARENT_TYPE_ID+DATASET;
	public static final String LAYER_CHILDREN	 	= PARENT_TYPE_ID+LAYER;
	public static final String PREVIEW_CHILDREN		= PARENT_TYPE_ID+PREVIEW;
	public static final String LOCATION_CHILDREN	= PARENT_TYPE_ID+LOCATION;
	public static final String PROJECT_CHILDREN		= PARENT_TYPE_ID+PROJECT;
	/**
	 * Get the schema for each object type
	 */
	public static final String DATASET_SCHEMA	= DATASET+SCHEMA;
	public static final String LAYER_SCHEMA 	= LAYER+SCHEMA;
	public static final String PREVIEW_SCHEMA 	= PREVIEW+SCHEMA;
	public static final String LOCATION_SCHEMA	= LOCATION+SCHEMA;
	public static final String PROJECT_SCHEMA 	= PROJECT+SCHEMA;
	
	/**
	 * Get a specific version of an entity
	 */
	public static final String LOCATION_VERSION_NUMBER		= LOCATION_VERSION+VERSION_NUMBER;
	
	/**
	 * Get the annotations of a specific version of an entity
	 */
	public static final String LOCATION_VERSION_ANNOTATIONS = LOCATION_VERSION_NUMBER+ANNOTATIONS;

	public static final String OBJECT_TYPE_SCHEMA = OBJECT_TYPE+SCHEMA;


	/**
	 * URL path for query controller
	 * 
	 */
	public static final String QUERY = "/query";

	/**
	 * URL suffix for preview info
	 */


	/**
	 * URL suffix for preview info
	 */
	@Deprecated
	public static final String PREVIEW_MAP = "/previewAsMap";

	/**
	 * URL suffix for locations info
	 */
	@Deprecated 
	public static final String LOCATIONS = "/locations";
	
	/**
	 * URL suffix for a locations info with its ID.
	 */
	public static final String LOCATIONS_ID = LOCATIONS+ID;

	/**
	 * URL suffix for S3 location metadata
	 */
	public static final String S3_LOCATION = "/awsS3Location";

	/**
	 * URL suffix for EBS location metadata
	 */
	public static final String EBS_LOCATION = "/awsEBSLocation";

	/**
	 * URL suffix for Sage location metadata
	 */
	public static final String SAGE_LOCATION = "/sageLocation";

	/**
	 * URL suffix for an unsupported location type
	 */
	public static final String UNSUPPORTED_LOCATION = "/notYetImplemented";

	/**
	 * URL prefix for Users in the system
	 * 
	 */
	public static final String USER = "/user";

	
	/**
	 * URL prefix for User Group model objects
	 * 
	 */
	public static final String USERGROUP = "/userGroup";
	
	/**
	 * URL prefix for Users in a UserGroup
	 * 
	 */
	public static final String RESOURCES = "/resources";
	
		/**
	 * URL prefix for User mirroring service
	 * 
	 */
	public static final String USER_MIRROR = "/userMirror";



	/**
	 * Mapping of type to URL prefix
	 */
	private static final Map<Class, String> MODEL2URL;

	/**
	 * Mapping of type to property URL suffixes
	 */
	private static final Map<Class, Collection<String>> MODEL2PROPERTY;

	/**
	 * Mapping of dependent property classes to their URL suffixes
	 */
	private static final Map<Class, String> PROPERTY2URLSUFFIX;

	/**
	 * Mapping of location types to URL suffixes
	 */
	private static final Map<String, String> LOCATIONTYPE2URL;

	/**
	 * Mapping of child models to their parent models
	 */
	private static final Map<Class, Class> CHILD2PARENTMODEL;

	/**
	 * Mapping of child URLs to their parent URLs
	 */
	private static final Map<String, String> CHILD2PARENTURL;

	/**
	 * This is a memoized cache for our URL regular expressions
	 */
	private static Map<Class, Pattern> MODEL2REGEX = new HashMap<Class, Pattern>();

	static {
		Map<Class, String> model2url = new HashMap<Class, String>();
		model2url.put(Dataset.class, DATASET);
		model2url.put(InputDataLayer.class, LAYER);
		model2url.put(Project.class, PROJECT);
		model2url.put(LayerLocation.class, LOCATION);
//		model2url.put(User.class, USER);
//		model2url.put(UserGroup.class, USERGROUP);
		MODEL2URL = Collections.unmodifiableMap(model2url);

		Map<Class, Collection<String>> model2property = new HashMap<Class, Collection<String>>();
		model2property.put(Dataset.class, Arrays
				.asList(new String[] { ANNOTATIONS }));
		model2property.put(InputDataLayer.class, Arrays.asList(new String[] {
				ANNOTATIONS, PREVIEW, LOCATIONS, S3_LOCATION, EBS_LOCATION,
				SAGE_LOCATION }));
		MODEL2PROPERTY = Collections.unmodifiableMap(model2property);

		Map<Class, String> property2urlsuffix = new HashMap<Class, String>();
		property2urlsuffix.put(Annotations.class, ANNOTATIONS);
		property2urlsuffix.put(LayerPreview.class, PREVIEW);
		property2urlsuffix.put(LayerLocations.class, LOCATIONS);
		PROPERTY2URLSUFFIX = Collections.unmodifiableMap(property2urlsuffix);
		Map<String, String> locationtype2url = new HashMap<String, String>();
		locationtype2url.put(LayerLocation.LocationTypeNames.awss3.name(),
				S3_LOCATION);
		locationtype2url.put(LayerLocation.LocationTypeNames.awsebs.name(),
				EBS_LOCATION);
		locationtype2url.put(LayerLocation.LocationTypeNames.sage.name(),
				SAGE_LOCATION);
		LOCATIONTYPE2URL = Collections.unmodifiableMap(locationtype2url);

		// TODO we don't need both of these maps, and for those that we do, we
		// can
		// derive them from each other

		Map<Class, Class> child2parent = new HashMap<Class, Class>();
		child2parent.put(InputDataLayer.class, Dataset.class);
		CHILD2PARENTMODEL = Collections.unmodifiableMap(child2parent);

		Map<String, String> child2parenturl = new HashMap<String, String>();
		// TODO create this using the other maps, being lazy right now
		child2parenturl.put(LAYER, DATASET);
		CHILD2PARENTURL = Collections.unmodifiableMap(child2parenturl);

	}

	/**
	 * Determine the controller URL prefix for a given model class
	 * 
	 * @param theModelClass
	 * @return the URL for the model class
	 */
	public static String getUrlForModel(Class theModelClass) {
		return MODEL2URL.get(theModelClass);
	}

	/**
	 * Determine the the parent model class given a child model class
	 * 
	 * @param theChildModelClass
	 * @return the parent model class
	 */
	public static Class getParentForChildModel(Class theChildModelClass) {
		return CHILD2PARENTMODEL.get(theChildModelClass);
	}

	/**
	 * Determine the the parent url given a child url
	 * 
	 * @param childUrl
	 * @return the parent URL
	 */
	public static String getParentForChildUrl(String childUrl) {
		return CHILD2PARENTURL.get(childUrl);
	}


	/**
	 * This is intended for usage by unit tests
	 * 
	 * @param theModelClass
	 * 
	 * @return The URL suffixes we currently have mapped to model classes
	 */
	public static Collection<String> getAllEntityUrlSuffixes(Class theModelClass) {
		return MODEL2PROPERTY.get(theModelClass);
	}

	/**
	 * Helper function to create a relative URL for an entity
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * Dev Note: I can imagine that this bit of code will evolve quite a bit
	 * over time as we expand the variety of our urls and query parameters
	 * 
	 * @param entity
	 * @param request
	 * @return the relative URI for the entity
	 */
	public static String makeEntityUri(Base entity, HttpServletRequest request) {

		String urlPrefix = getUrlPrefix(entity, request);

		String uri = null;
		try {
			uri = urlPrefix + UrlHelpers.getUrlForModel(entity.getClass())
					+ "/" + URLEncoder.encode(entity.getId(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE,
					"Something is really messed up if we don't support UTF-8",
					e);
		}
		return uri;
	}

	/**
	 * Helper function to to create a relative URL for an entity
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * @param entity
	 * @param urlPrefix
	 * @return the relative URI for the entity
	 */
	public static String makeEntityUri(Base entity, String urlPrefix) {

		String uri = null;
		try {
			uri = urlPrefix + UrlHelpers.getUrlForModel(entity.getClass())
					+ "/" + URLEncoder.encode(entity.getId(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE,
					"Something is really messed up if we don't support UTF-8",
					e);
		}
		return uri;
	}

	/**
	 * Helper function to create a relative URL for an entity's dependent
	 * property
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * @param request
	 * @return the uri for this entity's annotations
	 */
	public static String makeEntityPropertyUri(HttpServletRequest request) {
		return request.getRequestURI();
	}

	/**
	 * Helper function to create a relative URL for an entity's annotations
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * @param entity
	 * @param propertyClass
	 * @param request
	 * @return the uri for this entity's annotations
	 */
	public static String makeEntityPropertyUri(Base entity,
			Class propertyClass, HttpServletRequest request) {

		String urlPrefix = getUrlPrefix(entity, request);

		String uri = null;
		try {
			uri = urlPrefix + UrlHelpers.getUrlForModel(entity.getClass())
					+ "/" + URLEncoder.encode(entity.getId(), "UTF-8")
					+ PROPERTY2URLSUFFIX.get(propertyClass);
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE,
					"Something is really messed up if we don't support UTF-8",
					e);
		}
		return uri;
	}

	/**
	 * Helper function to translate ids found in URLs to ids used by the system
	 * <p>
	 * 
	 * Specifically we currently use the serialized system id url-encoded for
	 * use in URLs
	 * 
	 * @param id
	 * @return URL-decoded entity id
	 */
	public static String getEntityIdFromUriId(String id) {
		String entityId = null;
		try {
			entityId = URLDecoder.decode(id, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE,
					"Something is really messed up if we don't support UTF-8",
					e);
		}
		return entityId;
	}
	
	/**
	 * Get the URL prefix from a request.
	 * @param request
	 * @return
	 */
	public static String getUrlPrefixFromRequest(HttpServletRequest request){
		if(request == null) throw new IllegalArgumentException("Request cannot be null");
		if(request.getServletPath() == null) throw new IllegalArgumentException("Servlet path cannot be null");
		String urlPrefix = (null != request.getContextPath()) 
		? request.getContextPath() + request.getServletPath() 
				: request.getServletPath();
		return urlPrefix;
	}
	
	/**
	 * Set the URI for any entity.
	 * @param entity
	 * @param urlPrefix
	 */
	public static String createEntityUri(String entityId, Class<? extends Nodeable> entityClass, String urlPrefix){
		if(entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		if(entityClass == null) throw new IllegalArgumentException("Entity class cannot be null");
		if(urlPrefix == null) throw new IllegalArgumentException("Url prefix cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append(urlPrefix);
		ObjectType type = ObjectType.getNodeTypeForClass(entityClass);
		builder.append(type.getUrlPrefix());
		builder.append("/");
		builder.append(entityId);
		return builder.toString();
	}
	
	/**
	 * Set the base uri for any entity.
	 * @param entity
	 * @param request
	 */
	public static void setBaseUriForEntity(Nodeable entity, HttpServletRequest request){
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(request == null) throw new IllegalArgumentException("Request cannot be null");
		// First get the prefix
		String prefix = UrlHelpers.getUrlPrefixFromRequest(request);
		// Now build the uri.
		String uri = UrlHelpers.createEntityUri(entity.getId(), entity.getClass(), prefix);
		entity.setUri(uri);
	}
	
	/**
	 * Set the all of the Nodeable URLs (annotations, ACL)
	 * @param entity
	 * @param request
	 */
	public static void setAllNodeableUrls(Nodeable entity){
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getUri() == null) throw new IllegalArgumentException("Entity.uri cannot be null null");
		// Add the annotations
		entity.setAnnotations(entity.getUri()+ANNOTATIONS);
		// Add the acl
		entity.setAccessControlList(entity.getUri()+ACL);
	}
	
	/**
	 * Set the URL for the layers for any object that has layers.
	 * @param entity
	 */
	public static void setHasLayersUrl(HasLayers entity){
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getUri() == null) throw new IllegalArgumentException("Entity.uri cannot be null null");
		entity.setLayers(entity.getUri()+LAYER);
	}
	
	/**
	 * Set the URL for the locations for any object that has locations.
	 * @param entity
	 */
	public static void setHasLocationsUrl(HasLocations entity){
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getUri() == null) throw new IllegalArgumentException("Entity.uri cannot be null null");
		entity.setLocations(entity.getUri()+LOCATION);
	}
	
	/**
	 * Set the URL for the preview for any object that has preview.
	 * 
	 * @param entity
	 */
	public static void setHasPreviewsUrl(HasPreviews entity){
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getUri() == null) throw new IllegalArgumentException("Entity.uri cannot be null null");
		entity.setPreviews(entity.getUri()+PREVIEW);
	}
	
	/**
	 * Set the URL of a versionable entity.
	 */
	public static void setVersionableUrl(Versionable entity){
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getUri() == null) throw new IllegalArgumentException("Entity.uri cannot be null null");
		if(entity.getVersionNumber() == null) throw new IllegalArgumentException("Entity version number cannot be null");
		// This URL wil list all version for this entity.
		entity.setVersions(entity.getUri()+VERSION);
		// This URL will reference this specific version of the entity.
		entity.setVersionUrl(entity.getUri()+VERSION+"/"+entity.getVersionNumber());
	}
	
	/**
	 * 
	 * @param entity
	 * @param request
	 */
	public static void setAllUrlsForEntity(Nodeable entity, HttpServletRequest request){
		// First set the base url
		setBaseUriForEntity(entity, request);
		// Set the nodeable types
		setAllNodeableUrls(entity);
		// Set the specialty types
		// Layers
		if(entity instanceof HasLayers){
			setHasLayersUrl((HasLayers) entity);
		}
		// Locations
		if(entity instanceof HasLocations){
			setHasLocationsUrl((HasLocations) entity);
		}
		// Previews
		if(entity instanceof HasPreviews){
			setHasPreviewsUrl((HasPreviews) entity);
		}
		// Versions
		if(entity instanceof Versionable){
			setVersionableUrl((Versionable)entity);
		}
	}
	
	/**
	 * Helper method to validate all urs.
	 * @param type
	 * @param object
	 * @param clone
	 */
	public static void validateAllUrls(Nodeable object) {
		if(object == null) throw new IllegalArgumentException("Entity cannot be null");
		if(object.getUri() == null) throw new IllegalArgumentException("Entity.uri cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(object.getClass());
		String expectedBaseSuffix = type.getUrlPrefix()+"/"+object.getId();
		if(!object.getUri().endsWith(expectedBaseSuffix)){
			throw new IllegalArgumentException("Expected base uri suffix: "+expectedBaseSuffix+" but was: "+object.getUri());
		}
		String expected = object.getUri()+UrlHelpers.ANNOTATIONS;
		if(!expected.equals(object.getAnnotations())){
			throw new IllegalArgumentException("Expected annotations: "+expected+" but was: "+object.getAnnotations());
		}
		expected =  object.getUri()+UrlHelpers.ACL;
		if(!expected.equals(object.getAccessControlList())){
			throw new IllegalArgumentException("Expected annotations: "+expected+" but was: "+object.getAccessControlList());
		}
		// Has layers
		if(object instanceof HasLayers){
			HasLayers hasLayers = (HasLayers) object;
			expected = object.getUri()+UrlHelpers.LAYER;
			if(!expected.equals(hasLayers.getLayers())){
				throw new IllegalArgumentException("Expected layers: "+expected+" but was: "+hasLayers.getLayers());
			}
		}
		// Has locations
		if(object instanceof HasLocations){
			HasLocations has = (HasLocations) object;
			expected = object.getUri()+UrlHelpers.LOCATION;
			if(!expected.equals(has.getLocations())){
				throw new IllegalArgumentException("Expected locations: "+expected+" but was: "+has.getLocations());
			}
		}
		// Has preview
		if(object instanceof HasPreviews){
			HasPreviews has = (HasPreviews) object;
			expected = object.getUri()+UrlHelpers.PREVIEW;
			if(!expected.equals(has.getPreviews())){
				throw new IllegalArgumentException("Expected previews: "+expected+" but was: "+has.getPreviews());
			}
		}
		
		// Versionable
		if(object instanceof Versionable){
			Versionable able = (Versionable) object;
			expected = object.getUri()+UrlHelpers.VERSION;
			if(!expected.equals(able.getVersions())){
				throw new IllegalArgumentException("Expected versions: "+expected+" but was: "+able.getVersions());
			}
			if(able.getVersionNumber() == null){
				throw new IllegalArgumentException("Expected a versionable entity to have a version number");
			}
			expected = object.getUri()+UrlHelpers.VERSION+"/"+able.getVersionNumber();
			if(!expected.equals(able.getVersionUrl())){
				throw new IllegalArgumentException("Expected versionUrl: "+expected+" but was: "+able.getVersionUrl());
			}
		}
	}


	public static String getUrlPrefix(Base entity, HttpServletRequest request) {

		String urlPrefix = (null != request.getContextPath()) 
		? request.getContextPath() + request.getServletPath() 
				: request.getServletPath();

		String parentEntityPrefix = UrlHelpers.getUrlForModel(CHILD2PARENTMODEL
				.get(entity.getClass()));
		if (null != parentEntityPrefix) {
			Pattern pattern = MODEL2REGEX.get(entity.getClass());
			if (null == pattern) {
				String regex = "^(" + urlPrefix + parentEntityPrefix
						+ "/[^/]+)";
				pattern = Pattern.compile(regex);
				MODEL2REGEX.put(entity.getClass(), pattern);
			}
			Matcher matcher = pattern.matcher(request.getRequestURI());
			if (matcher.find()) {
				urlPrefix = matcher.group(1);
			} else {
				throw new RuntimeException(
						"Trouble making outgoing entity url from incoming url: "
								+ request.getRequestURI());
			}
		}

		return urlPrefix;
	}

	/**
	 * @param uriPrefix
	 * @param type
	 * @return the uri to be used to retrieve the metadata about the location
	 */
	public static String makeLocationUri(String uriPrefix, String type) {
		String suffix = LOCATIONTYPE2URL.get(type);
		if (null == suffix) {
			return uriPrefix;
		}
		return uriPrefix + suffix;
	}
	
}
