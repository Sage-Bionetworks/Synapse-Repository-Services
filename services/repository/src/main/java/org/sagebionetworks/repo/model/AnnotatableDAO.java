package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.gaejdo.GAEJDOAnnotatable;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Text;

/**
 * Any DAO for an annotatable class extends this interface.
 * 
 * @author bhoff
 * 
 * @param <S>
 *            The DTO for the annotatable class
 */
public interface AnnotatableDAO<S extends Base> extends BaseDAO<S> {

	/**
	 * @param id
	 * @return annotations for the given object, regardless of type
	 * @throws NotFoundException 
	 */
	public Annotations getAnnotations(String id) throws DatastoreException, NotFoundException;

	/**
	 * @return the DAO which provides the CRUD methods for annotations of a
	 *         particular type.
	 */
	public AnnotationDAO<S, String> getStringAnnotationDAO();

	/**
	 * @return the DAO which provides the CRUD methods for annotations of a
	 *         particular type.
	 */
	public AnnotationDAO<S, Float> getFloatAnnotationDAO();

	/**
	 * @return the DAO which provides the CRUD methods for annotations of a
	 *         particular type.
	 */
	public AnnotationDAO<S, Date> getDateAnnotationDAO();

}
