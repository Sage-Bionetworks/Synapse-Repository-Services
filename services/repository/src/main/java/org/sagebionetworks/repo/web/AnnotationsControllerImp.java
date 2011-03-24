package org.sagebionetworks.repo.web;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.util.SchemaHelper;

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
 * TODO
 * <ol>
 * <li>improve the AnnotationDAO API so that we no longer need to do the loops
 * below when updating an annotations model object in its entirety OR always
 * work with the annotations individually for CUD operations
 * http://sagebionetworks.jira.com/browse/PLFM-64
 * <li>add new functionality to register an annotation with a particular display
 * name, data type and validation rules (e.g. must be a term from ontology X).
 * From then on out if anyone tries to make an annotation of that annotation
 * type, the storage type must match.
 * <li>the annotation type also includes the display name which is separate from
 * the name used in the persistence layer
 * http://sagebionetworks.jira.com/browse/PLFM-65
 * </ol>
 * 
 * @author deflaux
 * @param <T>
 *            the particular type of entity the controller is managing
 */
public class AnnotationsControllerImp<T extends Base> implements
		AnnotationsController<T> {

	// match one or more whitespace characters
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	private AnnotatableDAO<T> annotatableDao;

	@Override
	public void setDao(BaseDAO<T> dao) {
		annotatableDao = (AnnotatableDAO<T>) dao;
	}

	@Override
	public Annotations getEntityAnnotations(String userId, String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		Annotations annotations = annotatableDao.getAnnotations(entityId);
		if (null == annotations) {
			throw new NotFoundException("no entity with id " + entityId
					+ " exists");
		}

		addServiceSpecificMetadata(id, annotations, request);

		return annotations;
	}

	@Override
	public Annotations updateEntityAnnotations(String userId, String id,
			Integer etag, Annotations updatedAnnotations,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException, InvalidModelException {

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
							+ " were updated since you last fetched them, retrieve them again and reapply the update");
		}

		// TODO below isn't how we want to do this for real, it should be like
		// this
		// dao.update(updatedAnnotations);

		// TODO this is currently additive but it should be overwriting
		// Developer Note: yes, nested loops are evil when N is large

		AnnotationDAO<T, String> stringAnnotationDAO = annotatableDao
				.getStringAnnotationDAO(entityId);
		AnnotationDAO<T, Double> doubleAnnotationDAO = annotatableDao
				.getDoubleAnnotationDAO(entityId);
		AnnotationDAO<T, Long> longAnnotationDAO = annotatableDao
				.getLongAnnotationDAO(entityId);
		AnnotationDAO<T, Date> dateAnnotationDAO = annotatableDao
				.getDateAnnotationDAO(entityId);

		Map<String, Collection<String>> updatedStringAnnotations = updatedAnnotations
				.getStringAnnotations();
		for (Map.Entry<String, Collection<String>> updatedAnnotation : updatedStringAnnotations
				.entrySet()) {
			checkAnnotationName(updatedAnnotation.getKey());
			for (String value : updatedAnnotation.getValue()) {
				stringAnnotationDAO.addAnnotation(updatedAnnotation.getKey(),
						value);
			}
		}

		Map<String, Collection<Double>> updatedDoubleAnnotations = updatedAnnotations
				.getDoubleAnnotations();
		for (Map.Entry<String, Collection<Double>> updatedAnnotation : updatedDoubleAnnotations
				.entrySet()) {
			checkAnnotationName(updatedAnnotation.getKey());
			for (Double value : updatedAnnotation.getValue()) {
				doubleAnnotationDAO.addAnnotation(updatedAnnotation.getKey(),
						value);
			}
		}

		Map<String, Collection<Long>> updatedLongAnnotations = updatedAnnotations
				.getLongAnnotations();
		for (Map.Entry<String, Collection<Long>> updatedAnnotation : updatedLongAnnotations
				.entrySet()) {
			checkAnnotationName(updatedAnnotation.getKey());
			for (Long value : updatedAnnotation.getValue()) {
				longAnnotationDAO.addAnnotation(updatedAnnotation.getKey(),
						value);
			}
		}

		Map<String, Collection<Date>> updatedDateAnnotations = updatedAnnotations
				.getDateAnnotations();
		for (Map.Entry<String, Collection<Date>> updatedAnnotation : updatedDateAnnotations
				.entrySet()) {
			checkAnnotationName(updatedAnnotation.getKey());
			for (Date value : updatedAnnotation.getValue()) {
				dateAnnotationDAO.addAnnotation(updatedAnnotation.getKey(),
						value);
			}
		}

		addServiceSpecificMetadata(id, updatedAnnotations, request);

		return updatedAnnotations;
	}

	@Override
	public JsonSchema getEntityAnnotationsSchema() throws DatastoreException {
		return SchemaHelper.getSchema(Annotations.class);
	}

	private void addServiceSpecificMetadata(String id, Annotations annotations,
			HttpServletRequest request) {
		annotations.setId(id); // the NON url-encoded id
		annotations.setUri(UrlHelpers.makeEntityPropertyUri(request));
		annotations.setEtag(UrlHelpers.makeEntityEtag(annotations));
	}

	private void checkAnnotationName(String key) throws InvalidModelException {
		Matcher matcher = WHITESPACE_PATTERN.matcher(key);
		if (matcher.find()) {
			throw new InvalidModelException(
					"Annotation names may not contain whitespace");
		}
		// TODO eventually deeper in the Annotation DAO is where we might
		// confirm that this annotation has been registered and the value is
		// valid and perhaps allow users to refer to annotations either by their
		// display name or persistence key
		return;
	}
}
