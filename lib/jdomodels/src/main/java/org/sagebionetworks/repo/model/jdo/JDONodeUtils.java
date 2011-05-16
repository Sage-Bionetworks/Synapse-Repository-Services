package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.query.ObjectType;

/**
 * Translates JDOs and DTOs.
 * 
 * @author jmhill
 *
 */
public class JDONodeUtils {
	
	/**
	 * Cratea  DTO from the JDO
	 * @param dto
	 * @return
	 */
	public static JDONode copyFromDto(Node dto){
		if(dto == null) throw new IllegalArgumentException("Dto cannot be null");
		JDONode jdo = new JDONode();
		updateFromDto(dto, jdo);
		return jdo;
	}
	
	/**
	 * Used to update an existing object
	 * @param dto
	 * @param jdo
	 * @return
	 */
	public static void updateFromDto(Node dto, JDONode jdo) {
		jdo.setName(dto.getName());
		jdo.setDescription(dto.getDescription());
		if(dto.getId() != null){
			jdo.setId(Long.parseLong(dto.getId()));
		}
		jdo.setCreatedOn(dto.getCreatedOn().getTime());
		jdo.setCreatedBy(dto.getCreatedBy());
		jdo.setModifiedBy(dto.getModifiedBy());
		jdo.setModifiedOn(dto.getModifiedOn().getTime());
		if(dto.getETag() != null){
			jdo.seteTag(Long.parseLong(dto.getETag()));
		}
	}
	
	/**
	 * Create a DTO from the JDO
	 * @param jdo
	 * @return
	 */
	public static Node copyFromJDO(JDONode jdo){
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
		dto.setModifiedBy(jdo.getModifiedBy());
		dto.setModifiedOn(new Date(jdo.getModifiedOn()));
		return dto;
	}
	

}
