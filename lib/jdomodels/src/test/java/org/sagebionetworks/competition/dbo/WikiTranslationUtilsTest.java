package org.sagebionetworks.competition.dbo;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.WikiTranslationUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiAttachment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiPage;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.wiki.WikiPage;

/**
 * Test for WikiTranslationUtils.
 * 
 * @author jmhill
 *
 */
public class WikiTranslationUtilsTest {
	

	@Test
	public void testRoundTrip(){
		S3FileHandle handleOne = new S3FileHandle();
		handleOne.setId("1974");
		handleOne.setFileName("foo.bar");
		S3FileHandle handleTwo = new S3FileHandle();
		handleTwo.setId("1975");
		handleTwo.setFileName("bar.txt");
		// Start with the DTO.
		WikiPage dto = new WikiPage();
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
		// We also must map the fileHandles to the file name
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(handleOne.getFileName(), handleOne);
		fileNameMap.put(handleTwo.getFileName(), handleTwo);
		dto.setMarkdown("This is the actual text of the wiki!!!");
		// Now to DBO
		DBOWikiPage wikiDBO = WikiTranslationUtils.createDBOFromDTO(dto);
		List<DBOWikiAttachment> attachmentsDBO = WikiTranslationUtils.createDBOAttachmentsFromDTO(fileNameMap, 123l);
		// Now back
		WikiPage clone = WikiTranslationUtils.createDTOfromDBO(wikiDBO, attachmentsDBO);
		Collections.sort(clone.getAttachmentFileHandleIds());
		assertNotNull(clone);
		assertEquals(dto, clone);
	}

}
