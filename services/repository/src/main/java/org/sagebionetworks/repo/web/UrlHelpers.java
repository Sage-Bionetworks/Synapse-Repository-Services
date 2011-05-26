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
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.LayerLocations;
import org.sagebionetworks.repo.model.LayerPreview;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;

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

	/**
	 * URL prefix for all objects that are referenced by their ID.
	 * 
	 */
	public static final String ID = "/{id}";
	
	/**
	 * The URL prefix for all object's Access Control List (ACL).
	 */
	public static final String ACL = "/acl";
	
	/**
	 * URL prefix for dataset model objects
	 * 
	 */
	public static final String DATASET = "/dataset";
	
	/**
	 * URL suffix for a dataset info with its ID.
	 */
	public static final String DATASET_ID = DATASET+ID;
	
	/**
	 * URL to get a dataset's ACL: /dataset/{id}/acl
	 */
	public static final String DATASET_ACL = DATASET_ID+ACL;
	
	/**
	 * URL prefix for dataset layer model objects
	 * 
	 */
	public static final String LAYER = "/layer";
	
	/**
	 * URL suffix for a layer info with its ID.
	 */
	public static final String LAYER_ID = LAYER+ID;
	
	/**
	 * URL to get a layer's ACL: /layer/{id}/acl
	 */
	public static final String LAYER_ACL = LAYER_ID+ACL;

	/**
	 * URL suffix for entity schemas
	 */
	public static final String SCHEMA = "/schema";

	/**
	 * URL suffix for entity annotations
	 * 
	 */
	public static final String ANNOTATIONS = "/annotations";

	/**
	 * URL path for query controller
	 * 
	 */
	public static final String QUERY = "/query";

	/**
	 * URL suffix for preview info
	 */
	public static final String PREVIEW = "/preview";

	/**
	 * URL suffix for preview info
	 */
	public static final String PREVIEW_MAP = "/previewAsMap";

	/**
	 * URL suffix for locations info
	 */
	public static final String LOCATIONS = "/locations";
	
	/**
	 * URL suffix for a locations info with its ID.
	 */
	public static final String LOCATIONS_ID = LOCATIONS+ID;
	
	/**
	 * URL to get a location's ACL: /locations/{id}/acl
	 */
	public static final String LOCATIONS_ACL = LOCATIONS_ID+ACL;
	
	/**
	 * URL suffix for Project info
	 */
	public static final String PROJECT = "/project";
	
	/**
	 * URL suffix for a Project info with its ID.
	 */
	public static final String PROJECT_ID = PROJECT+ID;
	
	/**
	 * URL to get a project's ACL: /project/{id}/acl
	 */
	public static final String PROJECT_ACL = PROJECT_ID+ACL;
	
	/**
	 * URL for a project's annotations.
	 */
	public static final String PROJECT_ANNOTATIONS = PROJECT_ID+ANNOTATIONS;

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

//	/**
//	 * URL prefix for Users in the system
//	 * 
//	 */
//	public static final String USER = "/user";

	
	/**
	 * URL prefix for User Group model objects
	 * 
	 */
	public static final String USERGROUP = "/usergroup";

	/**
	 * URL prefix for Users in a UserGroup
	 * 
	 */
	public static final String USERS = "/users";
	
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
	 * @return The URL prefixes we currently have mapped to model classes
	 */
	public static Collection<String> getAllEntityUrlPrefixes() {
		return MODEL2URL.values();
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


	// TODO this class needs unit tests
	private static String getUrlPrefix(Base entity, HttpServletRequest request) {

		String urlPrefix = request.getServletPath();

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
