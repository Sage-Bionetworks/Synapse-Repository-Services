package org.sagebionetworks.repo.model.jdo.temp;

import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class TempResourceManagerImpl implements TempResourceManager {

	@Override
	public Map<ResourceTypes, Object> getDatasetData(String datsetId,
			Set<ResourceTypes> typesToFetch) {
		// TODO Auto-generated method stub
		return null;
	}

}
