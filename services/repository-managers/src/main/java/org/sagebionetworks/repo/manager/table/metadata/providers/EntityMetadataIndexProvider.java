package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.springframework.stereotype.Service;

@Service
public class EntityMetadataIndexProvider implements MetadataIndexProvider {
	
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
