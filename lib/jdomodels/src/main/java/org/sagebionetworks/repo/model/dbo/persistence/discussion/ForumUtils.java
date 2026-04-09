package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;

public class ForumUtils {

	/**
	 * Translate from a DTO to DBO.
	 * @param dto
	 * @return dbo
	 */
	public static DBOForum createDBOFromDTO(Forum dto) {
		ValidateArgument.required(dto.getObjectId(), "objectId");
		ValidateArgument.required(dto.getObjectType(), "objectType");
		if(dto.getProjectId() !=null){
			throw new IllegalArgumentException("Project id should not be provided");
		}
		DBOForum dbo = new DBOForum();
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setObjectId(KeyFactory.stringToKey(dto.getObjectId()));
		dbo.setObjectType(dto.getObjectType().name());
		dbo.setEtag(dto.getEtag());
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
		String objectIdStr = KeyFactory.keyToString(dbo.getObjectId());
		dto.setObjectId(objectIdStr);
		ForumObjectType objectType = dbo.getObjectType() != null
				? ForumObjectType.valueOf(dbo.getObjectType())
				: ForumObjectType.ENTITY;
		dto.setObjectType(objectType);
		// Populate projectId for backward compatibility when it's an entity forum
		if (ForumObjectType.ENTITY.equals(objectType)) {
			dto.setProjectId(objectIdStr);
		}
		dto.setEtag(dbo.getEtag());
		return dto;
	}
}
