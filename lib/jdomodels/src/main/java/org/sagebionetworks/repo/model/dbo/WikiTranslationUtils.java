package org.sagebionetworks.repo.model.dbo;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiAttachment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiPage;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.wiki.WikiPage;

/**
 * Utility for translating to/from DTO/DBO
 * 
 * @author jmhill
 *
 */
public class WikiTranslationUtils {
	
	/**
	 * Create a wiki page DTO from the DBOs;
	 * @param page
	 * @param attachments
	 * @return
	 */
	public static WikiPage createDTOfromDBO(DBOWikiPage dtoPage, List<DBOWikiAttachment> dtoAttachments){
		WikiPage page = new WikiPage();
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.setId(dtoPage.getId().toString());
		page.setEtag(dtoPage.getEtag());
		page.setTitle(dtoPage.getTitle());
		page.setCreatedBy(dtoPage.getCreatedBy().toString());
		if(dtoPage.getCreatedOn() != null){
			page.setCreatedOn(new Date(dtoPage.getCreatedOn()));
		}
		page.setModifiedBy(dtoPage.getModifiedBy().toString());
		if(dtoPage.getModifiedOn() != null){
			page.setModifiedOn(new Date(dtoPage.getModifiedOn()));
		}
		if(dtoPage.getParentId() != null){
			page.setParentWikiId(dtoPage.getParentId().toString());
		}
		if(dtoPage.getMarkdown() != null){
			try {
				page.setMarkdown(new String(dtoPage.getMarkdown(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		// Add the attachments
		for(DBOWikiAttachment attachment: dtoAttachments){
			page.getAttachmentFileHandleIds().add(attachment.getFileHandleId().toString());
		}
		return page;
	}

	/**
	 * Create a DBO from the DTO.
	 * @param dto
	 * @return
	 */
	public static DBOWikiPage createDBOFromDTO(WikiPage dto){
		if(dto == null) throw new IllegalArgumentException("DTO cannot be null");
		DBOWikiPage page = new DBOWikiPage();
		if(dto.getId() != null){
			page.setId(new Long(dto.getId()));
		}
		page.setEtag(dto.getEtag());
		page.setTitle(dto.getTitle());
		if(dto.getCreatedBy() != null){
			page.setCreatedBy(new Long(dto.getCreatedBy()));
		}
		if(dto.getCreatedOn() != null){
			page.setCreatedOn(dto.getCreatedOn().getTime());
		}
		if(dto.getModifiedBy() != null){
			page.setModifiedBy(new Long(dto.getModifiedBy()));
		}
		if(dto.getModifiedOn() != null){
			page.setModifiedOn(dto.getModifiedOn().getTime());
		}
		if(dto.getParentWikiId() != null){
			page.setParentId(new Long(dto.getParentWikiId()));
		}
		if(dto.getMarkdown() != null){
			try {
				page.setMarkdown(dto.getMarkdown().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		return page;
	}
	
	/**
	 * Create a list of DBOWikiAttachment for all attachments of a DTO.
	 * @param dto
	 * @return
	 */
	public static List<DBOWikiAttachment> createDBOAttachmentsFromDTO(Map<String, FileHandle> fileNameToFileHandleMap, Long wikiId){
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileIdMap cannot be null");
		if(wikiId == null) throw new IllegalArgumentException("wikiId cannot be null"); 
		List<DBOWikiAttachment> list = new LinkedList<DBOWikiAttachment>();
		if(fileNameToFileHandleMap != null){
			for(String fileName: fileNameToFileHandleMap.keySet()){
				DBOWikiAttachment attachment = new DBOWikiAttachment();
				FileHandle handle = fileNameToFileHandleMap.get(fileName);
				if(handle == null) throw new IllegalArgumentException("FileHandle is null for fileName: "+fileName);
				if(handle.getId() == null) throw new IllegalArgumentException("FileHandle.getId id null for fileName: "+fileName);
				if(!fileName.equals(handle.getFileName())) throw new IllegalArgumentException("Map fileName does not mach the FileHandle.getFileName()");
				attachment.setWikiId(wikiId);
				attachment.setFileHandleId(Long.parseLong(handle.getId()));
				attachment.setFileName(handle.getFileName());
				list.add(attachment);
			}
		}
		return list;
	}
}
