package org.sagebionetworks.repo.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.web.controller.AbstractAnnotatableEntityController;

/**
 * Implementation for REST controller for CRUD operations on Annotation DTOs and
 * DAOs
 * <p>
 * 
 * This class performs the basic CRUD operations for all our DAO-backed
 * annotatable model objects. See controllers specific to particular models for
 * any special handling.
 * <p>
 * 
 * TODO this patient is still lying open on the operating table, don't CR it yet
 * 
 * @author deflaux
 * @param <T>
 */
public class AnnotatableDAOControllerImp<T extends Base> implements
		AbstractAnnotatableEntityController<T> {

	private static final Logger log = Logger
			.getLogger(AnnotatableDAOControllerImp.class.getName());

	protected Class<T> theModelClass;
	protected BaseDAO<T> dao;
	protected AnnotatableDAO<T> annotatableDao;

	/**
	 * @param theModelClass
	 */
	@SuppressWarnings("unchecked")
	public AnnotatableDAOControllerImp(Class<T> theModelClass) {
		this.theModelClass = theModelClass;
		// TODO @Autowired, no GAE references allowed in this class
		DAOFactory daoFactory = new GAEJDODAOFactoryImpl();
		this.dao = daoFactory.getDAO(theModelClass);
		this.annotatableDao = (AnnotatableDAO<T>) daoFactory
				.getDAO(theModelClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractAnnotatableEntityController
	 * #getEntity(java.lang.String)
	 */
	public Annotations getEntityAnnotations(String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException {

		String entityId = UrlPrefixes.getEntityIdFromUriId(id);

		Annotations annotations = annotatableDao.getAnnotations(entityId);
		if (null == annotations) {
			throw new NotFoundException("no entity with id " + entityId
					+ " exists");
		}

		annotations.setId(entityId); // the url-encoded id
		annotations.setUri(UrlPrefixes.makeEntityAnnotationsUri(theModelClass, annotations, request));
		annotations.setEtag(UrlPrefixes.makeEntityEtag(annotations));

		return annotations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractAnnotatableEntityController
	 * #updateEntity(java.lang.String, java.lang.Integer, Annotations)
	 */
	public Annotations updateEntityAnnotations(String id, Integer etag,
			Annotations updatedAnnotations, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException {

		String entityId = UrlPrefixes.getEntityIdFromUriId(id);
		//
		// Annotations entity = dao.get(entityId);
		// if(null == entity) {
		// throw new NotFoundException("no entity with id " + entityId +
		// " exists");
		// }
		// if(etag != entity.hashCode()) {
		// throw new ConflictingUpdateException("entity with id " + entityId
		// +
		// "was updated since you last fetched it, retrieve it again and reapply the update");
		// }
		// dao.update(updatedAnnotations);
		//

		// TODO this isn't how we want to do this for real
		// TODO is this additive or overwriting?

		Map<String, Collection<String>> stringAnnotations = updatedAnnotations
				.getStringAnnotations();
		AnnotationDAO<T, String> foo = annotatableDao.getStringAnnotationDAO();
		for (Map.Entry<String, Collection<String>> annotation : stringAnnotations
				.entrySet()) {
			for (String value : annotation.getValue()) {
				log.fine("Adding string annotation (" + annotation.getKey()
						+ ", " + value + ")");
				foo.addAnnotation(entityId, annotation.getKey(), value);
			}
		}

		Map<String, Collection<Float>> floatAnnotations = updatedAnnotations
				.getFloatAnnotations();
		AnnotationDAO<T, Float> bar = annotatableDao.getFloatAnnotationDAO();
		for (Map.Entry<String, Collection<Float>> annotation : floatAnnotations
				.entrySet()) {
			for (Float value : annotation.getValue()) {
				log.fine("Adding float annotation (" + annotation.getKey()
						+ ", " + value + ")");
				bar.addAnnotation(entityId, annotation.getKey(), value);
			}
		}

		Map<String, Collection<Date>> dateAnnotations = updatedAnnotations
				.getDateAnnotations();
		AnnotationDAO<T, Date> baz = annotatableDao.getDateAnnotationDAO();
		for (Map.Entry<String, Collection<Date>> annotation : dateAnnotations
				.entrySet()) {
			for (Date value : annotation.getValue()) {
				log.fine("Adding date annotation (" + annotation.getKey()
						+ ", " + value + ")");
				baz.addAnnotation(entityId, annotation.getKey(), value);
			}
		}

		updatedAnnotations.setId(entityId); // the url-encoded id
		updatedAnnotations.setUri(UrlPrefixes.makeEntityAnnotationsUri(theModelClass, updatedAnnotations, request));
		updatedAnnotations.setEtag(UrlPrefixes.makeEntityEtag(updatedAnnotations));

		return updatedAnnotations;
	}

}
