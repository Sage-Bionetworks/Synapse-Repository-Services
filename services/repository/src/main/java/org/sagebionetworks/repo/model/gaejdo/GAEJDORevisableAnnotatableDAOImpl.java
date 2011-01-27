package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Revisable;
import org.sagebionetworks.repo.model.RevisableDAO;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

abstract public class GAEJDORevisableAnnotatableDAOImpl
	<S extends Base & Revisable, T extends GAEJDOAnnotatable & GAEJDOBase & GAEJDORevisable<T>> implements BaseDAO<S>,
		RevisableDAO<S>, AnnotatableDAO<S> {


	protected GAEJDOBaseDAOHelper<S, T> baseDAO=null;
	protected GAEJDORevisableDAOHelper<S, T> revisableDAO=null;


	public GAEJDORevisableAnnotatableDAOImpl() {
		final GAEJDORevisableAnnotatableDAOImpl<S,T> parent = this;
		revisableDAO = new GAEJDORevisableDAOHelper<S, T>() {
			public Class<T> getJdoClass() {return parent.getJdoClass();}

			public T newJDO() {return parent.newJDO();}

			public S newDTO() {
				return parent.newDTO();
			}

			public void copyToDto(T gae, S dto) {
				parent.copyToDto(gae, dto);
			}

			public void copyFromDto(S dto, T gae)
					throws InvalidModelException {
				parent.copyFromDto(dto, gae);
			}
		};
		baseDAO = new GAEJDOBaseDAOHelper<S, T>() {
			public Class<T> getJdoClass() {return parent.getJdoClass();}

			public T newJDO() {return parent.newJDO();}

			public S newDTO() {
				return parent.newDTO();
			}

			public void copyToDto(T gae, S dto) {
				parent.copyToDto(gae, dto);
			}

			public void copyFromDto(S dto, T gae)
					throws InvalidModelException {
				parent.copyFromDto(dto, gae);
			}
		};
	}

	/**
	 * Create a new instance of the data transfer object.  
	 * Introducing this abstract method helps us avoid making assumptions about constructors.
	 * @return
	 */
	abstract public S newDTO();

	/**
	 * Create a new instance of the persistable object.
	 * Introducing this abstract method helps us avoid making assumptions about constructors.
	 * @return
	 */
	abstract public T newJDO();
	
	abstract public Class<T> getJdoClass();

	/**
	 * Do a shallow copy from the JDO object to the DTO object.
	 * 
	 * @param jdo
	 * @param dto
	 */
	abstract public void copyToDto(T jdo, S dto);

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	abstract public void copyFromDto(S dto, T jdo) throws InvalidModelException;

	public List<S> getInRange(int start, int end) throws DatastoreException {
		return revisableDAO.getInRange(start, end);
	}

	public List<S> getInRangeSortedByPrimaryField(int start, int end,
			String sortBy, boolean asc) throws DatastoreException {
		return revisableDAO.getInRangeSortedByPrimaryField(start, end, sortBy,
				asc);
	}

	public List<S> getInRangeHavingPrimaryField(int start, int end,
			String attribute, Object value) throws DatastoreException {
		return revisableDAO.getInRangeHavingPrimaryField(start, end, attribute,
				value);
	}

	// Question: is this the right spot for this sort of constant? 
	private static final String DEFAULT_VERSION = "0.0.1";

	/**
	 * @param dto
	 *            an original (not revised) dto
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(S dto) throws DatastoreException,
			InvalidModelException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();

			//
			// Set system-controlled immutable fields
			//
			// Question: is this where we want to be setting immutable
			// system-controlled fields for our
			// objects? This should only be set at creation time so its not
			// appropriate to put it in copyFromDTO.
			dto.setCreationDate(new Date()); // now

			//
			// Set default values for optional fields that have defaults
			//
			// Question: is this where we want to specify reasonable default
			// values?
			if (null == dto.getVersion()) {
				dto.setVersion(DEFAULT_VERSION);
			}

			T jdo = revisableDAO.create(pm, dto);
			tx.commit();
			copyToDto(jdo, dto);
			return KeyFactory.keyToString(jdo.getId());
		} catch (InvalidModelException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public S get(String id) throws DatastoreException, NotFoundException {
		return baseDAO.get(id);
	}

	public void delete(String id) throws DatastoreException, NotFoundException {
		baseDAO.delete(id);

	}

	/**
	 * This updates the 'shallow' properties. Neither Version nor deep
	 * properties change.
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 */
	public void update(S dto) throws DatastoreException, InvalidModelException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			revisableDAO.update(pm, dto);
			tx.commit();
		} catch (InvalidModelException ime) {
			throw ime;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	/**
	 * Create a revision of the object specified by the 'id' and 'version'
	 * fields, having the shallow properties from the given 'revision', and the
	 * deep properties of the given 'version'. The new revision will have the
	 * version given by the 'newVersion' parameter.
	 * 
	 * @param revision
	 * @param newVersion
	 * @param revisionDate
	 */
	public String revise(S revision, Date revisionDate)
			throws DatastoreException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			T newRevision = revisableDAO.revise(pm, revision,
					revisionDate);
			// now copy the 'deep' properties
			Key reviseeId = KeyFactory.stringToKey(revision.getId());
			@SuppressWarnings("unchecked")
			T revisee = (T) pm.getObjectId(reviseeId);
			GAEJDOAnnotations a = GAEJDOAnnotations.clone(revisee
					.getAnnotations());
			newRevision.setAnnotations(a);
			// TODO move this to GAEJDODataset class
			//newRevision.setLayers(new HashSet<Key>(revisee.getLayers()));
			pm.makePersistent(newRevision); // don't know if this is necessary
			tx.commit();
			return KeyFactory.keyToString(newRevision.getId());
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public int getCount() throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			int count = revisableDAO.getCount(pm);
			return count;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * 
	 * @param id
	 *            the id of any revision of the object
	 * @return the latest version of the object
	 * @throws DatastoreException
	 *             if no result
	 */
	public S getLatest(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			S latest = revisableDAO.getLatest(pm, id);
			return latest;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Get all versions of an object
	 * 
	 * @param id
	 * @return all revisions of the given object
	 */
	public Collection<S> getAllVersions(String id)
			throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Collection<S> allVersions = revisableDAO.getAllVersions(pm,
					id);
			return allVersions;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Deletes all revisions of a S
	 * 
	 * @param id
	 *            the id of any version of a revision series
	 * @throws DatastoreException
	 */
	public void deleteAllVersions(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Key key = KeyFactory.stringToKey(id);
			Collection<T> allVersions = revisableDAO
					.getAllVersions(pm, key);
			for (T jdo : allVersions) {
				// TODO move this to GAEJDODataset class

//				for (Key layerKey : jdo.getLayers()) {
//					// may have to check whether it's a InputDataLayer or
//					// AnalysisResult
//					TLayer layer = (TLayer) pm
//							.getObjectById(TLayer.class, layerKey);
//					pm.deletePersistent(layer);
//				}
				pm.deletePersistent(jdo);
			}
			tx.commit();
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	/**
	 * @param id
	 * @return annotations for the given object of the given type
	 */
	public Annotations getAnnotations(String id) throws DatastoreException, NotFoundException {
		Annotations ans = new Annotations();
		ans.setStringAnnotations(getStringAnnotationDAO().getAnnotations(id));
		ans.setFloatAnnotations(getFloatAnnotationDAO().getAnnotations(id));
		ans.setDateAnnotations(getDateAnnotationDAO().getAnnotations(id));
		return ans;
	}

	public AnnotationDAO<S, String> getStringAnnotationDAO() {
		final GAEJDORevisableAnnotatableDAOImpl<S,T> parent = this;
		return new GAEJDORevisableAnnotationDAOImpl<S, T, String>() {
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

			public void copyFromDto(S dto, T jdo)
					throws InvalidModelException {
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

	public AnnotationDAO<S, Float> getFloatAnnotationDAO() {
		final GAEJDORevisableAnnotatableDAOImpl<S,T> parent = this;
		return new GAEJDORevisableAnnotationDAOImpl<S, T, Float>() {

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

			public void copyFromDto(S dto, T jdo)
					throws InvalidModelException {
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

	public AnnotationDAO<S, Date> getDateAnnotationDAO() {
		final GAEJDORevisableAnnotatableDAOImpl<S,T> parent = this;
		return new GAEJDORevisableAnnotationDAOImpl<S, T, Date>() {
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

			public void copyFromDto(S dto, T jdo)
					throws InvalidModelException {
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
