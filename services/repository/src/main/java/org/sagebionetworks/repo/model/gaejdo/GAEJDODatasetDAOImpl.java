package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatasetLayer;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerPreview;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * This is the DAO for the GAEJDO implementation of Dataset.  As such it implements 
 * BaseDAO, RevisableDAO and AnnotatableDAO.  It wraps BaseDAOHelper, RevisableDAOHelper
 * and GAEJDORevisableAnnotationDAOImpl, which provide much of the functionality.
 * 
 * @author bhoff
 *
 */
public class GAEJDODatasetDAOImpl extends GAEJDORevisableAnnotatableDAOImpl<Dataset, GAEJDODataset> implements DatasetDAO {
	
//	private GAEJDOInputDataLayerDAOImpl inputDataLayerDAO = null;

	public GAEJDODatasetDAOImpl() {
		final GAEJDODatasetDAOImpl parent = this;
//		inputDataLayerDAO = new GAEJDOInputDataLayerDAOImpl();
	}

	public Dataset newDTO() {
		Dataset dto = new Dataset();
		return dto;
	}
	
	public Class<GAEJDODataset> getJdoClass() {return GAEJDODataset.class;}

	public GAEJDODataset newJDO() {
		GAEJDODataset jdo = new GAEJDODataset();
		GAEJDOAnnotations a = GAEJDOAnnotations.newGAEJDOAnnotations();
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
		dto.setCreationDate(gae.getCreationDate());
		dto.setStatus(gae.getStatus());
		dto.setReleaseDate(gae.getReleaseDate());
		dto.setVersion(gae.getRevision().getVersion().toString());
		Collection<LayerPreview> layers = new ArrayList<LayerPreview>();
		Collection<Key> layerKeys = gae.getLayers();
		if (null != layerKeys) {
			for (Key l : layerKeys) {
				layers.add(new LayerPreview(KeyFactory.keyToString(l)));
			}
		}
		dto.setLayers(layers);
	}

	/**
	 * 
	 * Note: This method does NOT copy layers or revision info to the GAEJDO
	 * object,
	 * those being done by the 'revise' method

	 * @param dto
	 * @param gae
	 * @throws InvalidModelException
	 */
	public void copyFromDto(Dataset dto, GAEJDODataset gae)
			throws InvalidModelException {

		//
		// Confirm that the DTO is valid by checking that all required fields
		// are set
		//
		// Question: is this where we want this sort of logic?
		// Dev Note: right now the only required field is name but I can imagine
		// that the
		// validation logic will become more complex over time
		if (null == dto.getName()) {
			throw new InvalidModelException(
					"'name' is a required property for Dataset");
		}
		gae.setName(dto.getName());
		gae.setDescription(dto.getDescription());
		gae.setCreator(dto.getCreator());
		gae.setCreationDate(dto.getCreationDate());
		gae.setStatus(dto.getStatus());
		gae.setReleaseDate(dto.getReleaseDate());
	}

	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "name", "description", "creator",
				"status", "releaseDate", "version" });
	}
