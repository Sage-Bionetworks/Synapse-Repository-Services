package org.sagebionetworks.repo.model.jdo;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;

/**
 * Translates JDOs and DTOs.
 * 
 * @author jmhill
 *
 */
public class JDONodeUtils {
		
	/**
	 * Used to update an existing object
	 * @param dto
	 * @param jdo
	 * @param rev 
	 * @return
	 * @throws DatastoreException 
	 */
	public static void updateFromDto(Node dto, DBONode jdo, DBORevision rev) throws DatastoreException {
		jdo.setName(dto.getName());
		jdo.setDescription(dto.getDescription());
		if(dto.getId() != null){
			jdo.setId(Long.parseLong(dto.getId()));
		}
		if(dto.getCreatedOn() != null){
			jdo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		jdo.setCreatedBy(dto.getCreatedBy());
		rev.setModifiedBy(dto.getModifiedBy());
		if(dto.getModifiedOn() != null){
			rev.setModifiedOn(dto.getModifiedOn().getTime());
		}
		rev.setComment(dto.getVersionComment());
		if(dto.getVersionLabel() != null){
			rev.setLabel(dto.getVersionLabel());
		}
		try {
			rev.setReferences(JDOSecondaryPropertyUtils.compressReferences(dto.getReferences()));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * Replace all fields from the dto.
	 * @param dto
	 * @param jdo
	 * @throws DatastoreException
	 */
	public static void replaceFromDto(Node dto, DBONode jdo) throws DatastoreException {
		jdo.setName(dto.getName());
		jdo.setDescription(dto.getDescription());
		if(dto.getId() != null){
			jdo.setId(KeyFactory.stringToKey(dto.getId()));
		}
		if(dto.getCreatedOn() != null){
			jdo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		jdo.setCreatedBy(dto.getCreatedBy());
		jdo.seteTag(KeyFactory.stringToKey(dto.getETag()));
		jdo.setCurrentRevNumber(dto.getVersionNumber());
		jdo.setNodeType(EntityType.valueOf(dto.getNodeType()).getId());
		if(dto.getParentId() != null){
			jdo.setParentId(KeyFactory.stringToKey(dto.getParentId()));
		}else{
			jdo.setParentId(null);
		}

	}
	
	/**
	 * Create a DTO from the JDO
	 * @param jdo
	 * @return
	 * @throws DatastoreException 
	 */
	public static Node copyFromJDO(DBONode jdo, DBORevision rev) throws DatastoreException{
		Node dto = new Node();
		dto.setName(jdo.getName());
		dto.setDescription(jdo.getDescription());
		if(jdo.getId() != null){
			dto.setId(jdo.getId().toString());
		}
		if(jdo.getParentId() != null){
			dto.setParentId(KeyFactory.keyToString(jdo.getParentId()));
		}
		if(jdo.geteTag() != null){
			dto.setETag(jdo.geteTag().toString());
		}
		if(jdo.getNodeType() != null){
			dto.setNodeType(EntityType.getTypeForId(jdo.getNodeType()).name());
		}
		dto.setCreatedOn(new Date(jdo.getCreatedOn()));
		dto.setCreatedBy(jdo.getCreatedBy());
		dto.setModifiedBy(rev.getModifiedBy());
		dto.setModifiedOn(new Date(rev.getModifiedOn()));
		dto.setVersionComment(rev.getComment());
		dto.setVersionLabel(rev.getLabel());
		if(rev.getRevisionNumber() != null){
			dto.setVersionNumber(rev.getRevisionNumber());
		}
		try {
			dto.setReferences(JDOSecondaryPropertyUtils.decompressedReferences(rev.getReferences()));
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return dto;
	}
	
}
