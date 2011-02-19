package org.sagebionetworks.repo.model.gaejdo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

public class GAEJDOInputDataLayerDAOImpl extends
		GAEJDORevisableAnnotatableDAOImpl<InputDataLayer, GAEJDOInputDataLayer>
		implements InputDataLayerDAO {

	private Key datasetId = null;

	public GAEJDOInputDataLayerDAOImpl(String userId, Key datasetId) {
		super(userId);
		this.datasetId = datasetId;
	}

	protected InputDataLayer newDTO() {
		return new InputDataLayer();
	}

	protected GAEJDOInputDataLayer newJDO() {
		GAEJDOInputDataLayer jdo = new GAEJDOInputDataLayer();

		GAEJDOAnnotations a = GAEJDOAnnotations.newGAEJDOAnnotations();
		jdo.setAnnotations(a);
		GAEJDORevision<GAEJDOInputDataLayer> r = new GAEJDORevision<GAEJDOInputDataLayer>();
		jdo.setRevision(r);
		GAEJDOLayerLocations l = GAEJDOLayerLocations.newGAEJDOLayerLocations();
		jdo.setLocations(l);
		
		return jdo;
	}

	protected void copyToDto(GAEJDOInputDataLayer jdo, InputDataLayer dto)
			throws DatastoreException {

		dto.setId(KeyFactory.keyToString(jdo.getId()));
		dto.setName(jdo.getName());
		dto.setDescription(jdo.getDescription().getValue());
		dto.setCreationDate(jdo.getCreationDate());
		dto.setVersion(jdo.getRevision().getVersion().toString());

		dto.setPublicationDate(jdo.getPublicationDate());
		dto.setReleaseNotes(jdo.getReleaseNotes().getValue());
		try {
			dto.setType(jdo.getType());
		} catch (InvalidModelException e) {
			throw new DatastoreException(
					"We changed our data model but neglected to clean up data previously stored",
					e);
		}
		dto.setTissueType(jdo.getTissueType());
		dto.setPlatform(jdo.getPlatform());
		dto.setProcessingFacility(jdo.getPlatform());
		dto.setQcBy(jdo.getQcBy());
		dto.setQcDate(jdo.getQcDate());
	}

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	protected void copyFromDto(InputDataLayer dto, GAEJDOInputDataLayer jdo)
			throws InvalidModelException {
		//
		// Confirm that the DTO is valid by checking that all required fields
		// are set
		//
		// Question: is this where we want this sort of logic?
		// Dev Note: right now the only required field is name and type but I
		// can imagine
		// that the
		// validation logic will become more complex over time
		if (null == dto.getName()) {
			throw new InvalidModelException(
					"'name' is a required property for InputDataLayer");
		}
		if (null == dto.getType()) {
			throw new InvalidModelException(
					"'type' is a required property for InputDataLayer");
		}
		jdo.setName(dto.getName());
		String description = (null == dto.getDescription()) ? "" : dto
				.getDescription();
		jdo.setDescription(new Text(description));
		jdo.setCreationDate(dto.getCreationDate());

		jdo.setPublicationDate(dto.getPublicationDate());
		String releaseNotes = (null == dto.getReleaseNotes()) ? "" : dto
				.getReleaseNotes();
		jdo.setReleaseNotes(new Text(releaseNotes));
		jdo.setType(dto.getType());
		jdo.setTissueType(dto.getTissueType());
		jdo.setPlatform(dto.getPlatform());
		jdo.setProcessingFacility(dto.getPlatform());
		jdo.setQcBy(dto.getQcBy());
		jdo.setQcDate(dto.getQcDate());
	}

	/**
	 * @param jdoClass
	 *            the class parameterized by T
	 */
	protected Class<GAEJDOInputDataLayer> getJdoClass() {
		return GAEJDOInputDataLayer.class;
	}

	public Collection<String> getPrimaryFields() {
		return GAEJDOInputDataLayer.getPrimaryFields();
	}

	/**
	 * take care of any work that has to be done before deleting the persistent
	 * object but within the same transaction (for example, deleting objects
	 * which this object composes, but which are not represented by owned
	 * relationships)
	 * 
	 * @param pm
	 * @param jdo
	 *            the object to be deleted
	 */
	// protected void preDelete(PersistenceManager pm, GAEJDOInputDataLayer jdo)
	// {
	// // remove layer from parent
	// GAEJDODataset parent = (GAEJDODataset) pm.getObjectById(
	// GAEJDODataset.class, datasetId);
	// parent.getLayers().remove(jdo.getId());
	// }

	// protected Key generateKey(PersistenceManager pm) throws
	// DatastoreException {
	// long n = 1000L + (long) getCount(pm); // could also use a 'sequence' to
	// // generate a unique integer
	// Key key = KeyFactory.createKey(datasetId, "GAEJDOInputDataLayer", n);
	// return key;
	// }

	// /**
	// * take care of any work that has to be done after creating the persistent
	// * object but within the same transaction
	// *
	// * @param pm
	// * @param jdo
	// */
	// protected void postCreate(PersistenceManager pm, GAEJDOInputDataLayer
	// jdo) {
	// // add layer to parent
	// GAEJDODataset parent = (GAEJDODataset) pm.getObjectById(
	// GAEJDODataset.class, datasetId);
	// parent.getInputLayers().add(jdo);
	// }
	//	

	/**
	 * Create a new object, using the information in the passed DTO
	 * 
	 * @param dto
	 * @return the ID of the created object
	 * @throws InvalidModelException
	 */
	public String create(InputDataLayer dto) throws InvalidModelException,
			DatastoreException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			GAEJDODataset ownerDataset = (GAEJDODataset) pm.getObjectById(
					GAEJDODataset.class, datasetId);

			GAEJDOInputDataLayer jdo = createIntern(dto);
			// copyFromDto(dto, jdo);
			ownerDataset.getInputLayers().add(jdo);
			pm.makePersistent(ownerDataset);
			GAEJDORevision<GAEJDOInputDataLayer> r = jdo.getRevision();
			r.setOriginal(r.getId()); // points to itself
			pm.makePersistent(ownerDataset); // not sure if it's necessary to
			// 'persist' again
			tx.commit();
			//tx = pm.currentTransaction();
			//tx.begin();
			addUserAccess(pm, jdo);
			//tx.commit();
			copyToDto(jdo, dto);
			return KeyFactory.keyToString(jdo.getId());
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
	 * Delete the specified object
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Key key = KeyFactory.stringToKey(id);
			GAEJDOInputDataLayer jdo = (GAEJDOInputDataLayer) pm.getObjectById(
					getJdoClass(), key);
			GAEJDODataset ownerDataset = (GAEJDODataset) pm.getObjectById(
					GAEJDODataset.class, datasetId);
			ownerDataset.getInputLayers().remove(jdo);
			pm.deletePersistent(jdo);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	// ---------------------------------------------------------------------------------------
	// Modified versions of the BaseDAOImpl methods, to constrain them to
	// dataset of interest
	// ---------------------------------------------------------------------------------------

	/**
	 * @return the latest revision of all layers for the Dataset of interest
	 */
	private Collection<GAEJDOInputDataLayer> getInputLayers(
			PersistenceManager pm) throws DatastoreException {
		GAEJDODataset jdo = (GAEJDODataset) pm.getObjectById(
				GAEJDODataset.class, datasetId);
		Collection<GAEJDOInputDataLayer> originalLayers = jdo.getInputLayers();
		Collection<GAEJDOInputDataLayer> ans = new HashSet<GAEJDOInputDataLayer>();
		for (GAEJDOInputDataLayer layer : originalLayers)
			ans.add(getLatest(pm, layer));
		// System.err.println("GAEJDInputDataLayer.getInputLayers: # layers="+originalLayers.size()+" # 'latest' layers="+ans.size());
		return ans;
	}

	protected int getCount(PersistenceManager pm) throws DatastoreException {
		return getInputLayers(pm).size();
	}

	/**
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and less than
	 *         index 'end'
	 */
	public List<InputDataLayer> getInRange(int start, int end)
			throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			List<InputDataLayer> ans = new ArrayList<InputDataLayer>();
			int cnt = 0;
			for (GAEJDOInputDataLayer jdo : getInputLayers(pm)) {
				if (cnt >= start && cnt < end) {
					InputDataLayer dto = newDTO();
					copyToDto(jdo, dto);
					ans.add(dto);
				}
				cnt++;
			}
			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	private static Object getFromField(Object owner, String fieldName) {
		try {
			String getterName = "get"
					+ Character.toUpperCase(fieldName.charAt(0))
					+ fieldName.substring(1);
			Method method = GAEJDOInputDataLayer.class.getMethod(getterName);
			return method.invoke(owner);
		} catch (NoSuchMethodException nme) {
			throw new IllegalArgumentException("No field " + fieldName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @param sortBy
	 * @param asc
	 *            if true then ascending, else descending
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end' and sorted by the given primary field
	 */
	public List<InputDataLayer> getInRangeSortedByPrimaryField(int start,
			int end, final String sortBy, final boolean asc)
			throws DatastoreException {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			// get all the layers
			List<GAEJDOInputDataLayer> layers = new ArrayList<GAEJDOInputDataLayer>(
					getInputLayers(pm));

			// I'm not a big fan of reflection. Initially this was done via a
			// database query,
			// but constraining to the dataset of interest requires doing the
			// sort in memory.
			// final Field field;
			// try {
			// field = GAEJDOInputDataLayer.class.getField(sortBy);
			// } catch (NoSuchFieldException nsfe) {
			// throw new IllegalArgumentException("No such field "+sortBy,
			// nsfe);
			// }
			// sort the layers
			Collections.sort(layers, new Comparator<GAEJDOInputDataLayer>() {
				public int compare(GAEJDOInputDataLayer o1,
						GAEJDOInputDataLayer o2) {
					try {
						Comparable v1 = (Comparable) getFromField(o1, sortBy);
						Comparable v2 = (Comparable) getFromField(o2, sortBy);
						if (v1 == null && v2 == null)
							return 0;
						if (v1 == null)
							return -1;
						if (v2 == null)
							return 1;
						return ((asc ? 1 : -1) * v1.compareTo(v2));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});

			// return just layers start..end
			List<InputDataLayer> ans = new ArrayList<InputDataLayer>();
			int cnt = 0;
			for (GAEJDOInputDataLayer jdo : layers) {
				if (cnt >= start && cnt < end) {
					InputDataLayer dto = newDTO();
					copyToDto(jdo, dto);
					ans.add(dto);
				}
				cnt++;
			}

			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}

	}

	/**
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @param attribute
	 * @param value
	 * @return a subset of results, starting at index 'start' and not going
	 *         beyond index 'end', having the given value for the given field
	 */
	public List<InputDataLayer> getInRangeHavingPrimaryField(int start,
			int end, String attribute, Object value) throws DatastoreException {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			// get all the layers
			List<GAEJDOInputDataLayer> layers = new ArrayList<GAEJDOInputDataLayer>(
					getInputLayers(pm));

			// I'm not a big fan of reflection. Initially this was done via a
			// database query,
			// but constraining to the dataset of interest requires doing the
			// sort in memory.
			// final Field field;
			// try {
			// field = GAEJDOInputDataLayer.class.getField(attribute);
			// } catch (NoSuchFieldException nsfe) {
			// throw new IllegalArgumentException("No such field "+attribute,
			// nsfe);
			// }

			List<InputDataLayer> ans = new ArrayList<InputDataLayer>();
			int cnt = 0;
			for (GAEJDOInputDataLayer jdo : layers) {
				boolean match = (value.equals(getFromField(jdo, attribute)));
				if (match) {
					if (cnt >= start && cnt < end) {
						InputDataLayer dto = newDTO();
						copyToDto(jdo, dto);
						ans.add(dto);
					}
					cnt++;
				}
			}

			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

}
