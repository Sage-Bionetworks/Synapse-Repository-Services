package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotations;
import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;
import org.sagebionetworks.repo.model.jdo.persistence.JDOInputDataLayer;
import org.sagebionetworks.repo.model.jdo.persistence.JDORevision;
import org.sagebionetworks.repo.web.NotFoundException;




/**
 * This is the DAO for the JDO implementation of Dataset. As such it
 * implements BaseDAO, RevisableDAO and AnnotatableDAO. It wraps BaseDAOHelper,
 * RevisableDAOHelper and JDORevisableAnnotationDAOImpl, which provide much
 * of the functionality.
 * 
 * @author bhoff
 * 
 */
public class JDODatasetDAOImpl extends
		JDORevisableAnnotatableDAOImpl<Dataset, JDODataset> implements
		DatasetDAO {
	
	public JDODatasetDAOImpl(String userId) {super(userId);}
	
	public Dataset newDTO() {
		Dataset dto = new Dataset();
		return dto;
	}

	public Class<JDODataset> getJdoClass() {
		return JDODataset.class;
	}

	public JDODataset newJDO() {
		JDODataset jdo = new JDODataset();
		jdo.setInputLayers(new HashSet<JDOInputDataLayer>());
		JDOAnnotations a = JDOAnnotations.newJDOAnnotations();
		jdo.setAnnotations(a);
		JDORevision<JDODataset> r = new JDORevision<JDODataset>();
		jdo.setRevision(r);
		return jdo;
	}

	public JDODataset cloneJdo(JDODataset jdo) throws DatastoreException {
		JDODataset clone = super.cloneJdo(jdo);
		Set<JDOInputDataLayer> inputLayers = new HashSet<JDOInputDataLayer>();
		JDOInputDataLayerDAOImpl layerDAO = new JDOInputDataLayerDAOImpl(userId, jdo.getId());
		for (JDOInputDataLayer layer : jdo.getInputLayers()) inputLayers.add(layerDAO.cloneJdo(layer));
		clone.setInputLayers(inputLayers);
		return clone;
	}

	public void copyToDto(JDODataset jdo, Dataset dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setName(jdo.getName());
		dto.setDescription(jdo.getDescription());
		dto.setCreator(jdo.getCreator());
		dto.setCreationDate(jdo.getCreationDate());
		dto.setStatus(jdo.getStatus());
		dto.setReleaseDate(jdo.getReleaseDate());
		dto.setVersion(jdo.getRevision().getVersion().toString());

		// Fill in our layer preview info
		InputDataLayerDAO layerDao = getInputDataLayerDAO(dto.getId());
		// Get all layers, we are making the assumption that this is not
		// more
		// than 10s of layers
		Collection<InputDataLayer> layers = layerDao.getInRange(0,
				Integer.MAX_VALUE);
		for (InputDataLayer layer : layers) {
			if(InputDataLayer.LayerTypeNames.E == InputDataLayer.LayerTypeNames.valueOf(layer.getType())) {
				dto.setHasExpressionData(true);
			}
			else if(InputDataLayer.LayerTypeNames.G == InputDataLayer.LayerTypeNames.valueOf(layer.getType())) {
				dto.setHasGeneticData(true);
			}
			else if(InputDataLayer.LayerTypeNames.C == InputDataLayer.LayerTypeNames.valueOf(layer.getType())) {
				dto.setHasClinicalData(true);
			}
		}
	}

	/**
	 * 
	 * Note: This method does NOT copy layers or revision info to the JDO
	 * object, those being done by the 'revise' method
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	public void copyFromDto(Dataset dto, JDODataset jdo)
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
		jdo.setName(dto.getName());
		jdo.setDescription(dto.getDescription());
		jdo.setCreator(dto.getCreator());
		jdo.setCreationDate(dto.getCreationDate());
		jdo.setStatus(dto.getStatus());
		jdo.setReleaseDate(dto.getReleaseDate());
	}

//	/**
//	 * take care of any work that has to be done before deleting the persisted
//	 * object
//	 * 
//	 * @param pm
//	 * @param jdo
//	 *            the object to be deleted
//	 */
//	public void preDelete(PersistenceManager pm, JDODataset jdo) {
//		super.preDelete(pm, jdo);
//	}

	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "name", "description", "creator",
				"status", "releaseDate", "version", "creationDate" });
	}

	public InputDataLayerDAO getInputDataLayerDAO(String datasetId) throws DatastoreException {
		return new JDOInputDataLayerDAOImpl(userId, KeyFactory
				.stringToKey(datasetId));
	}
	
	/**
	 * Override the parent method to remove authorization references to child layers prior to deletion
	 */
	public void delete(String id) throws DatastoreException, NotFoundException,
	UnauthorizedException {
		PersistenceManager pm = PMF.get();
	try {
		Long key = KeyFactory.stringToKey(id);
		JDODataset ds = pm.getObjectById(JDODataset.class, key);
		//  authorization check comes AFTER the retrieval step, so that we get a 'not found' result
		// rather than 'forbidden' when an object does not exist.
		if (!JDOUserGroupDAOImpl.canAccess(userId, getJdoClass().getName(), key, AuthorizationConstants.ACCESS_TYPE.CHANGE, pm))
			throw new UnauthorizedException();
		JDOInputDataLayerDAOImpl idlDAO = (JDOInputDataLayerDAOImpl)getInputDataLayerDAO(id);
		Set<JDOInputDataLayer> idls = ds.getInputLayers();
		for (JDOInputDataLayer idl: idls) {
			idlDAO.removeResourceFromAllGroups(idl.getId());
		}
	} catch (JDOObjectNotFoundException e) {
		throw new NotFoundException(e);
	} catch (UnauthorizedException e) {
		throw e;
	} catch (Exception e) {
		throw new DatastoreException(e);
	} finally {
		pm.close();
	}
		super.delete(id);
	}

}
