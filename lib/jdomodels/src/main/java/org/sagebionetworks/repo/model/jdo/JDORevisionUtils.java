package org.sagebionetworks.repo.model.jdo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
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
		return copy;
	}
	
	/**
	 * Create a DTO from the JDO.
	 * @param jdo
	 * @return
	 * @throws DatastoreException 
	 * @throws IOException 
	 */
	public static NodeRevision createDtoFromJdo(JDORevision jdo) throws DatastoreException{
		NodeRevision rev = new NodeRevision();
		if(jdo.getOwner() != null){
			rev.setNodeId(KeyFactory.keyToString(jdo.getOwner().getId()));
		}
		rev.setRevisionNumber(jdo.getRevisionNumber());
		rev.setComment(jdo.getComment());
		rev.setLabel(jdo.getLabel());
		rev.setModifiedBy(jdo.getModifiedBy());
		rev.setModifiedOn(new Date(jdo.getModifiedOn()));
		try {
			rev.setNamedAnnotations(JDOAnnotationsUtils.decompressedAnnotations(jdo.getAnnotations()));
			rev.setReferences(JDOAnnotationsUtils.decompressedReferences(jdo.getReferences()));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		return rev;
	}
	/**
	 * Update the JDO object using the DTO.
	 * @param dto
	 * @param jdo
	 * @throws IOException 
	 */
	public static void updateJdoFromDto(NodeRevision dto, JDORevision jdo, JDONode owner) throws DatastoreException{
		jdo.setOwner(owner);
		jdo.setComment(dto.getComment());
		jdo.setLabel(dto.getLabel());
		jdo.setModifiedBy(dto.getModifiedBy());
		if(dto.getModifiedOn() != null){
			jdo.setModifiedOn(dto.getModifiedOn().getTime());
		}
		jdo.setRevisionNumber(dto.getRevisionNumber());
		try {
			jdo.setAnnotations(JDOAnnotationsUtils.compressAnnotations(dto.getNamedAnnotations()));
			jdo.setReferences(JDOAnnotationsUtils.compressReferences(dto.getReferences()));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

}
