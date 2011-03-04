package org.sagebionetworks.repo.web;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;

/**
 * Accessor for annotatable entities
 * 
 * @author deflaux
 * @param <T>
 *            the particular type of entity we are querying
 * 
 */
public class AnnotatableEntitiesAccessorImpl<T extends Base> implements
		EntitiesAccessor<T> {

	private static final int MAX_ANNOTATIONS = 50000000; // MySQL's upper bound
															// on LIMIT
	private AnnotatableDAO<T> annotatableDao;
	private AnnotationDAO<T, String> stringAnnotationDAO;
	private AnnotationDAO<T, Double> doubleAnnotationDAO;
	private AnnotationDAO<T, Long> longAnnotationDAO;
	private AnnotationDAO<T, Date> dateAnnotationDAO;
	private Set<String> primaryFields = new HashSet<String>();

	/**
	 * Default constructor
	 */
	public AnnotatableEntitiesAccessorImpl() {
	}

	/**
	 * @param annotatableDao
	 */
	public AnnotatableEntitiesAccessorImpl(AnnotatableDAO<T> annotatableDao) {
		this.setDao(annotatableDao);
	}

	@Override
	public void setDao(BaseDAO<T> dao) {
		this.annotatableDao = (AnnotatableDAO<T>) dao;

		stringAnnotationDAO = annotatableDao.getStringAnnotationDAO(null);
		doubleAnnotationDAO = annotatableDao.getDoubleAnnotationDAO(null);
		longAnnotationDAO = annotatableDao.getLongAnnotationDAO(null);
		dateAnnotationDAO = annotatableDao.getDateAnnotationDAO(null);

		primaryFields.addAll(annotatableDao.getPrimaryFields());
	}

	@Override
	public List<T> getInRange(int offset, int limit) throws DatastoreException {
		List<T> entities = null;
		entities = annotatableDao.getInRange(offset - 1, offset + limit - 1);
		return entities;
	}

	@Override
	public List<T> getInRangeHaving(int offset, int limit, String attribute,
			Object value) throws DatastoreException {
		List<T> entities = null;

		if (primaryFields.contains(attribute)) {

			entities = annotatableDao.getInRangeHavingPrimaryField(offset - 1,
					offset + limit - 1, attribute, value);

		} else {

			// TODO this code goes with the hack below, to be addressed at a
			// later date
			Map<String, Integer> annotationFields = getAnnotationFields();
			Integer type = annotationFields.get(attribute);
			if (null == type) {
				throw new IllegalArgumentException("Field '" + attribute
						+ "' is not valid because there is no primary"
						+ " field or annotation with that name");
			} else if (1 == type) {
				entities = doubleAnnotationDAO.getInRangeHaving(offset - 1,
						offset + limit - 1, attribute, (Double) value);
			} else if (2 == type) {
				entities = longAnnotationDAO.getInRangeHaving(offset - 1,
						offset + limit - 1, attribute, (Long) value);
			} else if (3 == type) {
				entities = dateAnnotationDAO.getInRangeHaving(offset - 1,
						offset + limit - 1, attribute, (Date) value);
			} else {
				entities = stringAnnotationDAO.getInRangeHaving(offset - 1,
						offset + limit - 1, attribute, (String) value);
			}
		}
		return entities;
	}

	@Override
	public List<T> getInRangeSortedBy(int offset, int limit, String sort,
			Boolean ascending) throws DatastoreException {
		List<T> entities = null;

		if (ServiceConstants.DEFAULT_SORT_BY_PARAM.equals(sort)) {
			// The default is to not sort
			entities = annotatableDao
					.getInRange(offset - 1, offset + limit - 1);
		} else {
			if (primaryFields.contains(sort)) {

				entities = annotatableDao.getInRangeSortedByPrimaryField(
						offset - 1, offset + limit - 1, sort, ascending);

			} else {

				// TODO this code goes with the hack below, to be addressed at a
				// later date
				Map<String, Integer> annotationFields = getAnnotationFields();
				Integer type = annotationFields.get(sort);
				if (null == type) {
					throw new IllegalArgumentException("Field '" + sort
							+ "' is not sortable because there is no primary"
							+ " field or annotation with that name");
				} else if (1 == type) {
					entities = doubleAnnotationDAO.getInRangeSortedBy(
							offset - 1, offset + limit - 1, sort, ascending);
				} else if (2 == type) {
					entities = longAnnotationDAO.getInRangeSortedBy(offset - 1,
							offset + limit - 1, sort, ascending);
				} else if (3 == type) {
					entities = dateAnnotationDAO.getInRangeSortedBy(offset - 1,
							offset + limit - 1, sort, ascending);
				} else {
					entities = stringAnnotationDAO.getInRangeSortedBy(
							offset - 1, offset + limit - 1, sort, ascending);
				}
			}
		}
		return entities;
	}

	private Map<String, Integer> getAnnotationFields() {

		/*
		 * TODO this is a big hack and it won't scale, this is merely for demo
		 * purposes and will need to be implemented properly. One way to
		 * implement it property would be when ever we persist a new annotation
		 * to store its key and type. Keep that mapping cached in memory and
		 * persisted in the datastore.
		 */
		Map<String, Integer> annotationFields = new HashMap<String, Integer>();
		List<T> entities = null;
		try {
			entities = annotatableDao.getInRange(0, MAX_ANNOTATIONS);
			for (T entity : entities) {
				Annotations annotations = annotatableDao.getAnnotations(entity
						.getId());

				Map<String, Collection<String>> stringAnnotations = annotations
						.getStringAnnotations();
				for (Map.Entry<String, Collection<String>> annotation : stringAnnotations
						.entrySet()) {
					annotationFields.put(annotation.getKey(), 0);
				}

				Map<String, Collection<Double>> doubleAnnotations = annotations
						.getDoubleAnnotations();
				for (Map.Entry<String, Collection<Double>> annotation : doubleAnnotations
						.entrySet()) {
					annotationFields.put(annotation.getKey(), 1);
				}

				Map<String, Collection<Long>> longAnnotations = annotations
						.getLongAnnotations();
				for (Map.Entry<String, Collection<Long>> annotation : longAnnotations
						.entrySet()) {
					annotationFields.put(annotation.getKey(), 2);
				}

				Map<String, Collection<Date>> dateAnnotations = annotations
						.getDateAnnotations();
				for (Map.Entry<String, Collection<Date>> annotation : dateAnnotations
						.entrySet()) {
					annotationFields.put(annotation.getKey(), 3);
				}
			}
		} catch (DatastoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return annotationFields;
	}
}
