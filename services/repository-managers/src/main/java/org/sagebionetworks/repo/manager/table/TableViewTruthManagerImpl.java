package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class TableViewTruthManagerImpl implements TableViewTruthManager {
	
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	ViewScopeDao viewScopeDao;

	@Override
	public Long calculateTableViewCRC(String tableId) {
		// Start with all container IDs that define the view's scope
		Set<Long> viewContainers = getAllContainerIdsForViewScope(tableId);
		
		long crc = nodeDao.calculateCRCForAllFilesWithinContainers(viewContainers);
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableViewTruthManager#getAllContainerIdsForViewScope(java.lang.String)
	 */
	@Override
	public Set<Long> getAllContainerIdsForViewScope(String viewIdString) {
		ValidateArgument.required(viewIdString, "viewId");
		Long viewId = KeyFactory.stringToKey(viewIdString);
		// Lookup the scope for this view.
		Set<Long> scope = viewScopeDao.getViewScope(viewId);
		// Add all containers from the given scope
		Set<Long> allContainersInScope = new HashSet<Long>(scope);
		for(Long container: scope){
			List<Long> containers = nodeDao.getAllContainerIds(container);
			allContainersInScope.addAll(containers);
		}
		return allContainersInScope;
	}

}
