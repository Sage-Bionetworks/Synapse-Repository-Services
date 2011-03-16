package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Revisable;
import org.sagebionetworks.repo.model.RevisableDAO;
import org.sagebionetworks.repo.web.NotFoundException;




abstract public class JDORevisableAnnotatableDAOImpl<S extends Base & Revisable, T extends JDOAnnotatable & JDOBase & JDORevisable<T>>
		extends JDORevisableDAOImpl<S, T> implements RevisableDAO<S>,
		AnnotatableDAO<S> {
	
	public JDORevisableAnnotatableDAOImpl(String userId) {super(userId);}

	public T cloneJdo(T jdo) throws DatastoreException {
		T clone = super.cloneJdo(jdo);

		clone.setAnnotations(jdo.getAnnotations().cloneJdo());

		return clone;
	}

	/**
	 * @param id
	 * @return annotations for the given object
	 */
	public Annotations getAnnotations(String id) throws DatastoreException,
			NotFoundException {
		Annotations ans = new Annotations();
		ans.setStringAnnotations(getStringAnnotationDAO(id).getAnnotations());
		ans.setDoubleAnnotations(getDoubleAnnotationDAO(id).getAnnotations());
		ans.setLongAnnotations(getLongAnnotationDAO(id).getAnnotations());
		ans.setDateAnnotations(getDateAnnotationDAO(id).getAnnotations());
		return ans;
	}

	public AnnotationDAO<S, String> getStringAnnotationDAO() {return getStringAnnotationDAO(null);}
	
	public AnnotationDAO<S, String> getStringAnnotationDAO(final String owner) {
		final JDORevisableAnnotatableDAOImpl<S, T> parent = this;
		return new JDORevisableAnnotationDAOImpl<S, T, String>() {
			protected String getOwner() {return owner;}
			
			protected Class<? extends JDOAnnotation<String>> getAnnotationClass() {
				return JDOStringAnnotation.class;
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

			public void copyToDto(T jdo, S dto) throws DatastoreException {
				parent.copyToDto(jdo, dto);
			}

			public void copyFromDto(S dto, T jdo) throws InvalidModelException {
				parent.copyFromDto(dto, jdo);
			}

			protected Class<T> getOwnerClass() {
				return parent.getJdoClass();
			}

			protected void addAnnotation(JDOAnnotations annots,
					String attribute, String value) {
				annots.add(attribute, value);
			}

			protected void removeAnnotation(JDOAnnotations annots,
					String attribute, String value) {
				annots.remove(attribute, value);
			}

			protected Iterable<JDOAnnotation<String>> getIterable(
					JDOAnnotations annots) {
				return annots.getStringIterable();
			}
		};
	}

	public AnnotationDAO<S, Double> getDoubleAnnotationDAO() {return getDoubleAnnotationDAO(null);}
	
	public AnnotationDAO<S, Double> getDoubleAnnotationDAO(final String owner) {
		final JDORevisableAnnotatableDAOImpl<S, T> parent = this;
		return new JDORevisableAnnotationDAOImpl<S, T, Double>() {
			protected String getOwner() {return owner;}
			
			protected Class<? extends JDOAnnotation<Double>> getAnnotationClass() {
				return JDODoubleAnnotation.class;
			}

			protected Class<Double> getValueClass() {
				return Double.class;
			}

			protected String getCollectionName() {
				return "doubleAnnotations";
			}

			public S newDTO() {
				return parent.newDTO();
			}

			public T newJDO() {
				return parent.newJDO();
			}

			public void copyToDto(T jdo, S dto) throws DatastoreException {
				parent.copyToDto(jdo, dto);
			}

			public void copyFromDto(S dto, T jdo) throws InvalidModelException {
				parent.copyFromDto(dto, jdo);
			}

			protected Class<T> getOwnerClass() {
				return parent.getJdoClass();
			}

			protected void addAnnotation(JDOAnnotations annots,
					String attribute, Double value) {
				annots.add(attribute, value);
			}

			protected void removeAnnotation(JDOAnnotations annots,
					String attribute, Double value) {
				annots.remove(attribute, value);
			}

			protected Iterable<JDOAnnotation<Double>> getIterable(
					JDOAnnotations annots) {
				return annots.getDoubleIterable();
			}
		};
	}

	public AnnotationDAO<S, Long> getLongAnnotationDAO() {return getLongAnnotationDAO(null);}
	
	public AnnotationDAO<S, Long> getLongAnnotationDAO(final String owner) {
		final JDORevisableAnnotatableDAOImpl<S, T> parent = this;
		return new JDORevisableAnnotationDAOImpl<S, T, Long>() {
			protected String getOwner() {return owner;}
			
			protected Class<? extends JDOAnnotation<Long>> getAnnotationClass() {
				return JDOLongAnnotation.class;
			}

			protected Class<Long> getValueClass() {
				return Long.class;
			}

			protected String getCollectionName() {
				return "longAnnotations";
			}

			public S newDTO() {
				return parent.newDTO();
			}

			public T newJDO() {
				return parent.newJDO();
			}

			public void copyToDto(T jdo, S dto) throws DatastoreException {
				parent.copyToDto(jdo, dto);
			}

			public void copyFromDto(S dto, T jdo) throws InvalidModelException {
				parent.copyFromDto(dto, jdo);
			}

			protected Class<T> getOwnerClass() {
				return parent.getJdoClass();
			}

			protected void addAnnotation(JDOAnnotations annots,
					String attribute, Long value) {
				annots.add(attribute, value);
			}

			protected void removeAnnotation(JDOAnnotations annots,
					String attribute, Long value) {
				annots.remove(attribute, value);
			}

			protected Iterable<JDOAnnotation<Long>> getIterable(
					JDOAnnotations annots) {
				return annots.getLongIterable();
			}
		};
	}

	public AnnotationDAO<S, Date> getDateAnnotationDAO() {return getDateAnnotationDAO(null);}
	
	public AnnotationDAO<S, Date> getDateAnnotationDAO(final String owner) {
		final JDORevisableAnnotatableDAOImpl<S, T> parent = this;
		return new JDORevisableAnnotationDAOImpl<S, T, Date>() {
			protected String getOwner() {return owner;}
			
			protected Class<? extends JDOAnnotation<Date>> getAnnotationClass() {
				return JDODateAnnotation.class;
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

			public void copyToDto(T jdo, S dto) throws DatastoreException {
				parent.copyToDto(jdo, dto);
			}

			public void copyFromDto(S dto, T jdo) throws InvalidModelException {
				parent.copyFromDto(dto, jdo);
			}

			protected Class<T> getOwnerClass() {
				return parent.getJdoClass();
			}

			protected void addAnnotation(JDOAnnotations annots,
					String attribute, Date value) {
				annots.add(attribute, value);
			}

			protected void removeAnnotation(JDOAnnotations annots,
					String attribute, Date value) {
				annots.remove(attribute, value);
			}

			protected Iterable<JDOAnnotation<Date>> getIterable(
					JDOAnnotations annots) {
				return annots.getDateIterable();
			}
		};
	}

}
