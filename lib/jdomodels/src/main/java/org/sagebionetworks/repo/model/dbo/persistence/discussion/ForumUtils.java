package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import org.sagebionetworks.repo.model.discussion.Forum;

public class ForumUtils {

	/**
	 * Translate from a DTO to DBO.
	 * @param dto
	 * @return dbo
	 */
	public static DBOForum createDBOFromDTO(Forum dto) {
		DBOForum dbo = new DBOForum();
		dbo.setId(dto.getId());
		dbo.setProjectId(dto.getProjectId());
		return dbo;
	}

	/**
	 * Translate from a DBO to DTO.
	 * @param dbo
	 * @return dto
	 */
	public static Forum createDTOFromDBO(DBOForum dbo) {
		Forum dto = new Forum();
		dto.setId(dbo.getId());
		dto.setProjectId(dbo.getProjectId());
		return dto;
	}
}
