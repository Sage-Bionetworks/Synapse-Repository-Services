package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Translates JDOs and DTOs.
 * 
 * @author jmhill
 *
 */
class NodeUtils {
	

		
	/**
	 * Used to update an existing object
	 * @param dto
	 * @param jdo
	 * @param rev 
	 * @return
	 * @throws DatastoreException 
	 */
	public static void updateFromDto(Node dto, DBONode jdo, DBORevision rev, boolean deleteActivityId) throws DatastoreException, InvalidModelException {
		jdo.setName(dto.getName());
		if(dto.getDescription() !=  null){
			try {
				jdo.setDescription(dto.getDescription().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}else{
			jdo.setDescription(null);
		}

		if(dto.getId() != null){
			jdo.setId(KeyFactory.stringToKey(dto.getId()));
		}
		if(dto.getCreatedOn() != null){
			jdo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		if (dto.getCreatedByPrincipalId() != null){
			jdo.setCreatedBy(dto.getCreatedByPrincipalId());
		}
		if (dto.getModifiedByPrincipalId()==null) throw new InvalidModelException("modifiedByPrincipalId may not be null");
		rev.setModifiedBy(dto.getModifiedByPrincipalId());
		if (dto.getModifiedOn()==null) throw new InvalidModelException("modifiedOn may not be null");
		rev.setModifiedOn(dto.getModifiedOn().getTime());
		rev.setComment(dto.getVersionComment());
		if(dto.getVersionLabel() != null){
			rev.setLabel(dto.getVersionLabel());
		} 	
		if(dto.getFileHandleId() != null){
			rev.setFileHandleId(KeyFactory.stringToKey(dto.getFileHandleId()));
		}else{
			rev.setFileHandleId(null);
		}
		// bring in activity id, if set
		if(deleteActivityId) {
			rev.setActivityId(null);
		} else if(dto.getActivityId() != null) {
			rev.setActivityId(Long.parseLong(dto.getActivityId()));
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
		if(dto.getDescription() != null){
			try {
				jdo.setDescription(dto.getDescription().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}else{
			jdo.setDescription(null);
		}
		if(dto.getId() != null){
			jdo.setId(KeyFactory.stringToKey(dto.getId()));
		}
		if(dto.getCreatedOn() != null){
			jdo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		jdo.setCreatedBy(dto.getCreatedByPrincipalId());
		jdo.seteTag(KeyFactory.urlDecode(dto.getETag()));
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
		if(jdo.getDescription() != null){
			try {
				dto.setDescription(new String(jdo.getDescription(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}else{
			dto.setDescription(null);
		}
		if(jdo.getId() != null){
			dto.setId(KeyFactory.keyToString(jdo.getId()));
		}
		if(jdo.getParentId() != null){
			dto.setParentId(KeyFactory.keyToString(jdo.getParentId()));
		}
		if(jdo.geteTag() != null){
			dto.setETag(jdo.geteTag());
		}
		if(jdo.getNodeType() != null){
			dto.setNodeType(EntityType.getTypeForId(jdo.getNodeType()).name());
		}
		dto.setCreatedOn(new Date(jdo.getCreatedOn()));
		dto.setCreatedByPrincipalId(jdo.getCreatedBy());
		dto.setModifiedByPrincipalId(rev.getModifiedBy());
		dto.setModifiedOn(new Date(rev.getModifiedOn()));
		dto.setVersionComment(rev.getComment());
		dto.setVersionLabel(rev.getLabel());
		if(rev.getRevisionNumber() != null){
			dto.setVersionNumber(rev.getRevisionNumber());
		}
		if(rev.getFileHandleId() != null){
			dto.setFileHandleId(rev.getFileHandleId().toString());
		}
		if(rev.getActivityId() != null) {
			dto.setActivityId(rev.getActivityId().toString());
		} 
		
		try {
			dto.setReferences(JDOSecondaryPropertyUtils.decompressedReferences(rev.getReferences()));
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return dto;
	}
	
}
