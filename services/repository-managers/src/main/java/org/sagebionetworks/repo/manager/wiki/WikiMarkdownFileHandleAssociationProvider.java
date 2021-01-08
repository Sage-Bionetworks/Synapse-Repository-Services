package org.sagebionetworks.repo.manager.wiki;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.springframework.beans.factory.annotation.Autowired;

public class WikiMarkdownFileHandleAssociationProvider implements FileHandleAssociationProvider {
	
	@Autowired
	V2WikiPageDao wikiPageDaoV2;

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(
			List<String> fileHandleIds, String objectId) {
		return wikiPageDaoV2.getFileHandleIdsAssociatedWithWikiMarkdown(fileHandleIds, objectId);
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.WIKI;
	}


}
