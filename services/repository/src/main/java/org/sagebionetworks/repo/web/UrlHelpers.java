package org.sagebionetworks.repo.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.HasLayers;
import org.sagebionetworks.repo.model.HasPreviews;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.PrefixConst;
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

public class UrlHelpers {

	private static final Logger log = Logger.getLogger(UrlHelpers.class
			.getName());
	
	public static final String ACCESS 			= "/access";
	
	public static final String PERMISSIONS 		= "/permissions";

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
	
	public static final String TYPE = "/type";
	/**
	 * The URL prefix for all object's Access Control List (ACL).
	 */
	public static final String ACL = "/acl";
	public static final String OBJECT_TYPE_ID_ACL = OBJECT_TYPE_ID+ACL;
	
	public static final String BENEFACTOR = "/benefactor";
	public static final String OBJECT_TYPE_ID_BENEFACTOR = OBJECT_TYPE_ID+BENEFACTOR;
	
	/**
	 * URL suffix for entity annotations
	 * 
	 */
	public static final String ANNOTATIONS = "/annotations";
	
	/**
	 * URL suffix for locationable entity S3Token
	 * 
	 */
	public static final String S3TOKEN = "/s3Token";

	/**
	 * Used to get the path of a entity.
	 */
	public static final String PATH = "/path";
	
	/**
	 * URL suffix for entity schemas
	 */
	public static final String SCHEMA = "/schema";
	
	public static final String VERSION = "/version";
	
	public static final String REFERENCED_BY	= "/referencedby";

	/**
	 * All of the base URLs for Synapse objects
	 */
	public static final String DATASET 	= PrefixConst.DATASET;
	public static final String LAYER 	= PrefixConst.LAYER;
	public static final String PREVIEW 	= PrefixConst.PREVIEW;
	public static final String LOCATION = PrefixConst.LOCATION;
	public static final String PROJECT	= PrefixConst.PROJECT;
	public static final String EULA 	= PrefixConst.EULA;
	public static final String AGREEMENT = PrefixConst.AGREEMENT;
	public static final String FOLDER	 = PrefixConst.FOLDER;
	public static final String ENTITY	 = PrefixConst.ENTITY;
	public static final String ANALYSIS	 = PrefixConst.ANALYSIS;
	public static final String STEP 	 = PrefixConst.STEP;
	public static final String CODE 	 = PrefixConst.CODE;
	
	/**
	 * All of the base URLs for Synapse objects with ID.
	 */
	public static final String DATASET_ID	= DATASET+ID;
	public static final String LAYER_ID 	= LAYER+ID;
	public static final String PREVIEW_ID 	= PREVIEW+ID;
	public static final String LOCATION_ID	= LOCATION+ID;
	public static final String PROJECT_ID 	= PROJECT+ID;
	public static final String EULA_ID 		= EULA+ID;
	public static final String AGREEMENT_ID = AGREEMENT+ID;
	public static final String FOLDER_ID 	= FOLDER+ID;
	public static final String ENTITY_ID	= ENTITY+ID;
	public static final String ANALYSIS_ID	= ANALYSIS+ID;
	public static final String STEP_ID		= STEP+ID;
	public static final String CODE_ID		= CODE+ID;
	
	public static final String ENTITY_ID_TYPE = ENTITY_ID+TYPE;
	
	/**
	 * All of the base URLs for Synapse objects's Annotations.
	 */
	public static final String DATASET_ANNOTATIONS	= DATASET_ID+ANNOTATIONS;
	public static final String LAYER_ANNOTATIONS 	= LAYER_ID+ANNOTATIONS;
	public static final String PREVIEW_ANNOTATIONS 	= PREVIEW_ID+ANNOTATIONS;
	public static final String LOCATION_ANNOTATIONS = LOCATION_ID+ANNOTATIONS;
	public static final String PROJECT_ANNOTATIONS	= PROJECT_ID+ANNOTATIONS;
	public static final String EULA_ANNOTATIONS		= EULA_ID+ANNOTATIONS;
	public static final String AGREEMENT_ANNOTATIONS = AGREEMENT_ID+ANNOTATIONS;
	public static final String FOLDER_ANNOTATIONS 	= FOLDER_ID+ANNOTATIONS;
	public static final String ANALYSIS_ANNOTATIONS	= ANALYSIS_ID+ANNOTATIONS;
	public static final String STEP_ANNOTATIONS 	= STEP_ID+ANNOTATIONS;
	public static final String CODE_ANNOTATIONS 	= CODE_ID+ANNOTATIONS;
	public static final String ENTITY_ANNOTATIONS 	= ENTITY_ID+ANNOTATIONS;

