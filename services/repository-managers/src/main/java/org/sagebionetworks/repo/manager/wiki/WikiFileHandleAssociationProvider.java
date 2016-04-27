package org.sagebionetworks.repo.manager.wiki;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.springframework.beans.factory.annotation.Autowired;

public class WikiFileHandleAssociationProvider implements FileHandleAssociationProvider {

	@Autowired
	V2WikiPageDao wikiPageDaoV2;

	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		return wikiPageDaoV2.getFileHandleIdsAssociatedWithWiki(fileHandleIds, objectId);
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.WIKI;
	}

}
