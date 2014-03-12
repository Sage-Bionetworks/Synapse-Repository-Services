package org.sagebionetworks.repo.model.dbo;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiAttachmentReservation;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiMarkdown;
import org.sagebionetworks.repo.model.dbo.v2.persistence.V2DBOWikiPage;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
/**
 * Utility for translating to/from V2 DTO/DBO
 * (Derived from org.sagebionetworks.repo.model.dbo.WikiTranslationUtils)
 * 
 * @author hso
 *
 */
public class V2WikiTranslationUtils {
	public static final String ENCODED_COLON = "&#58;";
	public static final String COLON = ":";
	
	public static final String ENCODED_COMMA = "&#44;";
	public static final String COMMA = ",";
	
	/**
	 * Create a DBO from the DTO.
	 * @param dto
	 * @return
	 */
	public static V2DBOWikiPage createDBOFromDTO(V2WikiPage dto){
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
	public static V2WikiPage createDTOfromDBO(V2DBOWikiPage dtoPage, List<String> fileHandleIds, V2DBOWikiMarkdown dtoMarkdown){
		if(dtoPage == null) throw new IllegalArgumentException("WikiPage dbo cannot be null");
		if(fileHandleIds == null) throw new IllegalArgumentException("List of attachments cannot be null");
		if(dtoMarkdown == null) throw new IllegalArgumentException("Markdown file handle id cannot be null");
		
		V2WikiPage page = new V2WikiPage();
		page.setAttachmentFileHandleIds(fileHandleIds);
		page.setId(dtoPage.getId().toString());
		page.setEtag(dtoPage.getEtag());
		page.setTitle(dtoMarkdown.getTitle());
		page.setCreatedBy(dtoPage.getCreatedBy().toString());
		if(dtoPage.getCreatedOn() != null){
			page.setCreatedOn(new Date(dtoPage.getCreatedOn()));
		}
		page.setModifiedBy(dtoMarkdown.getModifiedBy().toString());
		if(dtoPage.getModifiedOn() != null){
			page.setModifiedOn(new Date(dtoMarkdown.getModifiedOn()));
		}
		if(dtoPage.getParentId() != null){
			page.setParentWikiId(dtoPage.getParentId().toString());
		}
		page.setMarkdownFileHandleId(dtoMarkdown.getFileHandleId().toString());
		
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
	public static List<V2DBOWikiAttachmentReservation> createDBOAttachmentReservationFromDTO(List<String> fileHandleIdsToInsert, 
			Long wikiId, Long timestamp){
		
		if(fileHandleIdsToInsert == null) throw new IllegalArgumentException("fileNameToFileIdMap cannot be null");
		if(wikiId == null) throw new IllegalArgumentException("wikiId cannot be null"); 
		if(timestamp == null) throw new IllegalArgumentException("timestamp cannot be null");
		
		List<V2DBOWikiAttachmentReservation> list = new LinkedList<V2DBOWikiAttachmentReservation>();
		for(String id: fileHandleIdsToInsert) {
			V2DBOWikiAttachmentReservation attachmentEntry = new V2DBOWikiAttachmentReservation();
			
			attachmentEntry.setWikiId(wikiId);
			attachmentEntry.setFileHandleId(Long.parseLong(id));
			attachmentEntry.setTimeStamp(new Timestamp(timestamp));
			list.add(attachmentEntry);
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
			Long wikiId, Long markdownFileHandleId, String title) {
		
		if(fileNameToFileHandleMap == null) throw new IllegalArgumentException("fileNameToFileIdMap cannot be null");
		if(wikiId == null) throw new IllegalArgumentException("wikiId cannot be null"); 
		if(markdownFileHandleId == null) throw new IllegalArgumentException("markdownFileHandleId cannot be null");
		
		V2DBOWikiMarkdown dbo = new V2DBOWikiMarkdown();
		dbo.setWikiId(wikiId);
		dbo.setFileHandleId(markdownFileHandleId);
		dbo.setTitle(title);
		// Escape special delimiters and build a list of fileHandleId:fileName entries
		StringBuffer attachmentIdList = new StringBuffer();
		for(String fileName: fileNameToFileHandleMap.keySet()) {
			String escapedFileName = encodeForDatabase(fileName);
			String escapedId = encodeForDatabase(fileNameToFileHandleMap.get(fileName).getId());

			attachmentIdList.append(escapedId + COLON);
			attachmentIdList.append(escapedFileName + ",");
		}

		// String class's split method will remove trailing empty strings for the fencepost ','
		
		String listString = attachmentIdList.toString();
		try {
			dbo.setAttachmentIdList(listString.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return dbo;
	}

	/**
	 * Process the list into a mapping of file names to file handle ids
	 * @param attachmentIdList
	 * @return
	 */
	public static Map<String, String> getFileNameAndHandleIdPairs(String attachmentIdList) {
		if(attachmentIdList == null) throw new IllegalArgumentException("attachmentIdList cannot be null for parsing");
		Map<String, String> fileNameToIdMap = new HashMap<String, String>();
		if(attachmentIdList.length() > 0) {
			// attachmentIdList string should be escaped and ready to split with delimiters
			String[] attachments = attachmentIdList.split(",");

			for(String attachment: attachments) {
				String[] idName = attachment.split(":");
				//should only be length 2
				String fileName = decodeForParsing(idName[1]);
				String id = decodeForParsing(idName[0]);
				fileNameToIdMap.put(fileName, id);
			}
			
		}		
		return fileNameToIdMap;
	}
	
	private static String encodeForDatabase(String str) {
		String encoded = str.replaceAll(COLON, ENCODED_COLON);
		encoded = encoded.replaceAll(COMMA, ENCODED_COMMA);
		return encoded;
	}
	
	private static String decodeForParsing(String str) {
		String decoded = str.replaceAll(ENCODED_COLON, COLON);
		decoded = decoded.replaceAll(ENCODED_COMMA, COMMA);
		return decoded;
	}
	
	public static String getStringFromByteArray(byte[] array) {
		String listToString;
		try {
			listToString = new String(array, "UTF-8");	
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return listToString;
	}
	
	/**
	 * Parses the attachment list and returns a list of the file handle ids
	 * @param attachmentsList
	 * @return
	 */
	public static List<String> createFileHandleListFromString(String attachmentsList) {
		List<String> fileHandleIds = new ArrayList<String>();
		if(attachmentsList != null) {
			// Process the list of attachments into a map for easy searching
			Map<String, String> fileNameToIdMap = V2WikiTranslationUtils.getFileNameAndHandleIdPairs(attachmentsList);
			for(String fileName: fileNameToIdMap.keySet()) {
				fileHandleIds.add(fileNameToIdMap.get(fileName));
			}
		}
		return fileHandleIds;
	}
}