	/**
	 * All of the base URLs for locationable entity s3Tokens
	 */
	public static final String DATASET_S3TOKEN 	= DATASET_ID+S3TOKEN;
	public static final String LAYER_S3TOKEN 	= LAYER_ID+S3TOKEN;
	public static final String CODE_S3TOKEN 	= CODE_ID+S3TOKEN;
	
	/**
	 * All of the base URLs for Synapse objects's paths.
	 */
	public static final String DATASET_PATH		= DATASET_ID+PATH;
	public static final String LAYER_PATH 		= LAYER_ID+PATH;
	public static final String PREVIEW_PATH 	= PREVIEW_ID+PATH;
	public static final String LOCATION_PATH 	= LOCATION_ID+PATH;
	public static final String PROJECT_PATH		= PROJECT_ID+PATH;
	public static final String EULA_PATH		= EULA_ID+PATH;
	public static final String AGREEMENT_PATH	= AGREEMENT_ID+PATH;
	public static final String FOLDER_PATH		= FOLDER_ID+PATH;
	public static final String ANALYSIS_PATH	= ANALYSIS_ID+PATH;
	public static final String STEP_PATH		= STEP_ID+PATH;
	public static final String CODE_PATH		= CODE_ID+PATH;
	
	/**
	 * All of the base URLs for Synapse object's versions.
	 */
	public static final String LOCATION_VERSION		= LOCATION_ID+VERSION;
	public static final String DATASET_VERSION		= DATASET_ID+VERSION;
	public static final String LAYER_VERSION		= LAYER_ID+VERSION;
	public static final String CODE_VERSION			= CODE_ID+VERSION;
	public static final String ENTITY_VERSION		= ENTITY_ID+VERSION;
	/**
	 * For Synapse objects that have children, these urls list all children of that type.
	 */
	public static final String DATASET_CHILDREN 	= PARENT_TYPE_ID+DATASET;
	public static final String LAYER_CHILDREN	 	= PARENT_TYPE_ID+LAYER;
	public static final String PREVIEW_CHILDREN		= PARENT_TYPE_ID+PREVIEW;
	public static final String LOCATION_CHILDREN	= PARENT_TYPE_ID+LOCATION;
	public static final String PROJECT_CHILDREN		= PARENT_TYPE_ID+PROJECT;
	public static final String EULA_CHILDREN		= PARENT_TYPE_ID+EULA;
	public static final String AGREEMENT_CHILDREN	= PARENT_TYPE_ID+AGREEMENT;
	public static final String FOLDER_CHILDREN		= PARENT_TYPE_ID+FOLDER;
	public static final String ANALYSIS_CHILDREN	= PARENT_TYPE_ID+ANALYSIS;
	public static final String STEP_CHILDREN		= PARENT_TYPE_ID+STEP;
	public static final String CODE_CHILDREN		= PARENT_TYPE_ID+CODE;
	/**
	 * Get the schema for each object type
	 */
	public static final String DATASET_SCHEMA	= DATASET+SCHEMA;
	public static final String LAYER_SCHEMA 	= LAYER+SCHEMA;
	public static final String PREVIEW_SCHEMA 	= PREVIEW+SCHEMA;
	public static final String LOCATION_SCHEMA	= LOCATION+SCHEMA;
	public static final String PROJECT_SCHEMA 	= PROJECT+SCHEMA;
	public static final String EULA_SCHEMA 		= EULA+SCHEMA;
	public static final String AGREEMENT_SCHEMA = AGREEMENT+SCHEMA;
	public static final String FOLDER_SCHEMA 	= FOLDER+SCHEMA;
	public static final String ANALYSIS_SCHEMA 	= ANALYSIS+SCHEMA;
	public static final String STEP_SCHEMA 		= STEP+SCHEMA;
	public static final String CODE_SCHEMA 		= CODE+SCHEMA;
	public static final String S3TOKEN_SCHEMA	= S3TOKEN+SCHEMA;
	
