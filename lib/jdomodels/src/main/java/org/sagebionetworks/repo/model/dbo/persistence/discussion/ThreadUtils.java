package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.discussion.DiscussionThread;

public class ThreadUtils {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Translate from a DTO to DBO.
	 * @param dto
	 * @return dbo
	 * @throws IOException 
	 */
	public static DBOThread createDBOFromDTO(DiscussionThread dto) {
		DBOThread dbo = new DBOThread();
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setForumId(Long.parseLong(dto.getForumId()));
		dbo.setTitle(dto.getTitle().getBytes(UTF8));
		dbo.setCreatedOn(dto.getCreatedOn());
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setModifiedOn(dto.getModifiedOn());
		dbo.setMessageUrl(dto.getMessageUrl());
		dbo.setIsEdited(dto.getIsEdited());
		dbo.setIsDeleted(dto.getIsDeleted());
		return dbo;
	}

	/**
	 * Translate from a DBO to DTO.
	 * @param dbo
	 * @return dto
	 */
	public static DiscussionThread createDTOFromDBO(DBOThread dbo) {
		DiscussionThread dto = new DiscussionThread();
		dto.setId(dbo.getId().toString());
		dto.setForumId(dbo.getForumId().toString());
		dto.setTitle(new String (dbo.getTitle(), UTF8));
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setModifiedOn(dbo.getModifiedOn());
		dto.setMessageUrl(dbo.getMessageUrl());
		dto.setIsEdited(dbo.getIsEdited());
		dto.setIsDeleted(dbo.getIsDeleted());
		return dto;
	}

	/**
	 * validate that all fields in the DBO are not null
	 * 
	 * @param dbo
	 */
	public static void validateDBOAndThrowException(DBOThread dbo) {
		if (dbo.getId() == null || dbo.getForumId() == null
				|| dbo.getTitle() == null || dbo.getCreatedOn() == null
				|| dbo.getCreatedBy() == null || dbo.getModifiedOn() == null
				|| dbo.getMessageUrl() == null || dbo.getIsEdited() == null
				|| dbo.getIsDeleted() == null || dbo.getEtag() == null) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * 
	 * @param results
	 * @return
	 */
	public static List<DiscussionThread> createDTOListFromDBOList(List<DBOThread> dboList) {
		List<DiscussionThread> dtoList = new ArrayList<DiscussionThread>();
		for (DBOThread dbo : dboList) {
			dtoList.add(createDTOFromDBO(dbo));
		}
		return dtoList;
	}
}
