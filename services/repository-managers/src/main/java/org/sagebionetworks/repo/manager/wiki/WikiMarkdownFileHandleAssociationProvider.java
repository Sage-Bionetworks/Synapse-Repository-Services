package org.sagebionetworks.repo.manager.wiki;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.wikiV2.V2DBOWikiMarkdown;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WikiMarkdownFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	private V2WikiPageDao wikiPageDaoV2;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public WikiMarkdownFileHandleAssociationProvider(V2WikiPageDao wikiPageDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.wikiPageDaoV2 = wikiPageDao;
		// Note: the wiki might also contain attachements, those are stored in the serialized field of the wiki but also in a dedicated table
		// that is actually scanned with the scanner provided by the dedicated WikiAttachmentFileHandleAssociationProvider
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new V2DBOWikiMarkdown().getTableMapping());
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.WikiMarkdown;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(
			List<String> fileHandleIds, String objectId) {
		return wikiPageDaoV2.getFileHandleIdsAssociatedWithWikiMarkdown(fileHandleIds, objectId);
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
