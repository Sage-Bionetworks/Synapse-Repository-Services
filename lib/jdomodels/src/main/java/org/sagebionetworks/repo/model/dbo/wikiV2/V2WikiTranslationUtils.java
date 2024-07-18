package org.sagebionetworks.repo.model.dbo.wikiV2;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
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
	public static V2WikiPage createDTOfromDBO(V2DBOWikiPage dtoPage, List<String> fileHandleIds, V2DBOWikiMarkdown dboMarkdown){
		if(dtoPage == null) throw new IllegalArgumentException("WikiPage dbo cannot be null");
		if(fileHandleIds == null) throw new IllegalArgumentException("List of attachments cannot be null");
		if(dboMarkdown == null) throw new IllegalArgumentException("Markdown file handle id cannot be null");
		
		V2WikiPage page = new V2WikiPage();
		page.setAttachmentFileHandleIds(fileHandleIds);
		page.setId(dtoPage.getId().toString());
		page.setEtag(dtoPage.getEtag());
		page.setTitle(dboMarkdown.getTitle());
		page.setCreatedBy(dtoPage.getCreatedBy().toString());
		if(dtoPage.getCreatedOn() != null){
			page.setCreatedOn(new Date(dtoPage.getCreatedOn()));
		}
		page.setModifiedBy(dboMarkdown.getModifiedBy().toString());
		if(dtoPage.getModifiedOn() != null){
			page.setModifiedOn(new Date(dboMarkdown.getModifiedOn()));
		}
		if(dtoPage.getParentId() != null){
			page.setParentWikiId(dtoPage.getParentId().toString());
		}
		page.setMarkdownFileHandleId(dboMarkdown.getFileHandleId().toString());
		
		return page;
	}
	
	/**
	 * Create a wiki order hint DTO from the DBO.
	 * @param page
	 * @param attachments
	 * @return
	 */
	public static V2WikiOrderHint createWikiOrderHintDTOfromDBO(V2DBOWikiOwner dbo){
		if(dbo == null) throw new IllegalArgumentException("WikiOwner dbo cannot be null");
		
		V2WikiOrderHint dto = new V2WikiOrderHint();
		dto.setOwnerId(dbo.getOwnerId().toString());
		dto.setOwnerObjectType(ObjectType.valueOf(dbo.getOwnerType()));
		dto.setEtag(dbo.getEtag());
		
		// Set order hint
		byte[] orderHintBytes = dbo.getOrderHint();
		if (orderHintBytes != null) {
			try {
				dto.setIdList(getOrderHintIdListFromBytes(dbo.getOrderHint()));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		
		return dto;
	}
	
	/**
	 * Create a wiki owner DBO from the DTO
	 * @param page
	 * @param attachments
	 * @return
	 */
	public static V2DBOWikiOwner createWikiOwnerDBOfromOrderHintDTO(V2WikiOrderHint dto, String rootWikiId){
		if(dto == null) throw new IllegalArgumentException("Order Hint dto cannot be null");
		if(rootWikiId == null) throw new IllegalArgumentException("Root Wiki Id rootWikiId cannot be null");
		
		V2DBOWikiOwner dbo = new V2DBOWikiOwner();
		dbo.setOwnerId(Long.parseLong(dto.getOwnerId()));
		dbo.setOwnerType(dto.getOwnerObjectType().name());
		dbo.setRootWikiId(Long.parseLong(rootWikiId));
		dbo.setEtag(dto.getEtag());

		if (dto.getIdList() == null) {
			dbo.setOrderHint(null);
		} else {
			StringBuffer orderHintCSV = new StringBuffer();
			for (int i = 0; i < dto.getIdList().size(); i++) {
				if (i > 0) {
					orderHintCSV.append(',');
				}
				orderHintCSV.append(dto.getIdList().get(i));
			}
			String listString = orderHintCSV.toString();
			try {
				dbo.setOrderHint(listString.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		
		return dbo;
	}
	
	private static List<String> getOrderHintIdListFromBytes(byte[] orderHintBytes) throws UnsupportedEncodingException {
		String idHintString = new String(orderHintBytes, "UTF-8");
		String[] idHintArray = idHintString.split(",");
		return Arrays.asList(idHintArray);
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

	/**
	 * This method converts a byte array into a list of attachments.
	 * 
	 * @param bytes
	 * @return
	 */
	public static List<WikiAttachment> convertByteArrayToWikiAttachmentList(byte[] bytes) {
		try {
			String attachmentList = new String(bytes, "UTF-8");
			List<WikiAttachment> wikiAttachmentList = new ArrayList<WikiAttachment>();
			if(attachmentList == null || attachmentList.length() == 0) {
				return wikiAttachmentList;
			}
			String[] attachments = attachmentList.split(",");
			for(String attachment: attachments) {
				if (attachment == null || attachment.length() == 0) {
					continue;
				}
				String[] idName = attachment.split(":");
				if (idName.length != 2) {
					throw new IllegalArgumentException("Wrong format for wiki attachment string: "+attachment);
				}
				String fileName = decodeForParsing(idName[1]);
				String id = decodeForParsing(idName[0]);
				wikiAttachmentList.add(new WikiAttachment(id, fileName));
			}
			return wikiAttachmentList;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a list of file handle id from the given attachment list
	 * 
	 * @param attachmentList
	 * @return
	 */
	public static List<String> getFileHandleIdList(List<WikiAttachment> attachmentList) {
		ValidateArgument.required(attachmentList, "attachmentList");
		List<String> fileHandleIdList = new ArrayList<String>();
		for (WikiAttachment attachment : attachmentList) {
			fileHandleIdList.add(attachment.getFileHandleId());
		}
		return fileHandleIdList;
	}

	/**
	 * Look up and return file handle Id for file Name.
	 * 
	 * @param attachmentList
	 * @param fileName
	 * @param notFoundException if the fileName does not appear in the list
	 * @return
	 */
	public static String lookupFileHandleIdAndThrowException(List<WikiAttachment> attachmentList, String fileName,
			NotFoundException notFoundException) {
		ValidateArgument.required(attachmentList, "attachmentList");
		ValidateArgument.required(fileName, "fileName");
		ValidateArgument.required(notFoundException, "notFoundException");
		for (WikiAttachment attachment : attachmentList) {
			if (attachment.getFileName().equals(fileName)) {
				return attachment.getFileHandleId();
			}
		}
		throw notFoundException;
	}
}
