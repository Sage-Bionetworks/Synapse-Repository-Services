package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;

public class UserGroupUtils {
	
	public static void copyDtoToDbo(UserGroup dto, DBOUserGroup dbo) {
		if (dto.getId()==null) {
			dbo.setId(null);
		} else {
			dbo.setId(Long.parseLong(dto.getId()));
		}
		if (dto.getEtag()==null) {
			dbo.seteTag(null);
		} else {
			dbo.seteTag(Long.parseLong(dto.getEtag()));
		}
		dbo.setCreationDate(dto.getCreationDate());
		dbo.setIsIndividual(dto.isIndividual());
		dbo.setName(dto.getName());

	}
	
	public static void copyDboToDto(DBOUserGroup dbo, UserGroup dto) {
		if (dbo.getId()==null) {
			dto.setId(null); 
		} else {
			dto.setId(dbo.getId().toString());
		}
		if (dbo.geteTag()==null) {
			dto.setEtag(null);
		} else {
			dto.setEtag(""+dbo.geteTag());
		}
		dto.setCreationDate(dbo.getCreationDate());
		dto.setIndividual(dbo.getIsIndividual());
		dto.setName(dbo.getName());
	}
	

}
