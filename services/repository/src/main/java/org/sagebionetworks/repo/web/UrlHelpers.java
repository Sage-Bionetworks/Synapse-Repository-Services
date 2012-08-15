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

import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.ServiceConstants.AttachmentType;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
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
	
	public static final String ACCESS 				= "/access";
	
	public static final String PERMISSIONS 			= "/permissions";

	public static final String ACCESS_TYPE_PARAM	= "accessType";
	
	public static final String BUNDLE				= "/bundle";

	/**
	 * URL prefix for all objects that are referenced by their ID.
	 * 
	 */
	public static final String ID_PATH_VARIABLE = "id";
	public static final String ID = "/{"+ID_PATH_VARIABLE+"}";
	
	/**
	 * URL prefix for all objects that are referenced by their ID.
	 * 
	 */
	public static final String PROFILE_ID = "/{profileId}";
	
	public static final String PARENT_TYPE 		= "/{parentType}";
	public static final String PARENT_ID 		= "/{parentId}";
	public static final String VERSION_NUMBER 	= "/{versionNumber}";
	public static final String PARENT_TYPE_ID 	= PARENT_TYPE+PARENT_ID;
	
//	public static final String TOKEN_ID = "{tokenId}/{filename}.{mimeType}";
	
	public static final String TYPE = "/type";
	/**
	 * The URL prefix for all object's Access Control List (ACL).
	 */
	public static final String ACL = "/acl";
	
	public static final String BENEFACTOR = "/benefactor";
	
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
	
	public static final String REGISTRY = "/registry";
	/**
	 * The Effective schema is the flattened schema of an entity.
	 */
	public static final String EFFECTIVE_SCHEMA = "/effectiveSchema";
	
	public static final String REST_RESOURCES = "/REST/resources";
	
	public static final String VERSION = "/version";
	
	public static final String REFERENCED_BY	= "/referencedby";
	
	public static final String ATTACHMENT_S3_TOKEN = "/s3AttachmentToken";
	
	public static final String ATTACHMENT_URL = "/attachmentUrl";
	
	public static final String MIGRATION_OBJECT_ID_PARAM = "id";

	/**
	 * parameter used by migration services to describe the type of migration 
	 * to be performed
	 */
	public static final String MIGRATION_TYPE_PARAM = "migrationType";
	
	/**
	 * All of the base URLs for Synapse objects
	 */
	public static final String ENTITY	 = PrefixConst.ENTITY;
	public static final String USER_PROFILE  = PrefixConst.USER_PROFILE;
	public static final String HEALTHCHECK = PrefixConst.HEALTHCHECK;
	public static final String VERSIONINFO = PrefixConst.VERSIONINFO;	
	/**
	 * All of the base URLs for Synapse object batch requests
	 */
	public static final String ENTITY_TYPE = ENTITY+TYPE;
	


	/**
	 * All of the base URLs for Synapse objects with ID.
	 */
	public static final String ENTITY_ID	= ENTITY+ID;
	public static final String USER_PROFILE_ID		= USER_PROFILE+PROFILE_ID;
	
	public static final String ENTITY_ID_BUNDLE = ENTITY_ID+BUNDLE;
	public static final String ENTITY_ID_ACL = ENTITY_ID+ACL;
	public static final String ENTITY_ID_ID_BENEFACTOR = ENTITY_ID+BENEFACTOR;
	
	/**
	 * Used to get an entity attachment token
	 */
	public static final String ENTITY_S3_ATTACHMENT_TOKEN = ENTITY_ID+ATTACHMENT_S3_TOKEN;
	/**
	 * The url used to get an attachment URL.
	 */
	public static final String ENTITY_ATTACHMENT_URL = ENTITY_ID+ATTACHMENT_URL;
	
	/**
	 * The url used to get a user profile attachment URL.
	 */
	public static final String USER_PROFILE_ATTACHMENT_URL = USER_PROFILE_ID+ATTACHMENT_URL;
	
	
	/**
	 * The base URL for Synapse objects's type (a.k.a. EntityHeader)
	 */
	public static final String ENTITY_ID_TYPE = ENTITY_ID+TYPE;

	/**
	 * All of the base URLs for Synapse objects's Annotations.
	 */
	public static final String ENTITY_ANNOTATIONS 	= ENTITY_ID+ANNOTATIONS;

	/**
	 * All of the base URLs for locationable entity s3Tokens
	 */
	public static final String ENTITY_S3TOKEN	= ENTITY_ID+S3TOKEN;
	
	/**
	 * Used to get a user profile attachment token
	 */
	public static final String USER_PROFILE_S3_ATTACHMENT_TOKEN = USER_PROFILE_ID+ATTACHMENT_S3_TOKEN;
	
	/**
	 * All of the base URLs for Synapse objects's paths.
	 */
	public static final String ENTITY_PATH		= ENTITY_ID+PATH;
	
	
	/**
	 * All of the base URLs for Synapse object's versions.
	 */
	public static final String ENTITY_VERSION		= ENTITY_ID+VERSION;

	
	public static final String ENTITY_VERSION_NUMBER		= ENTITY_VERSION+VERSION_NUMBER;
	
	/**
	 * Get the annotations of a specific version of an entity
	 */
	public static final String ENTITY_VERSION_ANNOTATIONS =		ENTITY_VERSION_NUMBER+ANNOTATIONS;

	/**
	 * Get the bundle for a specific version of an entity
	 */
	public static final String ENTITY_VERSION_NUMBER_BUNDLE = ENTITY_VERSION_NUMBER+BUNDLE;

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
	
	public static final String ACCESS_REQUIREMENT = "/accessRequirement";
	public static final String ACCESS_REQUIREMENT_WITH_ENTITY_ID = ENTITY_ID+ACCESS_REQUIREMENT;
	public static final String ACCESS_REQUIREMENT_WITH_REQUIREMENT_ID = ACCESS_REQUIREMENT+"/{requirementId}";
	
	public static final String ACCESS_REQUIREMENT_UNFULFILLED_WITH_ID = ENTITY_ID+"/accessRequirementUnfulfilled";
	
	public static final String ACCESS_APPROVAL = "/accessApproval";
	public static final String ACCESS_APPROVAL_WITH_ENTITY_ID = ENTITY_ID+ACCESS_APPROVAL;
	public static final String ACCESS_APPROVAL_WITH_APPROVAL_ID = ACCESS_APPROVAL+"/{approvalId}";
	
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
	 * Public access for Synapse user and group info
	 */
	public static final String USER_GROUP_HEADERS = "/userGroupHeaders";
	
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
	 * The parameter for a resource name.
	 */
	public static final String RESOURCE_ID = "resourceId";
	
	public static final String GET_ALL_BACKUP_OBJECTS = "/backupObjects";

	/**
	 * Used by AdministrationController service to say whether object dependencies should be calculated
	 * when listing objects to back up.
	 */
	public static final String INCLUDE_DEPENDENCIES_PARAM = "includeDependencies";

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
//	@SuppressWarnings("unchecked")
//	public static String getUrlForModel(Class theModelClass) {
//		EntityType type =  EntityType.getNodeTypeForClass(theModelClass);
//		return type.getUrlPrefix();
//	}

	
	/**
	 * Helper function to create a relative URL for an entity's annotations
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * @param request
	 * @return the uri for this entity's annotations
	 */
	public static String makeEntityAnnotationsUri(String entityId) {
		return ENTITY + "/" + entityId + ANNOTATIONS;
	}
	
	/**
	 * Helper function to create a relative URL for an entity's ACL
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * @param request
	 * @return the uri for this entity's annotations
	 */
	public static String makeEntityACLUri(String entityId) {
		return ENTITY + "/" + entityId + ACL;
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
			uri = urlPrefix + UrlHelpers.ENTITY
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
		builder.append(UrlHelpers.ENTITY);
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
		String expectedBaseSuffix = UrlHelpers.ENTITY +"/"+object.getId();
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
	public static String createACLRedirectURL(HttpServletRequest request, String id){
		if(request == null) throw new IllegalArgumentException("Request cannot be null");
		if(id == null) throw new IllegalArgumentException("ID cannot be null");
		StringBuilder redirectURL = new StringBuilder();
		redirectURL.append(UrlHelpers.getUrlPrefixFromRequest(request));
		redirectURL.append(UrlHelpers.ENTITY);
		redirectURL.append("/");
		redirectURL.append(id);
		redirectURL.append(UrlHelpers.ACL);
		return redirectURL.toString();
	}
	
	public static String getAttachmentTypeURL(ServiceConstants.AttachmentType type)
	{
		if (type == AttachmentType.ENTITY)
			return UrlHelpers.ENTITY;
		else if (type == AttachmentType.USER_PROFILE)
			return UrlHelpers.USER_PROFILE;
		else throw new IllegalArgumentException("Unrecognized attachment type: " + type);
	}
}
