package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.view.filter.FlatIdAndVersionFilter;
import org.sagebionetworks.table.cluster.view.filter.FlatIdsFilter;
import org.sagebionetworks.table.cluster.view.filter.IdVersionPair;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DatasetMetadataIndexProvider implements MetadataIndexProvider {

	private final NodeDAO nodeDao;
	private final NodeManager nodeManager;

	static final DefaultColumnModel DATASET_FILE_COLUMNS = DefaultColumnModel.builder(ViewObjectType.DATASET)
			.withObjectField(Constants.BASIC_DEAFULT_COLUMNS)
			.withObjectField(Constants.FILE_SPECIFIC_COLUMNS).build();

	@Autowired
	public DatasetMetadataIndexProvider(NodeDAO nodeDao, NodeManager nodeManager) {
		super();
		this.nodeDao = nodeDao;
		this.nodeManager = nodeManager;
	}

	@Override
	public ViewObjectType getObjectType() {
		return ViewObjectType.DATASET;
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
	public DefaultColumnModel getDefaultColumnModel(Long viewTypeMask) {
		return DATASET_FILE_COLUMNS;
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
	public boolean canUpdateAnnotation(ColumnModel model) {
		return true;
	}

	@Override
	public ViewFilter getViewFilter(Long viewId) {
		List<EntityRef> items = nodeDao.getNodeItems(viewId);
		Set<IdVersionPair> scope = items.stream().map(i -> new IdVersionPair()
				.setId(KeyFactory.stringToKey(i.getEntityId())).setVersion(i.getVersionNumber()))
				.collect(Collectors.toSet());
		return new FlatIdAndVersionFilter(ReplicationType.ENTITY, getSubTypes(), scope);
	}

	@Override
	public ViewFilter getViewFilter(Long viewTypeMask, Set<Long> containerIds) {
		return new FlatIdsFilter(ReplicationType.ENTITY, getSubTypes(), containerIds);
	}

	Set<SubType> getSubTypes() {
		// currently only files are supported.
		return Set.of(SubType.file);
	}

	@Override
	public void validateScopeAndType(Long typeMask, Set<Long> scopeIds, int maxContainersPerView) {
		if (scopeIds != null && scopeIds.size() > maxContainersPerView) {
			throw new IllegalArgumentException(
					String.format(TableConstants.MAXIMUM_OF_ITEMS_IN_A_DATASET_EXCEEDED, maxContainersPerView));
		}
	}

}
