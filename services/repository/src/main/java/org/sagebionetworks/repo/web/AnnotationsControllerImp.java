package org.sagebionetworks.repo.web;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.controller.AnnotationsController;

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
 *            the particular type of entity the controller is managing
 */
public class AnnotationsControllerImp<T extends Base> implements
		AnnotationsController<T> {

	private static final Logger log = Logger
			.getLogger(AnnotationsControllerImp.class.getName());

	private Class<T> theModelClass;
	private AnnotatableDAO<T> annotatableDao;
	private AnnotationDAO<T, String> stringAnnotationDAO;
	private AnnotationDAO<T, Float> floatAnnotationDAO;
	private AnnotationDAO<T, Date> dateAnnotationDAO;

	/**
	 * @param theModelClass
	 */
	public AnnotationsControllerImp(Class<T> theModelClass) {
		this.theModelClass = theModelClass;
	}

	@Override
	public void setDao(BaseDAO<T> dao) {
		annotatableDao = (AnnotatableDAO<T>) dao;
		stringAnnotationDAO = annotatableDao.getStringAnnotationDAO();
		floatAnnotationDAO = annotatableDao.getFloatAnnotationDAO();
		dateAnnotationDAO = annotatableDao.getDateAnnotationDAO();
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

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		Annotations annotations = annotatableDao.getAnnotations(entityId);
		if (null == annotations) {
			throw new NotFoundException("no entity with id " + entityId
					+ " exists");
		}

		addServiceSpecificMetadata(id, annotations, request);

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

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		Annotations annotations = annotatableDao.getAnnotations(entityId);
		if (null == annotations) {
			throw new NotFoundException("no entity with id " + entityId
					+ " exists");
		}

		if (etag != annotations.hashCode()) {
			throw new ConflictingUpdateException(
					"annotations for entity with id "
							+ entityId
							+ "were updated since you last fetched them, retrieve them again and reapply the update");
		}

		// dao.update(updatedAnnotations);

		// TODO this isn't how we want to do this for real
		// TODO this is currently additive but it should be overwriting
		// Developer Note: yes, nested loops are evil when N is large

		Map<String, Collection<String>> updatedStringAnnotations = updatedAnnotations
				.getStringAnnotations();
		for (Map.Entry<String, Collection<String>> updatedAnnotation : updatedStringAnnotations
				.entrySet()) {
			for (String value : updatedAnnotation.getValue()) {
				log.info("Adding string annotation ("
						+ updatedAnnotation.getKey() + ", " + value + ")");
				stringAnnotationDAO.addAnnotation(entityId, updatedAnnotation
						.getKey(), value);
			}
		}

		Map<String, Collection<Float>> updatedFloatAnnotations = updatedAnnotations
				.getFloatAnnotations();
		for (Map.Entry<String, Collection<Float>> updatedAnnotation : updatedFloatAnnotations
				.entrySet()) {
			for (Float value : updatedAnnotation.getValue()) {
				log.info("Adding float annotation ("
						+ updatedAnnotation.getKey() + ", " + value + ")");
				floatAnnotationDAO.addAnnotation(entityId, updatedAnnotation
						.getKey(), value);
			}
		}

		Map<String, Collection<Date>> updatedDateAnnotations = updatedAnnotations
				.getDateAnnotations();
		for (Map.Entry<String, Collection<Date>> updatedAnnotation : updatedDateAnnotations
				.entrySet()) {
			for (Date value : updatedAnnotation.getValue()) {
				log.info("Adding date annotation ("
						+ updatedAnnotation.getKey() + ", " + value + ")");
				dateAnnotationDAO.addAnnotation(entityId, updatedAnnotation
						.getKey(), value);
			}
		}

		addServiceSpecificMetadata(id, updatedAnnotations, request);

		return updatedAnnotations;
	}

	private void addServiceSpecificMetadata(String id, Annotations annotations,
			HttpServletRequest request) {
		annotations.setId(id); // the NON url-encoded id
		annotations.setUri(UrlHelpers.makeEntityAnnotationsUri(theModelClass,
				annotations, request));
		annotations.setEtag(UrlHelpers.makeEntityEtag(annotations));
	}
}
