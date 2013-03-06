/**
 * 
 */
package org.sagebionetworks.repo.manager.backup.migration;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableDAO;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectType;
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
	public QueryResults<MigratableObjectData> getAllObjects(long offset, long limit, boolean includeDependencies) throws DatastoreException {
			long total = 0L;
			List<MigratableObjectData> ods = new ArrayList<MigratableObjectData>();
			for (MigratableDAO migratableDAO : getMigratableDaos()) {
				long numNeeded = limit-ods.size();
				long localOffset = Math.max(0, offset-total); // offset relative to current DAO's results
				if (numNeeded>0L) {
					QueryResults<MigratableObjectData> localResults = 
						migratableDAO.getMigrationObjectData(localOffset, numNeeded, includeDependencies);
					ods.addAll(localResults.getResults());
					total += localResults.getTotalNumberOfResults();
				} else {
					total += migratableDAO.getCount();
				}
			}
			QueryResults<MigratableObjectData> queryResults = new QueryResults<MigratableObjectData>();
			queryResults.setResults(ods);
			queryResults.setTotalNumberOfResults((int)total);
			return queryResults;
	}
	@Override
	public QueryResults<MigratableObjectCount> getAllObjectsCounts() throws DatastoreException {
		List<MigratableObjectCount> ods = new ArrayList<MigratableObjectCount>();
		long total = 0L;
		for (MigratableDAO migratableDAO: getMigratableDaos()) {
			long c = migratableDAO.getCount();
			MigratableObjectCount moc = new MigratableObjectCount();
			moc.setCount(c);
			moc.setObjectType(migratableDAO.getMigratableObjectType().name()); 
			ods.add(moc);
			total += 1;
		}
		QueryResults<MigratableObjectCount> queryResults = new QueryResults<MigratableObjectCount>();
		queryResults.setResults(ods);
		queryResults.setTotalNumberOfResults((int)total);
		return queryResults;
	}
	
	/**
	 * Validation is done here.
	 */
	public void init(){
		if(migratableDaos == null) throw new IllegalArgumentException("The list of migratableDaos cannot be null");
		if(migratableDaos.size() != MigratableObjectType.values().length) throw new IllegalArgumentException("The size of migratableDaos: "+migratableDaos.size()+" does not match the number of MigratableObjectTypes: "+MigratableObjectType.values().length);
		// the order must match
		for(int i=0; i<MigratableObjectType.values().length; i++){
			MigratableDAO dao = migratableDaos.get(i);
			if(dao.getMigratableObjectType() == null) throw new IllegalArgumentException(dao.getClass().getName()+" returned null for getMigratableObjectType()");
			// the order must match
			if(dao.getMigratableObjectType() != MigratableObjectType.values()[i]){
				throw new IllegalArgumentException("The order of the MigratableObjectType enum must match the order of migratableDaos in DependencyManagerImpl.  Expected: "+MigratableObjectType.values()[i].name()+" but found: "+dao.getMigratableObjectType()+" at index: "+i);
			}
		}
	}


}
