package org.sagebionetworks.repo.model.jdo;


/**
 * Converts to/from JDOs and DTOs
 * @author jmhill
 *
 */
public class JDODatasetUtils {
	
//	/**
//	 * Create a new JDO object from the DTO
//	 * @param ds
//	 * @return
//	 * @throws InvalidModelException 
//	 */
//	public static JDODataset createFromDTO(Dataset dto) throws InvalidModelException{
//		JDODataset jdo = new JDODataset();
//
//		//
//		// Confirm that the DTO is valid by checking that all required fields
//		// are set
//		//
//		// Question: is this where we want this sort of logic?
//		// Dev Note: right now the only required field is name but I can imagine
//		// that the
//		// validation logic will become more complex over time
//		if (null == dto.getName()) {
//			throw new InvalidModelException(
//					"'name' is a required property for Dataset");
//		}
//		jdo.setName(dto.getName());
//		jdo.setDescription(dto.getDescription());
//		jdo.setCreator(dto.getCreator());
//		jdo.setCreationDate(dto.getCreationDate());
//		jdo.setStatus(dto.getStatus());
//		jdo.setReleaseDate(dto.getReleaseDate());
//		if(dto.getId() != null){
//			jdo.setId(Long.parseLong(dto.getId()));
//		}
//		return jdo;
//	}
//	
//	/**
//	 * Create a new JDO from the DTO
//	 * @param dto
//	 * @return
//	 * @throws InvalidModelException
//	 */
//	public static Dataset createFromJDO(JDODataset jdo) {
//		Dataset dto = new Dataset();
//		dto.setName(jdo.getName());
//		dto.setDescription(jdo.getDescription());
//		dto.setCreator(jdo.getCreator());
//		dto.setCreationDate(jdo.getCreationDate());
//		dto.setStatus(jdo.getStatus());
//		dto.setReleaseDate(jdo.getReleaseDate());
//		if(jdo.getId() != null){
//			dto.setId(jdo.getId().toString());
//		}
//		return dto;
//	}

}
