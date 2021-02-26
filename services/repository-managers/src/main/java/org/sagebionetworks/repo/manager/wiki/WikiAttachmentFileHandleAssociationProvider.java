package org.sagebionetworks.repo.manager.wiki;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.wikiV2.V2DBOWikiAttachmentReservation;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class WikiAttachmentFileHandleAssociationProvider implements FileHandleAssociationProvider {

	private V2WikiPageDao wikiPageDaoV2;
	private FileHandleAssociationScanner scanner;
	
	public WikiAttachmentFileHandleAssociationProvider(V2WikiPageDao wikiPageDaoV2, NamedParameterJdbcTemplate jdbcTemplate) {
		this.wikiPageDaoV2 = wikiPageDaoV2;
		// Note: This table contains all the attachments of a wiki plus the wiki id itself
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new V2DBOWikiAttachmentReservation().getTableMapping());
		
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		return wikiPageDaoV2.getFileHandleIdsAssociatedWithWikiAttachments(fileHandleIds, objectId);
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.WIKI;
	}

	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}

}
