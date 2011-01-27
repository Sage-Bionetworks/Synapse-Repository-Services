/**
 * 
 */
package org.sagebionetworks.repo.web;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;

/**
 * @author deflaux
 * @param <T>
 * 
 */
public class AnnotatableEntitiesAccessorImpl<T extends Base> implements
		EntitiesAccessor<T> {

	private Class<T> theModelClass;
	protected BaseDAO<T> dao;
	private AnnotatableDAO<T> annotatableDao;
	private AnnotationDAO<T, String> stringAnnotationDAO;
	private AnnotationDAO<T, Float> floatAnnotationDAO;
	private AnnotationDAO<T, Date> dateAnnotationDAO;
	private Set<String> primaryFields = new HashSet<String>();

	/**
	 * @param theModelClass
	 */
	@SuppressWarnings("unchecked")
	public AnnotatableEntitiesAccessorImpl(Class<T> theModelClass) {

		this.theModelClass = theModelClass;

		// TODO @Autowired, no GAE references allowed in this class
		DAOFactory daoFactory = new GAEJDODAOFactoryImpl();
		dao = daoFactory.getDAO(theModelClass);
		annotatableDao = (AnnotatableDAO<T>) dao;
		stringAnnotationDAO = annotatableDao.getStringAnnotationDAO();
		floatAnnotationDAO = annotatableDao.getFloatAnnotationDAO();
		dateAnnotationDAO = annotatableDao.getDateAnnotationDAO();

		primaryFields.addAll(dao.getPrimaryFields());
	}

	@Override
	public List<T> getInRange(int offset, int limit) throws DatastoreException {
		List<T> entities = null;
		entities = dao.getInRange(offset - 1, offset + limit - 1);
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
			entities = dao.getInRange(offset - 1, offset + limit - 1);
		} else {
			if (primaryFields.contains(sort)) {
				entities = dao.getInRangeSortedByPrimaryField(offset - 1,
						offset + limit - 1, sort, ascending);
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
