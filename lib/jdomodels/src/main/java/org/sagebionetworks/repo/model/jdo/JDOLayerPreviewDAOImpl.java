package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Collection;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerPreview;
import org.sagebionetworks.repo.model.LayerPreviewDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDOInputDataLayer;




/**
 * @author deflaux
 * 
 */
public class JDOLayerPreviewDAOImpl extends
		JDOBaseDAOImpl<LayerPreview, JDOInputDataLayer> implements
		LayerPreviewDAO {
	
	/**
	 * @param userId
	 */
	public JDOLayerPreviewDAOImpl(String userId) {super(userId);}

	@Override
	protected LayerPreview newDTO() {
		return new LayerPreview();
	}

	@Override
	protected void copyToDto(JDOInputDataLayer jdo, LayerPreview dto)
			throws DatastoreException {
		dto.setId(KeyFactory.keyToString(jdo.getId()));
		dto.setPreview(jdo.getPreview());
	}

	@Override
	protected void copyFromDto(LayerPreview dto, JDOInputDataLayer jdo)
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
		if (null == dto.getPreview()) {
			throw new InvalidModelException(
					"'preview' is a required property for LayerPreview");
		}
		jdo.setPreview(dto.getPreview());
	}

	@Override
	protected Class<JDOInputDataLayer> getJdoClass() {
		return JDOInputDataLayer.class;
	}

	@Override
	public Collection<String> getPrimaryFields() {
		Collection<String> fields = new ArrayList<String>();
		fields.add("preview");
		return fields;
	}

	@Override
	protected JDOInputDataLayer newJDO() {
		JDOInputDataLayer jdo = new JDOInputDataLayer();
		return jdo;
	}

}
