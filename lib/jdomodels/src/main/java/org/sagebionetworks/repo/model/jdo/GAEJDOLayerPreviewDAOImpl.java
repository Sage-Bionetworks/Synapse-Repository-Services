package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Collection;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerPreview;
import org.sagebionetworks.repo.model.LayerPreviewDAO;




/**
 * @author deflaux
 * 
 */
public class GAEJDOLayerPreviewDAOImpl extends
		GAEJDOBaseDAOImpl<LayerPreview, GAEJDOInputDataLayer> implements
		LayerPreviewDAO {
	
	/**
	 * @param userId
	 */
	public GAEJDOLayerPreviewDAOImpl(String userId) {super(userId);}

	@Override
	protected LayerPreview newDTO() {
		return new LayerPreview();
	}

	@Override
	protected void copyToDto(GAEJDOInputDataLayer jdo, LayerPreview dto)
			throws DatastoreException {
		dto.setId(KeyFactory.keyToString(jdo.getId()));
		dto.setPreview(jdo.getPreview());
	}

	@Override
	protected void copyFromDto(LayerPreview dto, GAEJDOInputDataLayer jdo)
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
	protected Class<GAEJDOInputDataLayer> getJdoClass() {
		return GAEJDOInputDataLayer.class;
	}

	@Override
	public Collection<String> getPrimaryFields() {
		Collection<String> fields = new ArrayList<String>();
		fields.add("preview");
		return fields;
	}

	@Override
	protected GAEJDOInputDataLayer newJDO() {
		GAEJDOInputDataLayer jdo = new GAEJDOInputDataLayer();
		return jdo;
	}

}
