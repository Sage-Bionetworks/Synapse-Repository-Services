package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityMetadataIndexProvider implements MetadataIndexProvider {
	
	private NodeManager nodeManager;
	private NodeDAO nodeDao;
	
	@Autowired
	public EntityMetadataIndexProvider(NodeManager nodeManager, NodeDAO nodeDao) {
		this.nodeManager = nodeManager;
		this.nodeDao = nodeDao;
	}

	@Override
	public List<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars) {
		return nodeDao.getEntityDTOs(objectIds, maxAnnotationChars);
	}
	
	@Override
	public Set<Long> getAllContainerIdsForScope(Set<Long> scope, Long viewTypeMask, int containerLimit) throws LimitExceededException {
		if(ViewTypeMask.Project.getMask() == viewTypeMask){
			return scope;
		}
		// Expand the scope to include all sub-folders
		return nodeDao.getAllContainerIds(scope, containerLimit);
	}
	
	@Override
	public boolean canUpdateAnnotation(ColumnModel model) {
		// No additional field is indexed, so all the annotations can be updated
		return true;
	}
	
	@Override
	public Annotations getAnnotations(UserInfo userInfo, String objectId) {
		return nodeManager.getUserAnnotations(userInfo, objectId);
	}
	
	@Override
	public void updateAnnotations(UserInfo userInfo, String objectId, Annotations annotations) {
		nodeManager.updateUserAnnotations(userInfo, objectId, annotations);
	}
	
	@Override
	public ViewObjectType getObjectType() {
		return ViewObjectType.ENTITY;
	}

	@Override
	public ColumnType getIdColumnType() {
		return ColumnType.ENTITYID;
	}

	@Override
	public ColumnType getParentIdColumnType() {
		return ColumnType.ENTITYID;
	}

	@Override
	public ColumnType getBenefactorIdColumnType() {
		return ColumnType.ENTITYID;
	}

	@Override
	public boolean supportsSubtypeFiltering() {
		return true;
	}

	@Override
	public List<String> getSubTypesForMask(Long typeMask) {
		List<String> typesFilter = new ArrayList<>();
		for(ViewTypeMask type: ViewTypeMask.values()) {
			if ((type.getMask() & typeMask) > 0) {
				typesFilter.add(type.getEntityType().name());
			}
		}
		return typesFilter;
	}

	@Override
	public boolean isFilterScopeByObjectId(Long typeMask) {
		if(ViewTypeMask.Project.getMask() == typeMask) {
			return true;
		}
		return false;
	}


}
