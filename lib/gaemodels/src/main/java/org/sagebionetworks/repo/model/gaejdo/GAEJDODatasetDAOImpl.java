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
 * This is the DAO for the GAEJDO implementation of Dataset. As such it
 * implements BaseDAO, RevisableDAO and AnnotatableDAO. It wraps BaseDAOHelper,
 * RevisableDAOHelper and GAEJDORevisableAnnotationDAOImpl, which provide much
 * of the functionality.
 * 
 * @author bhoff
 * 
 */
public class GAEJDODatasetDAOImpl extends
		GAEJDORevisableAnnotatableDAOImpl<Dataset, GAEJDODataset> implements
		DatasetDAO {

	public Dataset newDTO() {
		Dataset dto = new Dataset();
		return dto;
	}

	public Class<GAEJDODataset> getJdoClass() {
		return GAEJDODataset.class;
	}

	public GAEJDODataset newJDO() {
		GAEJDODataset jdo = new GAEJDODataset();
		GAEJDOAnnotations a = GAEJDOAnnotations.newGAEJDOAnnotations();
		jdo.setAnnotations(a);
		GAEJDORevision<GAEJDODataset> r = new GAEJDORevision<GAEJDODataset>();
		jdo.setRevision(r);
		return jdo;
	}

	public GAEJDODataset cloneJdo(GAEJDODataset jdo) {
		GAEJDODataset clone = super.cloneJdo(jdo);
		clone.setLayers(new HashSet<Key>(jdo.getLayers()));
		return clone;
	}

	public void copyToDto(GAEJDODataset jdo, Dataset dto) {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setName(jdo.getName());
		dto.setDescription(jdo.getDescription());
		dto.setCreator(jdo.getCreator());
		dto.setCreationDate(jdo.getCreationDate());
		dto.setStatus(jdo.getStatus());
		dto.setReleaseDate(jdo.getReleaseDate());
		dto.setVersion(jdo.getRevision().getVersion().toString());
	}

	/**
	 * 
	 * Note: This method does NOT copy layers or revision info to the GAEJDO
	 * object, those being done by the 'revise' method
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	public void copyFromDto(Dataset dto, GAEJDODataset jdo)
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

	/**
	 * take care of any work that has to be done before deleting the persisted
	 * object
	 * 
	 * @param pm
	 * @param jdo
	 *            the object to be deleted
	 */
	public void preDelete(PersistenceManager pm, GAEJDODataset jdo) {
		GAEJDOInputDataLayerDAOImpl layerDAO = new GAEJDOInputDataLayerDAOImpl(
				jdo.getId());
		for (Key layerKey : jdo.getLayers()) {
			GAEJDOInputDataLayer layer = (GAEJDOInputDataLayer) pm
					.getObjectById(GAEJDOInputDataLayer.class, layerKey);
			layerDAO.delete(pm, layer);
		}
		super.preDelete(pm, jdo);
	}

	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "name", "description", "creator",
				"status", "releaseDate", "version", "creationDate" });
	}

	public InputDataLayerDAO getInputDataLayerDAO(String datasetId) {
		return new GAEJDOInputDataLayerDAOImpl(KeyFactory
				.stringToKey(datasetId));
	}

}
