package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;

public class JDORevisionUtils {
	
	/**
	 * Make a copy of the passed JDORevision
	 * @param toCopy
	 * @return
	 */
	public static DBORevision makeCopyForNewVersion(DBORevision toCopy){
		DBORevision copy = new DBORevision();
		copy.setOwner(toCopy.getOwner());
		// Increment the revision number
		copy.setRevisionNumber(new Long(toCopy.getRevisionNumber()+1));
		// Make a copy of the annotations byte array
		if(toCopy.getAnnotations() != null){
			// Make a copy of the annotations.
			copy.setAnnotations(Arrays.copyOf(toCopy.getAnnotations(), toCopy.getAnnotations().length));
		}
		// Make a copy of the references byte array
		if(toCopy.getReferences() != null){
			// Make a copy of the references.
			copy.setReferences(Arrays.copyOf(toCopy.getReferences(), toCopy.getReferences().length));
		}
		// do not copy over Activity id!
		// copy the file handle
		if(toCopy.getFileHandleId() != null){
			copy.setFileHandleId(toCopy.getFileHandleId());
		}
		return copy;
	}
	

}
