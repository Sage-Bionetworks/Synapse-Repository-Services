package org.sagebionetworks.repo.model.gaejdo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

public class GAEJDOInputDataLayerDAOImpl extends
		GAEJDORevisableAnnotatableDAOImpl<InputDataLayer, GAEJDOInputDataLayer>
		implements InputDataLayerDAO {

	private Key datasetId = null;

	public GAEJDOInputDataLayerDAOImpl(Key datasetId) {
		this.datasetId = datasetId;
	}

	public InputDataLayer newDTO() {
		return new InputDataLayer();
	}

	public GAEJDOInputDataLayer newJDO() {
		GAEJDOInputDataLayer jdo = new GAEJDOInputDataLayer();

		GAEJDOAnnotations a = GAEJDOAnnotations.newGAEJDOAnnotations();
		jdo.setAnnotations(a);
		GAEJDORevision<GAEJDOInputDataLayer> r = new GAEJDORevision<GAEJDOInputDataLayer>();
		jdo.setRevision(r);

		return jdo;
	}

	public void copyToDto(GAEJDOInputDataLayer jdo, InputDataLayer dto) {
		// TODO InputDataLayer only has a subset of the fields in GAEJDODatasetLayer
		// and GAEJDOInputDataLayer, the rest need to be added to the DTO
		
		dto.setId(KeyFactory.keyToString(jdo.getId()));
		dto.setName(jdo.getName());
		dto.setCreationDate(jdo.getCreationDate());
		dto.setUri(dto.getUri() == null ? null : dto.getUri().toString());
	}

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	public void copyFromDto(InputDataLayer dto, GAEJDOInputDataLayer jdo)
			throws InvalidModelException {
		jdo.setName(dto.getName());
		jdo.setCreationDate(dto.getCreationDate());
		try {
			jdo.setUri(dto.getUri() == null ? null : new URI(dto.getUri()));
		} catch (URISyntaxException urie) {
			throw new InvalidModelException(urie);
		}
	}

	/**
	 * @param jdoClass
	 *            the class parameterized by T
	 */
	public Class<GAEJDOInputDataLayer> getJdoClass() {
		return GAEJDOInputDataLayer.class;
	}

	public Collection<String> getPrimaryFields() {
		return GAEJDOInputDataLayer.getPrimaryFields();
	}

	/**
	 * take care of any work that has to be done before deleting the persisted
	 * object
	 * 
	 * @param pm
	 * @param jdo
	 *            the object to be deleted
	 */
	public void preDelete(PersistenceManager pm, GAEJDOInputDataLayer jdo) {
		// no-op!
	}
}
