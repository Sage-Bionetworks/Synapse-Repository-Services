package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

@Service
public class DatasetMetadataIndexProvider implements MetadataIndexProvider {
	
	
	private NodeDAO nodeDao;
	private NodeManager nodeManager;
	
	// @formatter:off
	static final DefaultColumnModel BASIC_ENTITY_DEAFULT_COLUMNS = DefaultColumnModel.builder(ViewObjectType.DATASET)
			.withObjectField(
					ObjectField.id, 
					ObjectField.name, 
					ObjectField.createdOn, 
					ObjectField.createdBy,
					ObjectField.etag, 
					ObjectField.modifiedOn, 
					ObjectField.modifiedBy
			).build();
	// @formatter:on
	
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
	public List<String> getSubTypesForMask(Long typeMask) {
		// currently only files are supported.
		return ImmutableList.of(EntityType.file.name());
	}

	@Override
	public boolean isFilterScopeByObjectId(Long typeMask) {
		return true;
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
	public ObjectType getBenefactorObjectType() {
		return ObjectType.ENTITY;
	}

	@Override
	public DefaultColumnModel getDefaultColumnModel(Long viewTypeMask) {
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		return BASIC_ENTITY_DEAFULT_COLUMNS;
	}

	@Override
	public List<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars) {
		return nodeDao.getEntityDTOs(objectIds, maxAnnotationChars);
	}

	@Override
	public Set<Long> getContainerIdsForScope(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException {
		// The datasets are not hierarchical, so the scope cannot be expanded
		return scope;
	}
	
	@Override
	public Set<Long> getContainerIdsForReconciliation(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException {
		return getContainerIdsForScope(scope, viewTypeMask, containerLimit);
	}

	@Override
	public String createViewOverLimitMessage(Long viewTypeMask, int containerLimit) {
		return String.format("Maximum of %,d items in a dataset exceeded.", containerLimit);
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
	public void validateTypeMask(Long viewTypeMask) {
		// not currently using the view type mask
	}

}
