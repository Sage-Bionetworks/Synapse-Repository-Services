package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.NodeDAO;

public interface AllTypesValidator extends EntityValidator<Entity> {

	/**
	 * Set the NodeDAO for the validator (for testing purposes)
	 * 
	 * @param nodeDAO
	 */
	public void setNodeDAO(NodeDAO nodeDAO);

}
