package org.sagebionetworks.repo.model.gaejdo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.InvalidModelException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

public class GAEJDOInputDataLayerDAOImpl extends
		GAEJDORevisableAnnotatableDAOImpl<InputDataLayer, GAEJDOInputDataLayer>
		implements InputDataLayerDAO {

	private Key datasetId = null;

	public GAEJDOInputDataLayerDAOImpl(Key datasetId) {
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

		return jdo;
	}

	protected void copyToDto(GAEJDOInputDataLayer jdo, InputDataLayer dto) {		
		dto.setId(KeyFactory.keyToString(jdo.getId()));
		// TODO InputDataLayer only has a subset of the fields in GAEJDODatasetLayer
		// and GAEJDOInputDataLayer, the rest need to be added to the DTO
		
		dto.setId(KeyFactory.keyToString(jdo.getId()));
		dto.setName(jdo.getName());
		dto.setDescription(jdo.getDescription().getValue());
		dto.setCreationDate(jdo.getCreationDate());
		dto.setVersion(jdo.getRevision().getVersion().toString());
		
		dto.setPublicationDate(jdo.getPublicationDate());
		dto.setReleaseNotes(jdo.getReleaseNotes().getValue());
		dto.setType(jdo.getType());
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
		// Dev Note: right now the only required field is name and type but I can imagine
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
		jdo.setDescription(new Text(dto.getDescription()));
		jdo.setCreationDate(dto.getCreationDate());

		jdo.setPublicationDate(dto.getPublicationDate());
		jdo.setReleaseNotes(new Text(dto.getReleaseNotes()));
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
	 * object but within the same transaction (for example, deleteing objects
	 * which this object composes, but which are not represented by owned
	 * relationships)
	 * 
	 * @param pm
	 * @param jdo
	 *            the object to be deleted
	 */
	protected void preDelete(PersistenceManager pm, GAEJDOInputDataLayer jdo) {
		// remove layer from parent
		GAEJDODataset parent = (GAEJDODataset) pm.getObjectById(GAEJDODataset.class, datasetId);
		parent.getLayers().remove(jdo.getId());
	}
	
	protected Key generateKey(PersistenceManager pm) throws DatastoreException {
		long n= 1000L + (long)getCount(pm); // could also use a 'sequence' to generate a unique integer
		Key key = KeyFactory.createKey(datasetId, "GAEJDOInputDataLayer", n);
		return key;
	}

	/**
	 * take care of any work that has to be done after creating the persistent
	 * object but within the same transaction
	 * 
	 * @param pm
	 * @param jdo
	 */
	protected void postCreate(PersistenceManager pm, GAEJDOInputDataLayer jdo) {
		// add layer to parent
		GAEJDODataset parent = (GAEJDODataset) pm.getObjectById(GAEJDODataset.class, datasetId);
		parent.getLayers().add(jdo.getId());
	}

}
