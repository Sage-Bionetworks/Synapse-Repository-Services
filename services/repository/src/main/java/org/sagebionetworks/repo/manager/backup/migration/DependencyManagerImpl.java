/**
 * 
 */
package org.sagebionetworks.repo.manager.backup.migration;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableDAO;
import org.sagebionetworks.repo.model.ObjectData;
import org.sagebionetworks.repo.model.QueryResults;

/**
 * @author brucehoff
 *
 */
public class DependencyManagerImpl implements DependencyManager {

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.backup.migration.DependencyManager#getDependencies(java.lang.String, java.util.List)
	 */
	
	/*
	 * autowired by Spring
	 */
	List<MigratableDAO> migratableDaos;
	
	public List<MigratableDAO> getMigratableDaos() {
		return migratableDaos;
	}

	public void setMigratableDaos(List<MigratableDAO> migratableDaos) {
		this.migratableDaos = migratableDaos;
	}

	@Override
	public QueryResults<ObjectData> getAllObjects(long offset, long limit, boolean includeDependencies) throws DatastoreException {
			long total = 0L;
			List<ObjectData> ods = new ArrayList<ObjectData>();
			for (MigratableDAO migratableDAO : getMigratableDaos()) {
				long numNeeded = limit-ods.size();
				long localOffset = Math.max(0, offset-total); // offset relative to current DAO's results
				if (numNeeded>0L) {
					QueryResults<ObjectData> localResults = 
						migratableDAO.getMigrationObjectData(localOffset, numNeeded, includeDependencies);
					ods.addAll(localResults.getResults());
					total += localResults.getTotalNumberOfResults();
				} else {
					total += migratableDAO.getCount();
				}
			}
			QueryResults<ObjectData> queryResults = new QueryResults<ObjectData>();
			queryResults.setResults(ods);
			queryResults.setTotalNumberOfResults((int)total);
			return queryResults;
	}

}
