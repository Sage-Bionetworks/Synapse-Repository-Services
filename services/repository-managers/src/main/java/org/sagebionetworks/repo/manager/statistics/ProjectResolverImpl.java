package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectResolverImpl implements ProjectResolver {

	private NodeDAO nodeDao;

	@Autowired
	public ProjectResolverImpl(NodeDAO nodeDao) {
		this.nodeDao = nodeDao;
	}

	@Override
	public Long resolveProject(FileHandleAssociateType associationType, String associationId) {
		switch (associationType) {
		case FileEntity:
		case TableEntity:
			return getProjectForNode(associationId);
		default:
			throw new UnsupportedOperationException("Cannot resolve project for type " + associationType);
		}
	}

	private Long getProjectForNode(String objectId) {
		String projectIdString = nodeDao.getProjectId(objectId);
		return KeyFactory.stringToKey(projectIdString);
	}

}
