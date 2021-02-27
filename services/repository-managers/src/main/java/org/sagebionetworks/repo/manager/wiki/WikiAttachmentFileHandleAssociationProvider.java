package org.sagebionetworks.repo.manager.wiki;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.springframework.stereotype.Service;

@Service
public class WikiAttachmentFileHandleAssociationProvider implements FileHandleAssociationProvider {

	private V2WikiPageDao wikiPageDaoV2;
	
	public WikiAttachmentFileHandleAssociationProvider(V2WikiPageDao wikiPageDaoV2) {
		this.wikiPageDaoV2 = wikiPageDaoV2;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.WikiAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		return wikiPageDaoV2.getFileHandleIdsAssociatedWithWikiAttachments(fileHandleIds, objectId);
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.WIKI;
	}

}
