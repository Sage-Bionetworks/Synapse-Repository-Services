package org.sagebionetworks.repo.model.gaejdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnalysisResultDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class GAEJDODatasetDAOImpl implements DatasetDAO {
	
	private GAEJDOBaseDAOHelper<Dataset,GAEJDODataset> baseDAO = null;
	private GAEJDORevisableDAOHelper<Dataset,GAEJDODataset> revisableDAO = null;
	private GAEJDORevisableAnnotatableDAOHelper<Dataset, GAEJDODataset> annotatableDAO = null;
	
	public GAEJDODatasetDAOImpl() {
		final GAEJDODatasetDAOImpl parent = this;
		revisableDAO = new GAEJDORevisableDAOHelper<Dataset,GAEJDODataset>() {
			public Class<GAEJDODataset> getJdoClass() {return GAEJDODataset.class;}
			public GAEJDODataset newJDO() {return parent.newJDO();}
			public Dataset newDTO() {return parent.newDTO();}
			public void copyToDto(GAEJDODataset gae, Dataset dto) {parent.copyToDto(gae, dto);}
			public void copyFromDto(Dataset dto, GAEJDODataset gae) {parent.copyFromDto(dto, gae);}
		};
		baseDAO = new GAEJDOBaseDAOHelper<Dataset,GAEJDODataset>() {
			public Class<GAEJDODataset> getJdoClass() {return GAEJDODataset.class;}
			public GAEJDODataset newJDO() {return parent.newJDO();}
			public Dataset newDTO() {return parent.newDTO();}
			public void copyToDto(GAEJDODataset gae, Dataset dto) {parent.copyToDto(gae, dto);}
			public void copyFromDto(Dataset dto, GAEJDODataset gae) {parent.copyFromDto(dto, gae);}
		};
	}
	
	public Dataset newDTO() {
		Dataset dto = new Dataset();
		return dto;
	}
	
	public GAEJDODataset newJDO() {
		GAEJDODataset jdo = new GAEJDODataset();
		GAEJDOAnnotations a = new GAEJDOAnnotations();
		jdo.setAnnotations(a);
		GAEJDORevision<GAEJDODataset> r = new GAEJDORevision<GAEJDODataset>();
		jdo.setRevision(r);
		return jdo;
	}
	
	public void copyToDto(GAEJDODataset gae, Dataset dto) {
		dto.setId(KeyFactory.keyToString(gae.getId()));
		dto.setName(gae.getName());
		dto.setDescription(gae.getDescription());
		dto.setCreator(gae.getCreator());
		dto.setStatus(gae.getStatus());
		dto.setReleaseDate(gae.getReleaseDate());
		dto.setVersion(gae.getRevision().getVersion().toString());
		Collection<String> layers = new HashSet<String>();
		for (Key l : gae.getLayers()) layers.add(KeyFactory.keyToString(l));
		dto.setLayers(layers);
	}
	
	// Note:  This method does NOT copy layers or revision info to the GAEJDO object,
	// those being done by the 'revise' method
	public void copyFromDto(Dataset dto, GAEJDODataset gae) {
		gae.setName(dto.getName());
		gae.setDescription(dto.getDescription());
		gae.setCreator(dto.getCreator());
		gae.setStatus(dto.getStatus());
		gae.setReleaseDate(dto.getReleaseDate());
	}
	
	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[]{"name", "description", "creator", "status", "releaseDate", "version"});
	}

	public List<Dataset> getInRange(int start, int end) {
		return revisableDAO.getInRange(start, end);
	}
	
	public List<Dataset> getInRangeSortedByPrimaryField(int start, int end, String sortBy, boolean asc) {
		return revisableDAO.getInRangeSortedByPrimaryField(start, end, sortBy, asc);
	}
	
	public List<Dataset> getInRangeHavingPrimaryField(int start, int end, String attribute, Object value) {
		return revisableDAO.getInRangeHavingPrimaryField(start, end, attribute, value);
	}
	

	
	/**
	 * @param dataset an original (not revised) dataset
	 * @return the id of the newly created dataset
	 * @throws DatastoreException
	 */
	public String create(Dataset dataset) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				GAEJDODataset jdo = revisableDAO.create(pm, dataset);
				tx.commit();				
				return KeyFactory.keyToString(jdo.getId());
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}
	
	public Dataset get(String id) throws DatastoreException {
		return baseDAO.get(id);
	}

	public void delete(String id) throws DatastoreException {
		baseDAO.delete(id);
		
	}


	/**
	 * This updates the 'shallow' properties.  Neither Version nor deep properties change.
	 * @param dto non-null id is required
	 * @throws DatastoreException
	 */
	public void update(Dataset dto) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		Transaction tx=null;
		try {
		 	tx=pm.currentTransaction();
			tx.begin();
			revisableDAO.update(pm, dto);
			tx.commit();
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if(tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}	
	}
	
	/**
	 * Create a revision of the object specified by the 'id' and 'version' fields, having
	 * the shallow properties from the given 'revision', and the deep properties
	 * of the given 'version'.  The new revision will have the version given by the
	 * 'newVersion' parameter.
	 * 
	 * @param revision
	 * @param newVersion
	 * @param revisionDate
	 */
	public String revise(Dataset revision, Date revisionDate) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		Transaction tx=null;
		try {
		 	tx=pm.currentTransaction();
			tx.begin();
			GAEJDODataset newRevision = revisableDAO.revise(pm, revision, revisionDate);
			// now copy the 'deep' properties
			Key reviseeId = KeyFactory.stringToKey(revision.getId());
			GAEJDODataset revisee = (GAEJDODataset)pm.getObjectId(reviseeId);
			GAEJDOAnnotations a = GAEJDOAnnotations.clone(revisee.getAnnotations());
			newRevision.setAnnotations(a);
			newRevision.setLayers(new HashSet<Key>(revisee.getLayers()));
			pm.makePersistent(newRevision); // don't know if this is necessary
			tx.commit();
			return KeyFactory.keyToString(newRevision.getId());
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if(tx.isActive()) {
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
	 * @param id the id of any revision of the object
	 * @return the latest version of the object
	 * @throws DatastoreException if no result
	 */
	public Dataset getLatest(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			Dataset latest = revisableDAO.getLatest(pm, id);
			return latest;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}	
	}
	
	/**
	 * Get all versions of an object
	 * @param id
	 * @return all revisions of the given object
	 */
	public Collection<Dataset> getAllVersions(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			Collection<Dataset> allVersions = revisableDAO.getAllVersions(pm, id);
			return allVersions;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}	
	}
	
	/**
	 * Deletes all revisions of a Dataset
	 * @param id the id of any version of a revision series
	 * @throws DatastoreException
	 */
	public void deleteAllVersions(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				Key key = KeyFactory.stringToKey(id);
				Collection<GAEJDODataset> allVersions = revisableDAO.getAllVersions(pm, key);
				for (GAEJDODataset jdo : allVersions) {
					for (Key layerKey : jdo.getLayers()) {
						// may have to check whether it's a InputDataLayer or AnalysisResult
						GAEJDODatasetLayer layer = (GAEJDODatasetLayer)pm.getObjectById(GAEJDODatasetLayer.class, layerKey);
						pm.deletePersistent(layer);
					}
					pm.deletePersistent(jdo);
				}
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}
	

	
	@Override
	public void addAnnotation(String id, String attribute, String value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAnnotation(String id, String attribute, Integer value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAnnotation(String id, String attribute, Float value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAnnotation(String id, String attribute, Boolean value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAnnotation(String id, String attribute, Date value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAnnotation(String id, String attribute, String value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAnnotation(String id, String attribute, Integer value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAnnotation(String id, String attribute, Float value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAnnotation(String id, String attribute, Boolean value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAnnotation(String id, String attribute, Date value)
			throws DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Annotations getAnnotations(String id) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getStringAttributes() throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Integer> getIntegerAttributes() throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Boolean> getBooleanAttributes() throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Float> getFloatAttributes() throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Date> getDateAttributes() throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeSortedByString(int start, int end,
			String sortByAttr) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeSortedByInteger(int start, int end,
			String sortByAttr) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeSortedByBoolean(int start, int end,
			String sortByAttr) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeSortedByFloat(int start, int end,
			String sortByAttr) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeSortedByDate(int start, int end,
			String sortByAttr) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeHaving(int start, int end, String attribute,
			String value) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeHaving(int start, int end, String attribute,
			Integer value) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeHaving(int start, int end, String attribute,
			Boolean value) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeHaving(int start, int end, String attribute,
			Float value) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataset> getInRangeHaving(int start, int end, String attribute,
			Date value) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			List<Dataset> dtos = annotatableDAO.getHavingDateAnnotation(pm, attribute, value, start,end);
			return dtos;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}	
	}

	
	public void addLayer(String datasetId, String layerId) {
		throw new RuntimeException("Not yet implemented");
	}
	
	public void removeLayer(String datasetId, String layerId) {
		throw new RuntimeException("Not yet implemented");
	}
	
	public Collection<String> getLayers(String datasetId) {
		throw new RuntimeException("Not yet implemented");
	}

}
