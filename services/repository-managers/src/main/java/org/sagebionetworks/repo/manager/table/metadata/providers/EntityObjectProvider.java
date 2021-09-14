package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProvider;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.util.PaginationIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityObjectProvider implements ObjectDataProvider {
	
	public static final int PAGE_SIZE = 10_000;
	private final NodeDAO nodeDao;
	
	@Autowired
	public EntityObjectProvider(NodeDAO nodeDao) {
		super();
		this.nodeDao = nodeDao;
	}

	@Override
	public Iterator<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars) {
		return new PaginationIterator<ObjectDataDTO>((long limit, long offset) -> {
			return nodeDao.getEntityDTOs(objectIds, maxAnnotationChars, limit, offset);
		}, PAGE_SIZE);
	}
	
	@Override
	public Set<Long> getAvailableContainers(List<Long> containerIds) {
		return nodeDao.getAvailableNodes(containerIds);
	}

	@Override
	public List<IdAndEtag> getChildren(Long containerId) {
		return nodeDao.getChildren(containerId);
	}

	@Override
	public Map<Long, Long> getSumOfChildCRCsForEachContainer(List<Long> containerIds) {
		return nodeDao.getSumOfChildCRCsForEachParent(containerIds);
	}

	@Override
	public ReplicationType getReplicationType() {
		return ReplicationType.ENTITY;
	}

}