	/**
	 * Get a specific version of an entity
	 */
	public static final String LOCATION_VERSION_NUMBER		= LOCATION_VERSION+VERSION_NUMBER;
	public static final String DATASET_VERSION_NUMBER		= DATASET_VERSION+VERSION_NUMBER;
	public static final String LAYER_VERSION_NUMBER			= LAYER_VERSION+VERSION_NUMBER;
	public static final String CODE_VERSION_NUMBER			= CODE_VERSION+VERSION_NUMBER;
	public static final String ENTITY_VERSION_NUMBER		= ENTITY_VERSION+VERSION_NUMBER;
	
	/**
	 * Get the annotations of a specific version of an entity
	 */
	public static final String LOCATION_VERSION_ANNOTATIONS = 	LOCATION_VERSION_NUMBER+ANNOTATIONS;
	public static final String DATASET_VERSION_ANNOTATIONS = 	DATASET_VERSION_NUMBER+ANNOTATIONS;
	public static final String LAYER_VERSION_ANNOTATIONS = 		LAYER_VERSION_NUMBER+ANNOTATIONS;
	public static final String CODE_VERSION_ANNOTATIONS = 		CODE_VERSION_NUMBER+ANNOTATIONS;

	public static final String OBJECT_TYPE_SCHEMA = OBJECT_TYPE+SCHEMA;


	/**
	 * URL path for query controller
	 * 
	 */
	public static final String QUERY = "/query";

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
	 * All administration URLs must start with this URL or calls will be
	 * blocked when Synapse enters READ_ONLY or DOWN modes for maintenance.
	 */
	public static final String ADMIN = "/admin";
	/**
	 * These are the new more RESTful backup/restore URLS.
	 */
	public static final String DAEMON 						= ADMIN+"/daemon";
	public static final String BACKUP						= "/backup";
	public static final String RESTORE						= "/restore";
	public static final String SEARCH_DOCUMENT 				= "/searchDocument";
	public static final String DAEMON_ID					= "/{daemonId}";
	public static final String ENTITY_BACKUP_DAMEON			= DAEMON+BACKUP;
	public static final String ENTITY_RESTORE_DAMEON		= DAEMON+RESTORE;
	public static final String ENTITY_SEARCH_DOCUMENT_DAMEON = DAEMON+SEARCH_DOCUMENT;
	public static final String ENTITY_DAEMON_ID				= DAEMON+DAEMON_ID;
	
	public static final String CONCEPT	= "/concept";
	public static final String CONCEPT_ID	= CONCEPT+ID;
	public static final String CHILDERN_TRANSITIVE = "/childrenTransitive";
	public static final String CONCEPT_ID_CHILDERN_TRANSITIVE	= CONCEPT_ID+CHILDERN_TRANSITIVE;
	
	/**
	 * The name of the query parameter for a prefix filter.
	 */
	public static final String PREFIX_FILTER = "prefix";
	
	/**
	 * The other header key used to request JSONP
	 */
	public static final String REQUEST_CALLBACK_JSONP = "callback";
	
	
	/**
	 * The stack status of synapse 
	 */
	public static final String STACK_STATUS					= ADMIN+"/synapse/status";
	
	/**
	 * Mapping of dependent property classes to their URL suffixes
	 */
	private static final Map<Class, String> PROPERTY2URLSUFFIX;

	/**
	 * This is a memoized cache for our URL regular expressions
	 */
	private static Map<Class, Pattern> MODEL2REGEX = new HashMap<Class, Pattern>();

	static {

		Map<Class, String> property2urlsuffix = new HashMap<Class, String>();
		property2urlsuffix.put(Annotations.class, ANNOTATIONS);
		PROPERTY2URLSUFFIX = Collections.unmodifiableMap(property2urlsuffix);
	}

