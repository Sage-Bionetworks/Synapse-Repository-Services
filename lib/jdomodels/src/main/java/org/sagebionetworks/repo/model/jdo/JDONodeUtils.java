package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;

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
		return dto;
	}
	

}
