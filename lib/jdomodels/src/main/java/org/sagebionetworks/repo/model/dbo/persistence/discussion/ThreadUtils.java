package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
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
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
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
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setMessageUrl(dbo.getMessageUrl());
		dto.setIsEdited(dbo.getIsEdited());
		dto.setIsDeleted(dbo.getIsDeleted());
		return dto;
	}

	/**
	 * validate that some fields in the DTO are not null
	 * 
	 * @param dto
	 */
	public static void validateCreateDTOAndThrowException(DiscussionThread dto) {
		if (dto.getForumId() == null
				|| dto.getTitle() == null 
				|| dto.getCreatedOn() == null
				|| dto.getCreatedBy() == null 
				|| dto.getModifiedOn() == null
				|| dto.getMessageUrl() == null 
				|| dto.getIsEdited() == null
				|| dto.getIsDeleted() == null) {
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
