package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.gaejdo.GAEJDOAnnotatable;

import com.google.appengine.api.datastore.Text;

public interface AnnotatableDAO<S extends Base> {

	/**
	 * @param id
	 * @return annotations for the given object of the given type
	 */
	public Annotations getAnnotations(String id) throws DatastoreException ;
	
	public AnnotationDAO<S, String> getStringAnnotationDAO();
	public AnnotationDAO<S, Float> getFloatAnnotationDAO();
	public AnnotationDAO<S, Date> getDateAnnotationDAO();
	
}
