package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class ForumUtils {

	/**
	 * Translate from a DTO to DBO.
	 * @param dto
	 * @return dbo
	 */
	public static DBOForum createDBOFromDTO(Forum dto) {
		DBOForum dbo = new DBOForum();
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setProjectId(KeyFactory.stringToKey(dto.getProjectId()));
		return dbo;
	}

	/**
	 * Translate from a DBO to DTO.
	 * @param dbo
	 * @return dto
	 */
	public static Forum createDTOFromDBO(DBOForum dbo) {
		Forum dto = new Forum();
		dto.setId(dbo.getId().toString());
		dto.setProjectId(KeyFactory.keyToString(dbo.getProjectId()));
		dto.setEtag(dbo.getEtag());
		return dto;
	}
}