	/**
	 * Determine the controller URL prefix for a given model class
	 * 
	 * @param theModelClass
	 * @return the URL for the model class
	 */
	@SuppressWarnings("unchecked")
	public static String getUrlForModel(Class theModelClass) {
		EntityType type =  EntityType.getNodeTypeForClass(theModelClass);
		return type.getUrlPrefix();
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
	@SuppressWarnings("unchecked")
	public static String makeEntityPropertyUri(Entity entity,
			Class propertyClass, HttpServletRequest request) {

		String urlPrefix = getUrlPrefixFromRequest(request);

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
	 * @return Servlet context and path url prefix
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
	 * @param entityId 
	 * @param entityClass 
	 * @param urlPrefix
	 * @return the entity uri
	 */
	public static String createEntityUri(String entityId, Class<? extends Entity> entityClass, String urlPrefix){
		if(entityId == null) throw new IllegalArgumentException("Entity id cannot be null");
		if(entityClass == null) throw new IllegalArgumentException("Entity class cannot be null");
		if(urlPrefix == null) throw new IllegalArgumentException("Url prefix cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append(urlPrefix);
		EntityType type = EntityType.getNodeTypeForClass(entityClass);
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
	public static void setBaseUriForEntity(Entity entity, HttpServletRequest request){
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(request == null) throw new IllegalArgumentException("Request cannot be null");
		// First get the prefix
		String prefix = UrlHelpers.getUrlPrefixFromRequest(request);
		// Now build the uri.
		String uri = UrlHelpers.createEntityUri(entity.getId(), entity.getClass(), prefix);
		entity.setUri(uri);
	}
	
	/**
	 * Set the all of the Entity URLs (annotations, ACL)
	 * @param entity
	 */
	public static void setAllEntityUrls(Entity entity){
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getUri() == null) throw new IllegalArgumentException("Entity.uri cannot be null null");
		// Add the annotations
		entity.setAnnotations(entity.getUri()+ANNOTATIONS);
		// Add the acl
		entity.setAccessControlList(entity.getUri()+ACL);
		if(entity instanceof Locationable) {
			Locationable able = (Locationable) entity;
			able.setS3Token(entity.getUri() + UrlHelpers.S3TOKEN);
		}
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
	 * @param entity 
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
	public static void setAllUrlsForEntity(Entity entity, HttpServletRequest request){
		// First set the base url
		setBaseUriForEntity(entity, request);
		// Set the Entity types
		setAllEntityUrls(entity);
		// Set the specialty types
		// Layers
		if(entity instanceof HasLayers){
			setHasLayersUrl((HasLayers) entity);
		}
		// Previews
		if(entity instanceof HasPreviews){
			setHasPreviewsUrl((HasPreviews) entity);
		}
		// Versions
		if(entity instanceof Versionable){
			setVersionableUrl((Versionable)entity);
		}
		// Set the entity type
		entity.setEntityType(entity.getClass().getName());
	}
	
	/**
	 * Helper method to validate all urs.
	 * @param object
	 */
	public static void validateAllUrls(Entity object) {
		if(object == null) throw new IllegalArgumentException("Entity cannot be null");
		if(object.getUri() == null) throw new IllegalArgumentException("Entity.uri cannot be null");
		EntityType type = EntityType.getNodeTypeForClass(object.getClass());
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
			expected = object.getUri()+UrlHelpers.VERSION+"/"+able.getVersionNumber();
			if(!expected.equals(able.getVersionUrl())){
				throw new IllegalArgumentException("Expected versionUrl: "+expected+" but was: "+able.getVersionUrl());
			}
		}
		
		// Locationable
		if(object instanceof Locationable) {
			Locationable able = (Locationable) object;
			expected = object.getUri() + UrlHelpers.S3TOKEN;
			if(!expected.equals(able.getS3Token())) {
				throw new IllegalArgumentException("Expected s3Token: " + expected + " but was " + able.getS3Token());
			}
		}
	}
	
	/**
	 * Create an ACL redirect URL.
	 * @param request - The initial request.
	 * @param type - The type of the redirect entity.
	 * @param id - The ID of the redirect entity
	 * @return
	 */
	public static String createACLRedirectURL(HttpServletRequest request, EntityType type, String id){
		if(request == null) throw new IllegalArgumentException("Request cannot be null");
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		if(id == null) throw new IllegalArgumentException("ID cannot be null");
		StringBuilder redirectURL = new StringBuilder();
		redirectURL.append(UrlHelpers.getUrlPrefixFromRequest(request));
		redirectURL.append(type.getUrlPrefix());
		redirectURL.append("/");
		redirectURL.append(id);
		redirectURL.append(UrlHelpers.ACL);
		return redirectURL.toString();
	}
}
