package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityMetadataIndexProvider implements MetadataIndexProvider {

	private static final ViewObjectType OBJECT_TYPE = ViewObjectType.ENTITY;

	public static final String SCOPE_SIZE_LIMITED_EXCEEDED_FILE_VIEW = "The view's scope exceeds the maximum number of "
			+ "%d projects and/or folders. Note: The sub-folders of each project and folder in the scope count towards the limit.";
	public static final String SCOPE_SIZE_LIMITED_EXCEEDED_PROJECT_VIEW = "The view's scope exceeds the maximum number of "
			+ "%d projects.";
	public static final String PROJECT_TYPE_CANNOT_BE_COMBINED_WITH_ANY_OTHER_TYPE = "The Project type cannot be combined with any other type.";
	

	// @formatter:off
	static final DefaultColumnModel BASIC_ENTITY_DEAFULT_COLUMNS = DefaultColumnModel.builder(OBJECT_TYPE)
			.withObjectField(
					ObjectField.id, 
					ObjectField.name, 
					ObjectField.createdOn, 
					ObjectField.createdBy,
					ObjectField.etag, 
					ObjectField.modifiedOn, 
					ObjectField.modifiedBy
			).build();

	static final DefaultColumnModel FILE_VIEW_DEFAULT_COLUMNS = DefaultColumnModel.builder(OBJECT_TYPE)
			.withObjectField(
					ObjectField.id, 
					ObjectField.name, 
					ObjectField.createdOn, 
					ObjectField.createdBy,
					ObjectField.etag, 
					ObjectField.type, 
					ObjectField.currentVersion, 
					ObjectField.parentId,
					ObjectField.benefactorId, 
					ObjectField.projectId, 
					ObjectField.modifiedOn, 
					ObjectField.modifiedBy,
					ObjectField.dataFileHandleId, 
					ObjectField.dataFileSizeBytes, 
					ObjectField.dataFileMD5Hex
			).build();
	// @formatter:on

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
	public Set<Long> getContainerIdsForScope(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException {
		if (ViewTypeMask.Project.getMask() == viewTypeMask) {
			return scope;
		}

		// Expand the scope to include all sub-folders
		return nodeDao.getAllContainerIds(scope, containerLimit);
	}

	@Override
	public String createViewOverLimitMessage(Long viewTypeMask, int containerLimit) {
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		if (ViewTypeMask.Project.getMask() == viewTypeMask) {
			return String.format(SCOPE_SIZE_LIMITED_EXCEEDED_PROJECT_VIEW, containerLimit);
		} else {
			return String.format(SCOPE_SIZE_LIMITED_EXCEEDED_FILE_VIEW, containerLimit);
		}
	}

	@Override
	public boolean canUpdateAnnotation(ColumnModel model) {
		// No additional field is indexed, so all the annotations can be updated
		return true;
	}

	@Override
	public Optional<Annotations> getAnnotations(UserInfo userInfo, String objectId) {
		return Optional.ofNullable(nodeManager.getUserAnnotations(userInfo, objectId));
	}

	@Override
	public void updateAnnotations(UserInfo userInfo, String objectId, Annotations annotations) {
		nodeManager.updateUserAnnotations(userInfo, objectId, annotations);
	}

	@Override
	public Set<Long> getContainerIdsForReconciliation(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException {
		if (ViewTypeMask.Project.getMask() == viewTypeMask) {
			// project views reconcile with root.
			Long rootId = KeyFactory.stringToKey(NodeUtils.ROOT_ENTITY_ID);
			return Collections.singleton(rootId);
		} else {
			// all other views reconcile one the view's scope.
			return getContainerIdsForScope(scope, viewTypeMask, containerLimit);
		}
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
	public DefaultColumnModel getDefaultColumnModel(Long viewTypeMask) {
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		if ((viewTypeMask & ViewTypeMask.File.getMask()) > 0) {
			// mask includes files so return file columns.
			return FILE_VIEW_DEFAULT_COLUMNS;
		} else {
			// mask does not include files so return basic entity columns.
			return BASIC_ENTITY_DEAFULT_COLUMNS;
		}
	}

	@Override
	public ViewObjectType getObjectType() {
		return OBJECT_TYPE;
	}
	
	@Override
	public ObjectType getBenefactorObjectType() {
		return ObjectType.ENTITY;
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
	public List<String> getSubTypesForMask(Long typeMask) {
		ValidateArgument.required(typeMask, "viewTypeMask");
		List<String> typesFilter = new ArrayList<>();
		for (ViewTypeMask type : ViewTypeMask.values()) {
			if ((type.getMask() & typeMask) > 0) {
				typesFilter.add(type.getEntityType().name());
			}
		}
		return typesFilter;
	}

	@Override
	public boolean isFilterScopeByObjectId(Long typeMask) {
		ValidateArgument.required(typeMask, "viewTypeMask");
		if (ViewTypeMask.Project.getMask() == typeMask) {
			return true;
		}
		return false;
	}

	@Override
	public void validateTypeMask(Long viewTypeMask) {
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		
		if ((viewTypeMask & ViewTypeMask.Project.getMask()) > 0) {
			if (viewTypeMask != ViewTypeMask.Project.getMask()) {
				throw new IllegalArgumentException(PROJECT_TYPE_CANNOT_BE_COMBINED_WITH_ANY_OTHER_TYPE);
			}
		}
		
	}
}
