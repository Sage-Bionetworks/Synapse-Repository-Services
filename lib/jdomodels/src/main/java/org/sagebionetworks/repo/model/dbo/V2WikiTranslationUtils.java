package org.sagebionetworks.repo.model.dbo;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.v2.wiki.WikiPage;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiPage;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiMarkdown;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiAttachmentReservation;
import org.sagebionetworks.repo.model.file.FileHandle;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
/**
 * Utility for translating to/from V2 DTO/DBO
 * (Derived from org.sagebionetworks.repo.model.dbo.WikiTranslationUtils)
 * 
 * @author hso
 *
 */
public class V2WikiTranslationUtils {

	/**
	 * Create a DBO from the DTO.
	 * @param dto
	 * @return
	 */
	public static V2DBOWikiPage createDBOFromDTO(WikiPage dto){
		if(dto == null) throw new IllegalArgumentException("DTO cannot be null");
		V2DBOWikiPage dbo = new V2DBOWikiPage();
		if(dto.getId() != null) {
			dbo.setId(new Long(dto.getId()));
		}
		dbo.setEtag(dto.getEtag());
		dbo.setTitle(dto.getTitle());
		if(dto.getCreatedBy() != null){
			dbo.setCreatedBy(new Long(dto.getCreatedBy()));
		}
		if(dto.getCreatedOn() != null){
			dbo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		if(dto.getModifiedBy() != null){
			dbo.setModifiedBy(new Long(dto.getModifiedBy()));
		}
		if(dto.getModifiedOn() != null){
			dbo.setModifiedOn(dto.getModifiedOn().getTime());
		}
		if(dto.getParentWikiId() != null){
			dbo.setParentId(new Long(dto.getParentWikiId()));
		}
		return dbo;
	}
	
	/**
	 * Create a wiki page DTO from the DBOs;
	 * @param page
	 * @param attachments
	 * @return
	 */
	public static WikiPage createDTOfromDBO(V2DBOWikiPage dtoPage, List<V2DBOWikiAttachmentReservation> dtoAttachments){
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
		if(dtoPage.getMarkdownVersion() != null){
			page.setMarkdownFileHandleId(dtoPage.getMarkdownVersion().toString());
		}
		for(V2DBOWikiAttachmentReservation attachment: dtoAttachments) {
			page.getAttachmentFileHandleIds().add(attachment.getFileHandleId().toString());
		}
		return page;
	}
	
	/**
	 * Create a list of DBOWikiAttachmentReservation for all attachments of a DTO.
	 * @param fileNameToFileHandleMap
	 * @param wikiId
	 * @param timestamp
	 * 
	 * @return
	 */
	public static List<V2DBOWikiAttachmentReservation> createDBOAttachmentReservationFromDTO(Map<String, FileHandle> fileNameToFileHandleMap, 
			Long wikiId, Long timestamp){
		
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileIdMap cannot be null");
		if(wikiId == null) throw new IllegalArgumentException("wikiId cannot be null"); 
		List<V2DBOWikiAttachmentReservation> list = new LinkedList<V2DBOWikiAttachmentReservation>();
		if(fileNameToFileHandleMap != null){
			for(String fileName: fileNameToFileHandleMap.keySet()) {
				V2DBOWikiAttachmentReservation attachmentEntry = new V2DBOWikiAttachmentReservation();
				FileHandle handle = fileNameToFileHandleMap.get(fileName);
				if(handle == null) throw new IllegalArgumentException("FileHandle is null for fileName: "+fileName);
				if(handle.getId() == null) throw new IllegalArgumentException("FileHandle.getId id null for fileName: "+fileName);
				
				attachmentEntry.setWikiId(wikiId);
				attachmentEntry.setFileHandleId(Long.parseLong(handle.getId()));
				attachmentEntry.setTimeStamp(new Timestamp(timestamp));
				list.add(attachmentEntry);
			}
		}
		return list;
	}
	
	/**
	 * Create DBO for WikiMarkdown from DTO.
	 * @param fileNameToFileHandleMap
	 * @param wikiId
	 * @param markdownFileHandleId
	 * 
	 * @return
	 */
	public static V2DBOWikiMarkdown createDBOWikiMarkdownFromDTO(Map<String, FileHandle> fileNameToFileHandleMap, 
			Long wikiId, Long markdownFileHandleId) {
		
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileIdMap cannot be null");
		if(wikiId == null) throw new IllegalArgumentException("wikiId cannot be null"); 
		if(markdownFileHandleId == null) throw new IllegalArgumentException("markdownFileHandleId cannot be null");
		
		// Build a string list of [fileHandleId:fileName] entries 
		StringBuffer attachmentIdList = new StringBuffer();
		for(String fileName: fileNameToFileHandleMap.keySet()) {
			attachmentIdList.append("[");
			attachmentIdList.append(fileNameToFileHandleMap.get(fileName) + ":");
			attachmentIdList.append(fileName + "],");
		}
		attachmentIdList.setLength(attachmentIdList.length() - 1);
		
		V2DBOWikiMarkdown dbo = new V2DBOWikiMarkdown();
		dbo.setWikiId(wikiId);
		dbo.setFileHandleId(markdownFileHandleId);
		dbo.setAttachmentIdList(attachmentIdList.toString());
		return dbo;
	}

}
