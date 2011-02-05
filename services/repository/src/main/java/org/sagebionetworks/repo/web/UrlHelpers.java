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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InputDataLayer;

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
@SuppressWarnings("unchecked")
public class UrlHelpers {

	private static final Logger log = Logger.getLogger(UrlHelpers.class
			.getName());

	/**
	 * URL prefix for dataset model objects
	 * 
	 */
	public static final String DATASET = "/dataset";

	/**
	 * URL prefix for dataset layer model objects
	 * 
	 */
	public static final String LAYER = "/layer";

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
	 * Mapping of type to url prefix
	 */
	private static final Map<Class, String> MODEL2URL;

	/**
	 * Mapping of child models to their parent models
	 */
	private static final Map<Class, Class> CHILD2PARENTMODEL;

	/**
	 * Mapping of child urls to their parent urls
	 */
	private static final Map<String, String> CHILD2PARENTURL;

	// This is a memoized cache for our URL regular expressions
	private static Map<Class, Pattern> MODEL2REGEX = new HashMap();

	static {
		Map<Class, String> model2url = new HashMap<Class, String>();
		model2url.put(Dataset.class, DATASET);
		model2url.put(InputDataLayer.class, LAYER);
		MODEL2URL = Collections.unmodifiableMap(model2url);

		// TODO we don't need all these maps, and for those that we do, we can
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
	public static String makeEntityAnnotationsUri(Class entityClass,
			Annotations annotations, HttpServletRequest request) {

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
	 * @param request
	 * @return the uri for this entity's annotations
	 */
	public static String makeEntityAnnotationsUri(Base entity,
			HttpServletRequest request) {

		String urlPrefix = getUrlPrefix(entity, request);

		String uri = null;
		try {
			uri = urlPrefix + UrlHelpers.getUrlForModel(entity.getClass())
					+ "/" + URLEncoder.encode(entity.getId(), "UTF-8")
					+ ANNOTATIONS;
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
}
