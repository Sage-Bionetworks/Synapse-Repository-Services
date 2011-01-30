package org.sagebionetworks.repo.web;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
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

	private AnnotatableDAO<T> annotatableDao;
	private AnnotationDAO<T, String> stringAnnotationDAO;
	private AnnotationDAO<T, Float> floatAnnotationDAO;
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

		stringAnnotationDAO = annotatableDao.getStringAnnotationDAO();
		floatAnnotationDAO = annotatableDao.getFloatAnnotationDAO();
		dateAnnotationDAO = annotatableDao.getDateAnnotationDAO();

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
			Object value) {
		throw new RuntimeException("Not implemented yet");
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
				entities = stringAnnotationDAO.getInRangeSortedBy(offset - 1,
						offset + limit - 1, sort, ascending);

				// TODO need to find a good way to infer the type
				// entities = floatAnnotationDAO.getInRangeSortedBy(offset - 1,
				// offset + limit - 1, sort, ascending);
				// entities = dateAnnotationDAO.getInRangeSortedBy(offset - 1,
				// offset + limit - 1, sort, ascending);
			}
		}
		return entities;
	}
}
