package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDORevision;

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
	 * @return
	 */
	public static void updateFromDto(Node dto, JDONode jdo, JDORevision rev) {
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
	}
	
	/**
	 * Create a DTO from the JDO
	 * @param jdo
	 * @return
	 */
	public static Node copyFromJDO(JDONode jdo, JDORevision rev){
		Node dto = new Node();
		dto.setName(jdo.getName());
		dto.setDescription(jdo.getDescription());
		if(jdo.getId() != null){
			dto.setId(jdo.getId().toString());
		}
		if(jdo.getParent() != null){
			dto.setParentId(jdo.getParent().getId().toString());
		}
		if(jdo.geteTag() != null){
			dto.setETag(jdo.geteTag().toString());
		}
		if(jdo.getNodeType() != null){
			dto.setNodeType(jdo.getNodeType().getName());
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
		return dto;
	}
	
}
