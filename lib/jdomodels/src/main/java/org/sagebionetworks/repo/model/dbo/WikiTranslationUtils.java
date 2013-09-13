package org.sagebionetworks.repo.model.dbo;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.backup.WikiPageAttachmentBackup;
import org.sagebionetworks.repo.model.backup.WikiPageBackup;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
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

	/**
	 * Create a WikiPageBackup from the DBOs
	 * @param key
	 * @param dbo
	 * @param attachments
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static WikiPageBackup createWikiBackupFromDBO(WikiPageKey key, DBOWikiPage dbo, List<DBOWikiAttachment> attachments) {
		if(key == null) throw new IllegalArgumentException("WikiPageKey cannot be null");
		if(dbo == null) throw new IllegalArgumentException("DBOWikiPage cannot be null");
		if(attachments == null) throw new IllegalArgumentException("attachments cannot be null");
		WikiPageBackup backup = new WikiPageBackup();
		backup.setOwnerId(Long.parseLong(key.getOwnerObjectId()));
		backup.setOwnerType(key.getOwnerObjectType().name());
		backup.setId(Long.parseLong(key.getWikiPageId()));
		backup.setAttachmentFileHandles(new LinkedList<WikiPageAttachmentBackup>());
		for(DBOWikiAttachment att: attachments){
			backup.getAttachmentFileHandles().add(new WikiPageAttachmentBackup(att.getFileHandleId(), att.getFileName()));
		}
		backup.setCreatedBy(dbo.getCreatedBy());
		backup.setCreatedOn(dbo.getCreatedOn());
		backup.setEtag(dbo.getEtag());
		if(dbo.getMarkdown() != null){
			try {
				backup.setMarkdown(new String(dbo.getMarkdown(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		backup.setModifiedBy(dbo.getModifiedBy());
		backup.setModifiedOn(dbo.getModifiedOn());
		backup.setParentWikiId(dbo.getParentId());
		backup.setTitle(dbo.getTitle());
		return backup;
	}
	
	/**
	 * Extract the WikiPageKey from a backup.
	 * @param backup
	 * @return
	 */
	public static WikiPageKey createWikiPageKeyFromBackup(WikiPageBackup backup){
		if(backup == null) throw new IllegalArgumentException("WikiPageBackup cannot be null");
		return new WikiPageKey(backup.getOwnerId().toString(), ObjectType.valueOf(backup.getOwnerType()), backup.getId().toString());
	}
	
	/**
	 * Create DBOWikiPage from a backup
	 * 
	 * @param backup
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static DBOWikiPage createWikiPageDBOFromBackup(WikiPageBackup backup) {
		if(backup == null) throw new IllegalArgumentException("WikiPageBackup cannot be null");
		DBOWikiPage dbo = new DBOWikiPage();
		dbo.setId(new Long(backup.getId()));
		dbo.setCreatedBy(backup.getCreatedBy());
		dbo.setCreatedOn(backup.getCreatedOn());
		dbo.setEtag(backup.getEtag());
		if(backup.getMarkdown() != null){
			try {
				dbo.setMarkdown(backup.getMarkdown().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		dbo.setModifiedBy(backup.getModifiedBy());
		dbo.setModifiedOn(backup.getModifiedOn());
		dbo.setParentId(backup.getParentWikiId());
		dbo.setTitle(backup.getTitle());
		return dbo;
	}
	
	/**
	 * Create the attachments from a backup
	 * @param backup
	 * @return
	 */
	public static List<DBOWikiAttachment> createWikiPageDBOAttachmentsFromBackup(WikiPageBackup backup){
		if(backup == null) throw new IllegalArgumentException("WikiPageBackup cannot be null");
		List<DBOWikiAttachment> list = new LinkedList<DBOWikiAttachment>();
		if(backup.getAttachmentFileHandles() != null){
			for(WikiPageAttachmentBackup wpab: backup.getAttachmentFileHandles()){
				DBOWikiAttachment dboAttachment = new DBOWikiAttachment();
				dboAttachment.setFileHandleId(wpab.getFileHandleId());
				dboAttachment.setFileName(wpab.getFileName());
				dboAttachment.setWikiId(backup.getId());
				list.add(dboAttachment);
			}
		}
		return list;
	}
}
