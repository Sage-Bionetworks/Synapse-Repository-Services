package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProvider;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
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
	public ReplicationType getReplicationType() {
		return ReplicationType.ENTITY;
	}

	@Override
	public Iterator<IdAndChecksum> streamOverIdsAndChecksumsForChildren(Long salt, Set<Long> parentIds,
			Set<SubType> subTypes) {
		return  nodeDao.getIdsAndChecksumsForChildren(salt, parentIds, subTypes).iterator();
	}

	@Override
	public Iterator<IdAndChecksum> streamOverIdsAndChecksumsForObjects(Long salt, Set<Long> objectIds) {
		return nodeDao.getIdsAndChecksumsForObjects(salt, objectIds).iterator();
	}

}
