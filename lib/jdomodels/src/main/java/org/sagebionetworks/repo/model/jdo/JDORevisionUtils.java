package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;

import org.sagebionetworks.repo.model.jdo.persistence.JDORevision;

public class JDORevisionUtils {
	
	/**
	 * Make a copy of the passed JDORevision
	 * @param toCopy
	 * @return
	 */
	public static JDORevision makeCopyForNewVersion(JDORevision toCopy){
		JDORevision copy = new JDORevision();
		copy.setOwner(toCopy.getOwner());
		// Increment the revision number
		copy.setRevisionNumber(new Long(toCopy.getRevisionNumber()+1));
		// Make a copy of the annotations array
		if(toCopy.getAnnotations() != null){
			// Make a copy of the annotations.
			copy.setAnnotations(Arrays.copyOf(toCopy.getAnnotations(), toCopy.getAnnotations().length));
		}
		return copy;
	}

}
