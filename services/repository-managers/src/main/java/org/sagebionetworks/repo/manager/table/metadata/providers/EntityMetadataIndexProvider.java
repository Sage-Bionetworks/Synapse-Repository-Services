package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.view.filter.FlatIdsFilter;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

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
			.withObjectField(Constants.BASIC_DEAFULT_COLUMNS).build();

	static final DefaultColumnModel FILE_VIEW_DEFAULT_COLUMNS = DefaultColumnModel.builder(OBJECT_TYPE)
			.withObjectField(Constants.BASIC_DEAFULT_COLUMNS)
			.withObjectField(Constants.FILE_SPECIFIC_COLUMNS).build();

	private final NodeManager nodeManager;
	private final NodeDAO nodeDao;
	private final ViewScopeDao viewScopeDao;

	@Autowired
	public EntityMetadataIndexProvider(NodeManager nodeManager, NodeDAO nodeDao, ViewScopeDao viewScopeDao) {
		this.nodeManager = nodeManager;
		this.nodeDao = nodeDao;
		this.viewScopeDao = viewScopeDao;
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

	public Set<SubType> getSubTypesForMask(Long typeMask) {
		ValidateArgument.required(typeMask, "viewTypeMask");
		Set<SubType> typesFilter = new HashSet<>();
		for (ViewTypeMask type : ViewTypeMask.values()) {
			if ((type.getMask() & typeMask) > 0) {
				typesFilter.add(SubType.valueOf(type.getEntityType().name()));
			}
		}
		return typesFilter;
	}

	@Override
	public ViewFilter getViewFilter(Long viewId) {
		ViewScopeType type = viewScopeDao.getViewScopeType(viewId);
		Set<Long> scope = viewScopeDao.getViewScope(viewId);
		return getViewFilter(type.getTypeMask(), scope);
	}

	@Override
	public ViewFilter getViewFilter(Long typeMask, Set<Long> scope) {
		Set<SubType> subTypes = getSubTypesForMask(typeMask);
		if (ViewTypeMask.Project.getMask() == typeMask) {
			return new FlatIdsFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.project), scope);
		}else {
			try {
				Set<Long> allContainers = nodeDao.getAllContainerIds(scope, TableConstants.MAX_CONTAINERS_PER_VIEW);
				return new HierarchicaFilter(ReplicationType.ENTITY, subTypes, allContainers);
			} catch (LimitExceededException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Override
	public void validateScopeAndType(Long typeMask, Set<Long> scopeIds, int maxContainersPerView) {
		if ((typeMask & ViewTypeMask.Project.getMask()) > 0) {
			if (typeMask != ViewTypeMask.Project.getMask()) {
				throw new IllegalArgumentException(PROJECT_TYPE_CANNOT_BE_COMBINED_WITH_ANY_OTHER_TYPE);
			}
		}
		
		if(scopeIds != null) {
			if (ViewTypeMask.Project.getMask() == typeMask) {
				if(scopeIds.size() > maxContainersPerView) {
					throw new IllegalArgumentException(String.format(SCOPE_SIZE_LIMITED_EXCEEDED_PROJECT_VIEW, maxContainersPerView));
				}
			}else {
				try {
					nodeDao.getAllContainerIds(scopeIds, (int) maxContainersPerView);
				} catch (LimitExceededException e) {
					throw new IllegalArgumentException(String.format(SCOPE_SIZE_LIMITED_EXCEEDED_FILE_VIEW, maxContainersPerView));
				}
			}
		}
	}
}
