package org.sagebionetworks.repo.model.dbo.dao;

import java.util.List;

import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;

public class UserGroupUtils {
	
	public static void copyDtoToDbo(UserGroup dto, DBOUserGroup dbo) {
		if (dto.getId()==null) {
			dbo.setId(null);
		} else {
			dbo.setId(Long.parseLong(dto.getId()));
		}
		dbo.setCreationDate(dto.getCreationDate());
		dbo.setIsIndividual(dto.getIsIndividual());
		dbo.setName(dto.getName());
		dbo.setEtag(dto.getEtag());
	}
	
	public static void copyDboToDto(DBOUserGroup dbo, UserGroup dto) {
		if (dbo.getId()==null) {
			dto.setId(null); 
		} else {
			dto.setId(dbo.getId().toString());
		}
		dto.setCreationDate(dbo.getCreationDate());
		dto.setIsIndividual(dbo.getIsIndividual());
		dto.setName(dbo.getName());
		dto.setEtag(dbo.getEtag());
	}
	
	public static void copyDboToDto(List<DBOUserGroup> dbos, List<UserGroup> dtos) {
		dtos.clear();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
	}

}
