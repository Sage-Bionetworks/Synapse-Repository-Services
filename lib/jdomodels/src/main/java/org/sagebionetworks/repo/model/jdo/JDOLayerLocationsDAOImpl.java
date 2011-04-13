package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.LayerLocations;
import org.sagebionetworks.repo.model.LayerLocationsDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDOInputDataLayer;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLayerLocation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLayerLocations;



/**
 * @author deflaux
 * 
 */
public class JDOLayerLocationsDAOImpl extends
		JDOBaseDAOImpl<LayerLocations, JDOInputDataLayer> implements
		LayerLocationsDAO {
	
	public JDOLayerLocationsDAOImpl(String userId) {super(userId);}

	@Override
	protected LayerLocations newDTO() {
		return new LayerLocations();
	}

	@Override
	protected void copyToDto(JDOInputDataLayer jdo, LayerLocations dto)
			throws DatastoreException {
		dto.setId(KeyFactory.keyToString(jdo.getId()));
		Collection<LayerLocation> dtoLocations = new HashSet<LayerLocation>();
		if (null != jdo.getLocations() && null != jdo.getLocations().getLayerLocations()) {
			for (JDOLayerLocation location : jdo.getLocations().getLayerLocations()) {
				dtoLocations.add(location.toLayerLocation());
			}
		}
		dto.setLocations(dtoLocations);
	}

	@Override
	protected void copyFromDto(LayerLocations dto, JDOInputDataLayer jdo)
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
		if (null == dto.getLocations()) {
			throw new InvalidModelException(
					"'locations' is a required property for LayerLocations");
		}
		Set<JDOLayerLocation> jdoLocations = new HashSet<JDOLayerLocation>();
		if (null != dto.getLocations()) {
			for (LayerLocation location : dto.getLocations()) {
				jdoLocations.add(new JDOLayerLocation(location));
			}
		}
		JDOLayerLocations container = jdo.getLocations();
		container.setLayerLocations(jdoLocations);
	}

	@Override
	protected Class<JDOInputDataLayer> getJdoClass() {
		return JDOInputDataLayer.class;
	}

	@Override
	public Collection<String> getPrimaryFields() {
		Collection<String> fields = new ArrayList<String>();
		fields.add("locations");
		return fields;
	}

	@Override
	protected JDOInputDataLayer newJDO() {
		JDOInputDataLayer jdo = new JDOInputDataLayer();
		return jdo;
	}

}