//<<<<<<< .mine
//=======
//
//	public List<Dataset> getInRange(int start, int end) throws DatastoreException {
//		return revisableDAO.getInRange(start, end);
//	}
//
//	public List<Dataset> getInRangeSortedByPrimaryField(int start, int end,
//			String sortBy, boolean asc) throws DatastoreException {
//		return revisableDAO.getInRangeSortedByPrimaryField(start, end, sortBy,
//				asc);
//	}
//
//	public List<Dataset> getInRangeHavingPrimaryField(int start, int end,
//			String attribute, Object value) throws DatastoreException {
//		return revisableDAO.getInRangeHavingPrimaryField(start, end, attribute,
//				value);
//	}
//
//	/**
//	 * @param dataset
//	 *            an original (not revised) dataset
//	 * @return the id of the newly created dataset
//	 * @throws DatastoreException
//	 * @throws InvalidModelException
//	 */
//	public String create(Dataset dataset) throws DatastoreException,
//			InvalidModelException {
//		PersistenceManager pm = PMF.get();
//		Transaction tx = null;
//		try {
//			tx = pm.currentTransaction();
//			tx.begin();
//
//			//
//			// Set system-controlled immutable fields
//			//
//			// Question: is this where we want to be setting immutable
//			// system-controlled fields for our
//			// objects? This should only be set at creation time so its not
//			// appropriate to put it in copyFromDTO.
//			dataset.setCreationDate(new Date()); // now
//
//			//
//			// Set default values for optional fields that have defaults
//			//
//			// Question: is this where we want to specify reasonable default
//			// values?
//			if (null == dataset.getVersion()) {
//				dataset.setVersion(DEFAULT_VERSION);
//			}
//
//			GAEJDODataset jdo = revisableDAO.create(pm, dataset);
//			tx.commit();
//			copyToDto(jdo, dataset);
//			return KeyFactory.keyToString(jdo.getId());
//		} catch (InvalidModelException e) {
//			throw e;
//		} catch (Exception e) {
//			throw new DatastoreException(e);
//		} finally {
//			if (tx.isActive()) {
//				tx.rollback();
//			}
//			pm.close();
//		}
//	}
//
//	public Dataset get(String id) throws DatastoreException, NotFoundException {
//		return baseDAO.get(id);
//	}
//
//	public void delete(String id) throws DatastoreException, NotFoundException {
//		baseDAO.delete(id);
//
//	}
//
//	/**
//	 * This updates the 'shallow' properties. Neither Version nor deep
//	 * properties change.
//	 * 
//	 * @param dto
//	 *            non-null id is required
//	 * @throws DatastoreException
//	 * @throws InvalidModelException 
//	 * @throws NotFoundException 
//	 */
//	public void update(Dataset dto) throws DatastoreException, InvalidModelException, NotFoundException {
//		PersistenceManager pm = PMF.get();
//		Transaction tx = null;
//		try {
//			tx = pm.currentTransaction();
//			tx.begin();
//			revisableDAO.update(pm, dto);
//			tx.commit();
//		} catch (InvalidModelException e) {
//			throw e;
//		} catch (JDOObjectNotFoundException e) {
//			throw new NotFoundException(e);
//		} catch (Exception e) {
//			throw new DatastoreException(e);
//		} finally {
//			if (tx.isActive()) {
//				tx.rollback();
//			}
//			pm.close();
//		}
//	}
//
//	/**
//	 * Create a revision of the object specified by the 'id' and 'version'
//	 * fields, having the shallow properties from the given 'revision', and the
//	 * deep properties of the given 'version'. The new revision will have the
//	 * version given by the 'newVersion' parameter.
//	 * 
//	 * @param revision
//	 * @param newVersion
//	 * @param revisionDate
//	 */
//	public String revise(Dataset revision, Date revisionDate)
//			throws DatastoreException {
//		PersistenceManager pm = PMF.get();
//		Transaction tx = null;
//		try {
//			tx = pm.currentTransaction();
//			tx.begin();
//			GAEJDODataset newRevision = revisableDAO.revise(pm, revision,
//					revisionDate);
//			// now copy the 'deep' properties
//			Key reviseeId = KeyFactory.stringToKey(revision.getId());
//			GAEJDODataset revisee = (GAEJDODataset) pm.getObjectId(reviseeId);
//			GAEJDOAnnotations a = GAEJDOAnnotations.clone(revisee
//					.getAnnotations());
//			newRevision.setAnnotations(a);
//			newRevision.setLayers(new HashSet<Key>(revisee.getLayers()));
//			pm.makePersistent(newRevision); // don't know if this is necessary
//			tx.commit();
//			return KeyFactory.keyToString(newRevision.getId());
//		} catch (Exception e) {
//			throw new DatastoreException(e);
//		} finally {
//			if (tx.isActive()) {
//				tx.rollback();
//			}
//			pm.close();
//		}
//	}
//
//	public int getCount() throws DatastoreException {
//		PersistenceManager pm = PMF.get();
//		try {
//			int count = revisableDAO.getCount(pm);
//			return count;
//		} catch (Exception e) {
//			throw new DatastoreException(e);
//		} finally {
//			pm.close();
//		}
//	}
//
//	/**
//	 * 
//	 * @param id
//	 *            the id of any revision of the object
//	 * @return the latest version of the object
//	 * @throws DatastoreException
//	 *             if no result
//	 */
//	public Dataset getLatest(String id) throws DatastoreException {
//		PersistenceManager pm = PMF.get();
//		try {
//			Dataset latest = revisableDAO.getLatest(pm, id);
//			return latest;
//		} catch (Exception e) {
//			throw new DatastoreException(e);
//		} finally {
//			pm.close();
//		}
//	}
//

//>>>>>>> .r178
	
	public InputDataLayerDAO getInputDataLayerDAO(String datasetId) {
		return new GAEJDOInputDataLayerDAOImpl(KeyFactory.stringToKey(datasetId));
	}

	
	// TODO : when deleting a dataset, delete its layers too



}
