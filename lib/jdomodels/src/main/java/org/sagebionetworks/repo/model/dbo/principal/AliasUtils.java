package org.sagebionetworks.repo.model.dbo.principal;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;

/**
 * Utilities for alias.
 * @author John
 *
 */
public class AliasUtils {
	
	/**
	 * Create a DBO from the DTO
	 * @param dto
	 * @return
	 */
	public static DBOPrincipalAlias createDBOFromDTO(PrincipalAlias dto){
		DBOPrincipalAlias dbo = new DBOPrincipalAlias();
		// First validate the name type.
		if(dto.getAlias() == null) throw new IllegalArgumentException("Alias cannot be null");
		if(dto.getPrincipalId() == null) throw new IllegalArgumentException("Principal ID cannot be null");
		if(dto.getType() == null) throw new IllegalArgumentException("AliasType cannot be null");
		if(dto.getIsValidated() == null) throw new IllegalArgumentException("IsValidated cannot be null");
		// convert the alias to a unique string
		dbo.setAliasUnique(getUniqueAliasName(dto.getAlias()));
		dbo.setAliasDisplay(dto.getAlias());
		dbo.setAliasType(AliasEnum.valueOf(dto.getType().name()));
		dbo.setEtag(dto.getEtag());
		dbo.setId(dto.getAliasId());
		dbo.setValidated(dto.getIsValidated());
		dbo.setPrincipalId(dto.getPrincipalId());
		return dbo;
	}
	
	/**
	 * Convert a list of aliases DBOs to a list of DTOs
	 * @param dbos
	 * @return
	 */
	public static List<PrincipalAlias> createDTOFromDBO(List<DBOPrincipalAlias> dbos){
		List<PrincipalAlias> list = new LinkedList<PrincipalAlias>();
		if(dbos != null){
			for(DBOPrincipalAlias dbo: dbos){
				list.add(createDTOFromDBO(dbo));
			}
		}
		return list;
	}
	
	/**
	 * Convert a DBO to a DTO.
	 * @param dbo
	 * @return
	 */
	public static PrincipalAlias createDTOFromDBO(DBOPrincipalAlias dbo) {
		PrincipalAlias dto = new PrincipalAlias();
		dto.setAlias(dbo.getAliasDisplay());
		dto.setAliasId(dbo.getId());
		dto.setEtag(dbo.getEtag());
		dto.setIsValidated(dbo.getValidated());
		dto.setPrincipalId(dbo.getPrincipalId());
		dto.setType(AliasType.valueOf(dbo.getAliasType().name()));
		return dto;
	}
	
	
	// Used to replace all characters expect letters and numbers.
	private static Pattern PRINICPAL_UNIQUENESS_REPLACE_PATTERN = Pattern
			.compile("[^a-z0-9]");
	
	private static Pattern TEAM_NAME_REPLACE_PATTERN = Pattern
			.compile("[^a-z0-9A-Z ._-]");


	/**
	 * Get the string that will be used for a uniqueness check for alias
	 * names. Only lower case letters and numbers contribute to the uniqueness
	 * of a principal name. All other characters (-,., ,_) are ignored.
	 * 
	 * @param inputName
	 * @return
	 */
	public static String getUniqueAliasName(String inputName) {
		if (inputName == null)
			throw new IllegalArgumentException("Name cannot be null");
		// Case does not contribute to uniqueness
		String lower = inputName.toLowerCase();
		// Only letters and numbers contribute to the uniqueness
		Matcher m = PRINICPAL_UNIQUENESS_REPLACE_PATTERN.matcher(lower);
		// Replace all non-letters and numbers with empty strings
		return m.replaceAll("");
	}


}
