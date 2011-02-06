package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Revisable;
import org.sagebionetworks.repo.model.RevisableDAO;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

abstract public class GAEJDORevisableAnnotatableDAOImpl<S extends Base & Revisable, T extends GAEJDOAnnotatable & GAEJDOBase & GAEJDORevisable<T>>
		extends GAEJDORevisableDAOImpl<S, T> implements RevisableDAO<S>,
		AnnotatableDAO<S> {

	public T cloneJdo(T jdo) {
		T clone = super.cloneJdo(jdo);

		clone.setAnnotations(jdo.getAnnotations().cloneJdo());

		return clone;
	}

	/**
	 * @param id
	 * @return annotations for the given object of the given type
	 */
	public Annotations getAnnotations(String id) throws DatastoreException,
			NotFoundException {
		Annotations ans = new Annotations();
		ans.setStringAnnotations(getStringAnnotationDAO(id).getAnnotations());
		ans.setFloatAnnotations(getFloatAnnotationDAO(id).getAnnotations());
		ans.setDateAnnotations(getDateAnnotationDAO(id).getAnnotations());
		return ans;
	}

	public AnnotationDAO<S, String> getStringAnnotationDAO() {return getStringAnnotationDAO(null);}
	
	public AnnotationDAO<S, String> getStringAnnotationDAO(final String owner) {
		final GAEJDORevisableAnnotatableDAOImpl<S, T> parent = this;
		return new GAEJDORevisableAnnotationDAOImpl<S, T, String>() {
			protected String getOwner() {return owner;}
			
			protected Class<? extends GAEJDOAnnotation<String>> getAnnotationClass() {
				return GAEJDOStringAnnotation.class;
			}

			protected Class<String> getValueClass() {
				return String.class;
			}

			protected String getCollectionName() {
				return "stringAnnotations";
			}

			public S newDTO() {
				return parent.newDTO();
			}

			public T newJDO() {
				return parent.newJDO();
			}

			public void copyToDto(T jdo, S dto) {
				parent.copyToDto(jdo, dto);
			}

			public void copyFromDto(S dto, T jdo) throws InvalidModelException {
				parent.copyFromDto(dto, jdo);
			}

			protected Class<T> getOwnerClass() {
				return parent.getJdoClass();
			}

			protected void addAnnotation(GAEJDOAnnotations annots,
					String attribute, String value) {
				annots.add(attribute, value);
			}

			protected void removeAnnotation(GAEJDOAnnotations annots,
					String attribute, String value) {
				annots.remove(attribute, value);
			}

			protected Iterable<GAEJDOAnnotation<String>> getIterable(
					GAEJDOAnnotations annots) {
				return annots.getStringIterable();
			}
		};
	}

	public AnnotationDAO<S, Float> getFloatAnnotationDAO() {return getFloatAnnotationDAO(null);}
	
	public AnnotationDAO<S, Float> getFloatAnnotationDAO(final String owner) {
		final GAEJDORevisableAnnotatableDAOImpl<S, T> parent = this;
		return new GAEJDORevisableAnnotationDAOImpl<S, T, Float>() {
			protected String getOwner() {return owner;}
			
			protected Class<? extends GAEJDOAnnotation<Float>> getAnnotationClass() {
				return GAEJDOFloatAnnotation.class;
			}

			protected Class<Float> getValueClass() {
				return Float.class;
			}

			protected String getCollectionName() {
				return "floatAnnotations";
			}

			public S newDTO() {
				return parent.newDTO();
			}

			public T newJDO() {
				return parent.newJDO();
			}

			public void copyToDto(T jdo, S dto) {
				parent.copyToDto(jdo, dto);
			}

			public void copyFromDto(S dto, T jdo) throws InvalidModelException {
				parent.copyFromDto(dto, jdo);
			}

			protected Class<T> getOwnerClass() {
				return parent.getJdoClass();
			}

			protected void addAnnotation(GAEJDOAnnotations annots,
					String attribute, Float value) {
				annots.add(attribute, value);
			}

			protected void removeAnnotation(GAEJDOAnnotations annots,
					String attribute, Float value) {
				annots.remove(attribute, value);
			}

			protected Iterable<GAEJDOAnnotation<Float>> getIterable(
					GAEJDOAnnotations annots) {
				return annots.getFloatIterable();
			}
		};
	}

	public AnnotationDAO<S, Date> getDateAnnotationDAO() {return getDateAnnotationDAO(null);}
	
	public AnnotationDAO<S, Date> getDateAnnotationDAO(final String owner) {
		final GAEJDORevisableAnnotatableDAOImpl<S, T> parent = this;
		return new GAEJDORevisableAnnotationDAOImpl<S, T, Date>() {
			protected String getOwner() {return owner;}
			
			protected Class<? extends GAEJDOAnnotation<Date>> getAnnotationClass() {
				return GAEJDODateAnnotation.class;
			}

			protected Class<Date> getValueClass() {
				return Date.class;
			}

			protected String getCollectionName() {
				return "dateAnnotations";
			}

			public S newDTO() {
				return parent.newDTO();
			}

			public T newJDO() {
				return parent.newJDO();
			}

			public void copyToDto(T jdo, S dto) {
				parent.copyToDto(jdo, dto);
			}

			public void copyFromDto(S dto, T jdo) throws InvalidModelException {
				parent.copyFromDto(dto, jdo);
			}

			protected Class<T> getOwnerClass() {
				return parent.getJdoClass();
			}

			protected void addAnnotation(GAEJDOAnnotations annots,
					String attribute, Date value) {
				annots.add(attribute, value);
			}

			protected void removeAnnotation(GAEJDOAnnotations annots,
					String attribute, Date value) {
				annots.remove(attribute, value);
			}

			protected Iterable<GAEJDOAnnotation<Date>> getIterable(
					GAEJDOAnnotations annots) {
				return annots.getDateIterable();
			}
		};
	}

}
