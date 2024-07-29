package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;

public class JDORevisionUtils {
	
	/**
	 * Make a copy of the passed JDORevision
	 * @param toCopy
	 * @return
	 */
	public static DBORevision makeCopyForNewVersion(DBORevision toCopy, Long revisionNumber) {
		DBORevision copy = new DBORevision();
		
		copy.setDescription(toCopy.getDescription());
		copy.setOwner(toCopy.getOwner());
		copy.setRevisionNumber(revisionNumber);

		if(toCopy.getEntityPropertyAnnotations() != null){
			copy.setEntityPropertyAnnotations(Arrays.copyOf(toCopy.getEntityPropertyAnnotations(), toCopy.getEntityPropertyAnnotations().length));
		}

		copy.setUserAnnotationsJSON(toCopy.getUserAnnotationsJSON());
		copy.setReferenceJson(toCopy.getReferenceJson());
		// do not copy over Activity id!
		// copy the file handle
		if(toCopy.getFileHandleId() != null){
			copy.setFileHandleId(toCopy.getFileHandleId());
		}
		return copy;
	}
	

}
