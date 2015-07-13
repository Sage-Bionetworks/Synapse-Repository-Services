package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBOReference;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Converts reference to their DBO object.
 * @author John
 * @author kimyen
 *
 */
public class ReferenceUtil {

	/**
	 * Convert a Reference to a DBOReference
	 * 
	 * @param ownerId
	 * @param reference
	 * @return
	 */
	public static DBOReference createDBOReference(Long ownerId, Reference reference) {
		DBOReference dbo = new DBOReference();
		dbo.setGroupName("linksTo");
		dbo.setOwner(ownerId);
		dbo.setTargetId(KeyFactory.stringToKey(reference.getTargetId()));
		dbo.setTargetRevision(reference.getTargetVersionNumber());
		return dbo;
	}

}
