package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Test for V2WikiTranslationUtils.
 * 
 * @author hso
 *
 */
public class V2WikiTranslationUtilsTest {
	
	@Test
	public void testCreateDBOWikiMarkdownFromDTO() {
		Long wikiId = new Long(123);
		Long markdownFileHandleId = new Long(456);
		String wikiTitle = "Title";
		S3FileHandle handleOne = new S3FileHandle();
		handleOne.setId("19,74");
		handleOne.setFileName("f:oo.bar");
		S3FileHandle handleTwo = new S3FileHandle();
		handleTwo.setId("19,75");
		handleTwo.setFileName("ba:r.txt");
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(handleOne.getFileName(), handleOne);
		fileNameMap.put(handleTwo.getFileName(), handleTwo);
		
		V2DBOWikiMarkdown markdownDbo = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameMap, wikiId, markdownFileHandleId, wikiTitle);
		byte[] list = markdownDbo.getAttachmentIdList();
		List<WikiAttachment> attachmentList = V2WikiTranslationUtils.convertByteArrayToWikiAttachmentList(list);
		
		//Order of id:name pairs varies because it is constructed from a set
		WikiAttachment attachment1 = new WikiAttachment("19,74", "f:oo.bar");
		WikiAttachment attachment2 = new WikiAttachment("19,75", "ba:r.txt");
		assertEquals(2, attachmentList.size());
		assertTrue(attachmentList.contains(attachment1));
		assertTrue(attachmentList.contains(attachment2));
		
