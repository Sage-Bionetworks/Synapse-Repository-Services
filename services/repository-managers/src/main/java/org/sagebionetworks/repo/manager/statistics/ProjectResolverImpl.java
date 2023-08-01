package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProjectResolverImpl implements ProjectResolver {

	private NodeDAO nodeDao;

	@Autowired
	public ProjectResolverImpl(NodeDAO nodeDao) {
		this.nodeDao = nodeDao;
	}

	@Override
	public Optional<Long> resolveProject(FileHandleAssociateType associationType, String associationId) {
		switch (associationType) {
		case FileEntity:
		case TableEntity:
			return getProjectForNode(associationId);
		default:
			return Optional.empty();
		}
	}

	private Optional<Long> getProjectForNode(String objectId) {
		Optional<String> projectIdString = nodeDao.getProjectId(objectId);
		return projectIdString.map(KeyFactory::stringToKey);
	}

}
