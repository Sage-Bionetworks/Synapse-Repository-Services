package org.sagebionetworks.repo.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.Comment;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Message;

/**
 * Mapping of URLs to model classes
 * 
 * TODO rename this class
 * 
 * @author deflaux
 * 
 */
@SuppressWarnings("unchecked")
public class UrlPrefixes {

	private static final Logger log = Logger.getLogger(UrlPrefixes.class
			.getName());

	/**
	 * URL prefix for comment model objects
	 * 
	 */
	public static final String COMMENT = "/comment";

	/**
	 * URL prefix for dataset model objects
	 * 
	 */
	public static final String DATASET = "/dataset";

	/**
	 * URL prefix for message objects
	 * 
	 */
	public static final String MESSAGE = "/message";

	/**
	 * Mapping of type to url prefix
	 */
	private static final Map<Class, String> MODEL2URL;

	static {
		Map<Class, String> model2url = new HashMap<Class, String>();
		model2url.put(Comment.class, COMMENT);
		model2url.put(Dataset.class, DATASET);
		model2url.put(Message.class, MESSAGE);
		MODEL2URL = Collections.unmodifiableMap(model2url);
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
	 * 
	 * @return The URL prefixes we currently have mapped to model classes
	 */
	public static Collection<String> getAllUrlPrefixes() {
		return MODEL2URL.values();
	}

	/**
	 * Helper function to create a relative URL for an entity
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * @param entity
	 * @param request
	 * @return the relative URI for the entity
	 */
	public static String makeEntityUri(Base entity, HttpServletRequest request) {
		String uri = null;
		try {
			uri = request.getServletPath()
					+ UrlPrefixes.getUrlForModel(entity.getClass()) + "/"
					+ URLEncoder.encode(entity.getId(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE,
					"Something is really messed up if we don't support UTF-8",
					e);
		}
		return uri;
	}

	/**
	 * Helper function to create a relative URL for an entity's annotations
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * @param entityClass 
	 * @param annotations 
	 * @param request
	 * @return the uri for this entity's annotations
	 */
	public static String makeEntityAnnotationsUri(Class entityClass, Annotations annotations,	HttpServletRequest request) {
		String uri = null;
		try {
			uri = request.getServletPath()
					+ UrlPrefixes.getUrlForModel(entityClass) + "/"
					+ URLEncoder.encode(annotations.getId(), "UTF-8") + "/annotations";
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE,
					"Something is really messed up if we don't support UTF-8",
					e);
		}
		return uri;
	}
	
	/**
	 * Helper function to create a relative URL for an entity's annotations
	 * <p>
	 * 
	 * This includes not only the entity id but also the controller and servlet
	 * portions of the path
	 * 
	 * @param entity
	 * @param request
	 * @return the uri for this entity's annotations
	 */
	public static String makeEntityAnnotationsUri(Base entity,	HttpServletRequest request) {
		String uri = null;
		try {
			uri = request.getServletPath()
					+ UrlPrefixes.getUrlForModel(entity.getClass()) + "/"
					+ URLEncoder.encode(entity.getId(), "UTF-8") + "/annotations";
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
	 * Helper function to create values for using in etags for an entity
	 * <p>
	 * 
	 * The current implementation uses hash code since different versions of our
	 * model objects will have different hash code values
	 * 
	 * @param entity
	 * @return the ETag for the entity
	 */
	public static String makeEntityEtag(Base entity) {
		Integer hashCode = entity.hashCode();
		return hashCode.toString();
	}

}
