package org.sagebionetworks.competition.dbo;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.WikiTranslationUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiAttachment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOWikiPage;
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
		dto.getAttachmentFileHandleIds().add("1974");
		dto.getAttachmentFileHandleIds().add("1975");
		dto.setMarkdown("This is the actual text of the wiki!!!");
		// Now to DBO
		DBOWikiPage wikiDBO = WikiTranslationUtils.createDBOFromDTO(dto);
		List<DBOWikiAttachment> attachmentsDBO = WikiTranslationUtils.createDBOAttachmentsFromDTO(dto, 123l);
		// Now back
		WikiPage clone = WikiTranslationUtils.createDTOfromDBO(wikiDBO, attachmentsDBO);
		assertNotNull(clone);
		assertEquals(dto, clone);
	}

}