		// Send in an empty map == no attachments
		fileNameMap.clear();
		V2DBOWikiMarkdown markdownDbo2 = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameMap, wikiId, markdownFileHandleId, wikiTitle + "2");
		assertTrue(markdownDbo2.getAttachmentIdList().length == 0);
	}

	@Test
	public void testCreateDBOAttachmentReservationFromDTO() {
		Long wikiId = new Long(123);
		Long timeStamp = new Long(4567);
		Timestamp ts = new Timestamp(timeStamp);
		
		S3FileHandle handleOne = new S3FileHandle();
		handleOne.setId("1974");
		Long id = Long.parseLong("1974");
		
		List<String> newFileHandlesToInsert = new ArrayList<String>();
		newFileHandlesToInsert.add(handleOne.getId());
		
		List<V2DBOWikiAttachmentReservation> attachments = V2WikiTranslationUtils.createDBOAttachmentReservationFromDTO(newFileHandlesToInsert, wikiId, timeStamp);
		assertTrue(attachments.size() == 1);
		V2DBOWikiAttachmentReservation attachment = attachments.get(0);
		assertTrue(attachment.getFileHandleId().equals(id));
		assertTrue(attachment.getWikiId().equals(wikiId));
		assertTrue(attachment.getTimeStamp().equals(ts));
	}

	@Test
	public void testCreateDTOfromDBO() {
		// Wiki's markdown file handle id
		Long markdownFileHandleId = new Long(333);
		// Wiki Dbo
		V2DBOWikiPage wikiDbo = new V2DBOWikiPage();
		Long id = new Long(123);
		Long user = new Long(111);
		Long time = new Long(222);
		wikiDbo.setCreatedBy(user);
		wikiDbo.setCreatedOn(time);
		wikiDbo.setEtag("etag");
		wikiDbo.setId(id);
		wikiDbo.setMarkdownVersion(new Long(0));
		wikiDbo.setModifiedBy(user);
		wikiDbo.setModifiedOn(time);
		wikiDbo.setParentId(null);
		wikiDbo.setRootId(id);
		wikiDbo.setTitle("Title");

		try {
			V2WikiTranslationUtils.createDTOfromDBO(wikiDbo, new ArrayList<String>(), null);
			fail("Null Markdown file handle id should not be allowed");
		} catch(IllegalArgumentException e) {
			// expected
		}
		
		V2DBOWikiMarkdown markdownDbo = new V2DBOWikiMarkdown();
		markdownDbo.setFileHandleId(markdownFileHandleId);
		markdownDbo.setMarkdownVersion(new Long(0));
		markdownDbo.setModifiedBy(user);
		markdownDbo.setModifiedOn(time);
		markdownDbo.setTitle("Title");
		markdownDbo.setWikiId(id);
		markdownDbo.setAttachmentIdList(new byte[0]);
		V2WikiPage pageClone = V2WikiTranslationUtils.createDTOfromDBO(wikiDbo, new ArrayList<String>(), markdownDbo);
		
		// WikiPage
		V2WikiPage page = new V2WikiPage();
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		page.setCreatedBy(String.valueOf(user));
		page.setCreatedOn(new Date(time));
		page.setEtag("etag");
		page.setId(String.valueOf(id));
		page.setMarkdownFileHandleId(String.valueOf(markdownFileHandleId));
		page.setModifiedBy(String.valueOf(user));
		page.setModifiedOn(new Date(time));
		page.setParentWikiId(null);
		page.setTitle("Title");
		
		assertTrue(pageClone.equals(page));
	}
	
	@Test
	public void testCreateDBOFromDTO() {
		try {
			V2WikiTranslationUtils.createDBOFromDTO(null);
			fail("Null dbo should not be allowed");
		} catch(IllegalArgumentException e) {
			// expected
		}
		Long markdownFileHandleId = new Long(333);
		Long fileHandleId = new Long(444);
		Long user = new Long(111);
		
		// Mimick the dbo that only passes through createDBOFromDto 
		// Some properties are not set until after we return from this method
		V2DBOWikiPage wikiDbo = new V2DBOWikiPage();
		wikiDbo.setCreatedBy(user);
		wikiDbo.setCreatedOn(null);
		wikiDbo.setEtag(null);
		wikiDbo.setId(null);
		wikiDbo.setMarkdownVersion(null);
		wikiDbo.setModifiedBy(user);
		wikiDbo.setModifiedOn(null);
		wikiDbo.setParentId(null);
		wikiDbo.setRootId(null);
		wikiDbo.setTitle("Title");
		
		// WikiPage
		V2WikiPage page = new V2WikiPage();
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		page.getAttachmentFileHandleIds().add(String.valueOf(fileHandleId));
		page.setCreatedBy(String.valueOf(user));
		page.setCreatedOn(null);
		page.setId(null);
		page.setMarkdownFileHandleId(String.valueOf(markdownFileHandleId));
		page.setModifiedBy(String.valueOf(user));
		page.setModifiedOn(null);
		page.setParentWikiId(null);
		page.setTitle("Title");
		
		V2DBOWikiPage dbo = V2WikiTranslationUtils.createDBOFromDTO(page);
		assertTrue(dbo.equals(wikiDbo));
	}
	
	@Test
	public void testRoundTrip() {
		S3FileHandle handleOne = new S3FileHandle();
		handleOne.setId("19,74");
		handleOne.setFileName("f:oo.bar");
		S3FileHandle handleTwo = new S3FileHandle();
		handleTwo.setId("19,75");
		handleTwo.setFileName("ba:r.txt");
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(handleOne.getFileName(), handleOne);
		fileNameMap.put(handleTwo.getFileName(), handleTwo);
		
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("2000");
		markdown.setFileName("markdownContent");
		
		V2WikiPage dto = new V2WikiPage();
		dto.setId("123");
		dto.setEtag("etag");
		dto.setTitle("title");
		dto.setCreatedBy("456");
		dto.setCreatedOn(new Date(88l));
		dto.setModifiedBy("987");
		dto.setModifiedOn(new Date(99l));
		dto.setParentWikiId("0");
		dto.setAttachmentFileHandleIds(new LinkedList<String>());
		dto.getAttachmentFileHandleIds().add(handleOne.getId());
		dto.getAttachmentFileHandleIds().add(handleTwo.getId());
		dto.setMarkdownFileHandleId(markdown.getId());
		
		V2DBOWikiPage wikiDbo = V2WikiTranslationUtils.createDBOFromDTO(dto);
		V2DBOWikiMarkdown markdownDbo = V2WikiTranslationUtils.createDBOWikiMarkdownFromDTO(fileNameMap, wikiDbo.getId(), new Long(markdown.getId()), wikiDbo.getTitle());
		markdownDbo.setModifiedBy(wikiDbo.getModifiedBy());
		markdownDbo.setModifiedOn(wikiDbo.getModifiedOn());
		List<String> fileHandleIds = new ArrayList<String>();
		fileHandleIds.add(handleOne.getId());
		fileHandleIds.add(handleTwo.getId());
		V2WikiPage clone = V2WikiTranslationUtils.createDTOfromDBO(wikiDbo, fileHandleIds, markdownDbo);
		Collections.sort(clone.getAttachmentFileHandleIds());
		assertNotNull(clone);
		assertEquals(dto, clone);
	}
	
	@Test
	public void testCreateWikiOrderHintDTOfromDBO() throws UnsupportedEncodingException {
		V2DBOWikiOwner dbo = new V2DBOWikiOwner();
		String listString = "1,2,3,4,5";
		dbo.setOrderHint(listString.getBytes("UTF-8"));
		dbo.setEtag("etag");
		dbo.setOwnerId(new Long(456));
		dbo.setOwnerType(ObjectType.EVALUATION);
		
		V2WikiOrderHint dto = V2WikiTranslationUtils.createWikiOrderHintDTOfromDBO(dbo);
		
		assertTrue(Arrays.equals(dto.getIdList().toArray(), listString.split(",")));
		assertTrue(dto.getEtag().equals(dbo.getEtag()));
		assertTrue(dto.getOwnerId().equals(dbo.getOwnerId().toString()));
		assertTrue(dto.getOwnerObjectType().equals(dbo.getOwnerType()));
	}
	
	@Test
	public void testCreateWikiOrderHintDTOfromDBOEmptyIdList() throws UnsupportedEncodingException {
		V2DBOWikiOwner dbo = new V2DBOWikiOwner();
		String listString = "";
		dbo.setOrderHint(listString.getBytes("UTF-8"));
		dbo.setEtag("etag");
		dbo.setOwnerId(new Long(456));
		dbo.setOwnerType(ObjectType.EVALUATION);
		
		V2WikiOrderHint dto = V2WikiTranslationUtils.createWikiOrderHintDTOfromDBO(dbo);
		
		assertTrue(Arrays.equals(dto.getIdList().toArray(), listString.split(",")));
		assertTrue(dto.getEtag().equals(dbo.getEtag()));
		assertTrue(dto.getOwnerId().equals(dbo.getOwnerId().toString()));
		assertTrue(dto.getOwnerObjectType().equals(dbo.getOwnerType()));
	}
	
	@Test
	public void testCreateWikiOrderHintDTOfromDBONullIdList() throws UnsupportedEncodingException {
		V2DBOWikiOwner dbo = new V2DBOWikiOwner();
		dbo.setOrderHint(null);
		dbo.setEtag("etag");
		dbo.setOwnerId(new Long(456));
		dbo.setOwnerType(ObjectType.EVALUATION);
		
		V2WikiOrderHint dto = V2WikiTranslationUtils.createWikiOrderHintDTOfromDBO(dbo);
		
		assertTrue(dto.getIdList() == null);
		assertTrue(dto.getEtag().equals(dbo.getEtag()));
		assertTrue(dto.getOwnerId().equals(dbo.getOwnerId().toString()));
		assertTrue(dto.getOwnerObjectType().equals(dbo.getOwnerType()));
	}
	
	@Test
	public void testCreateWikiOwnerDBOfromOrderHintDTO() throws Exception {
		V2WikiOrderHint dto = new V2WikiOrderHint();
		dto.setEtag("etag");
		dto.setIdList(Arrays.asList(new String[] {"A", "X", "B", "Y"}));
		dto.setOwnerId("123");
		dto.setOwnerObjectType(ObjectType.EVALUATION);
		
		V2DBOWikiOwner dbo = V2WikiTranslationUtils.createWikiOwnerDBOfromOrderHintDTO(dto, "456");
		
		assertTrue(dbo.getEtag().equals(dto.getEtag()));
		assertTrue(dbo.getOwnerId().equals(Long.parseLong(dto.getOwnerId())));
		assertTrue(dbo.getRootWikiId().equals(Long.parseLong("456")));
		assertTrue(dbo.getOwnerType().equals(dto.getOwnerObjectType()));
	}
	
	@Test
	public void testOrderHintConversionRoundTrip() {
		V2WikiOrderHint dto = new V2WikiOrderHint();
		dto.setEtag("etag");
		dto.setIdList(Arrays.asList(new String[] {"A", "X", "B", "Y"}));
		dto.setOwnerId("123");
		dto.setOwnerObjectType(ObjectType.EVALUATION);
		
		// Convert to DBO.
		V2DBOWikiOwner dbo = V2WikiTranslationUtils.createWikiOwnerDBOfromOrderHintDTO(dto, "456");
		
		// Back to DTO.
		V2WikiOrderHint newDTO = V2WikiTranslationUtils.createWikiOrderHintDTOfromDBO(dbo);
		
		assertTrue(dto.getEtag().equals(newDTO.getEtag()));
		assertTrue(dto.getOwnerId().equals(newDTO.getOwnerId()));
		assertTrue(dto.getOwnerObjectType().equals(newDTO.getOwnerObjectType()));
		assertTrue(Arrays.equals(dto.getIdList().toArray(), newDTO.getIdList().toArray()));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetFileHandleIdListNullAttachmentList() {
		V2WikiTranslationUtils.getFileHandleIdList(null);
	}

	@Test
	public void testGetFileHandleIdListEmptyAttachmentList() {
		List<String> fileHandleIdList = V2WikiTranslationUtils.getFileHandleIdList(new ArrayList<WikiAttachment>(0));
		assertNotNull(fileHandleIdList);
		assertTrue(fileHandleIdList.isEmpty());
	}

	@Test
	public void testGetFileHandleIdList() {
		WikiAttachment attachment1 = new WikiAttachment("1", "file1");
		WikiAttachment attachment2 = new WikiAttachment("2", "file2");
		List<String> fileHandleIdList = V2WikiTranslationUtils.getFileHandleIdList(Arrays.asList(attachment1, attachment2));
		assertNotNull(fileHandleIdList);
		assertEquals(2, fileHandleIdList.size());
		assertTrue(fileHandleIdList.contains(attachment1.getFileHandleId()));
		assertTrue(fileHandleIdList.contains(attachment2.getFileHandleId()));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLookupFileHandleIdAndThrowExceptionNullList() {
		V2WikiTranslationUtils.lookupFileHandleIdAndThrowException(null, "fileName", new NotFoundException());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLookupFileHandleIdAndThrowExceptionNullFileName() {
		V2WikiTranslationUtils.lookupFileHandleIdAndThrowException(new ArrayList<WikiAttachment>(0), null, new NotFoundException());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLookupFileHandleIdAndThrowExceptionNullException() {
		V2WikiTranslationUtils.lookupFileHandleIdAndThrowException(new ArrayList<WikiAttachment>(0), "fileName", null);
	}

	@Test (expected = NotFoundException.class)
	public void testLookupFileHandleIdAndThrowExceptionEmptyList() {
		V2WikiTranslationUtils.lookupFileHandleIdAndThrowException(new ArrayList<WikiAttachment>(0), "fileName", new NotFoundException());
	}

	@Test (expected = NotFoundException.class)
	public void testLookupFileHandleIdAndThrowExceptionNotFound() {
		V2WikiTranslationUtils.lookupFileHandleIdAndThrowException(Arrays.asList(new WikiAttachment("1", "file1")), "fileName", new NotFoundException());
	}

	@Test
	public void testLookupFileHandleIdAndThrowExceptionFound() {
		assertEquals("1", V2WikiTranslationUtils.lookupFileHandleIdAndThrowException(Arrays.asList(new WikiAttachment("1", "file1")), "file1", new NotFoundException()));
	}

/*	@Test
	public void testConvertByteArrayToWikiAttachmentList*/
}
