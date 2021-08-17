package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.springframework.stereotype.Service;

@Service
public class DatasetMetadataIndexProvider implements MetadataIndexProvider {

	@Override
	public ViewObjectType getObjectType() {
		return ViewObjectType.DATASET;
	}

	@Override
	public List<String> getSubTypesForMask(Long typeMask) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isFilterScopeByObjectId(Long typeMask) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ColumnType getIdColumnType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ColumnType getParentIdColumnType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ColumnType getBenefactorIdColumnType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectType getBenefactorObjectType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DefaultColumnModel getDefaultColumnModel(Long viewTypeMask) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Long> getContainerIdsForScope(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String createViewOverLimitMessage(Long viewTypeMask, int containerLimit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<Annotations> getAnnotations(UserInfo userInfo, String objectId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateAnnotations(UserInfo userInfo, String objectId, Annotations annotations) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canUpdateAnnotation(ColumnModel model) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<Long> getContainerIdsForReconciliation(Set<Long> scope, Long viewTypeMask, int containerLimit)
			throws LimitExceededException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Long> getAvailableContainers(List<Long> containerIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IdAndEtag> getChildren(Long containerId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, Long> getSumOfChildCRCsForEachContainer(List<Long> containerIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void validateTypeMask(Long viewTypeMask) {
		// TODO Auto-generated method stub

	}

}
