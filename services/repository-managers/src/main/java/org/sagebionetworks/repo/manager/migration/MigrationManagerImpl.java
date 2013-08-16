package org.sagebionetworks.repo.manager.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.MigatableTableDAO;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.ListBucketProvider;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationUtils;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic implementation of migration manager.
 * 
 * @author John
 *
 */
public class MigrationManagerImpl implements MigrationManager {
	
	@Autowired
	MigatableTableDAO migratableTableDao;

	/**
	 * The maximum size of a backup batch.
	 */
	int backupBatchMax = 500;

	/**
	 * Used for unit testing.
	 * @param migratableTableDao
	 * @param backupBatchMax
	 */
	public MigrationManagerImpl(MigatableTableDAO migratableTableDao, int backupBatchMax) {
		super();
		this.migratableTableDao = migratableTableDao;
		this.backupBatchMax = backupBatchMax;
	}
	
	public MigrationManagerImpl(){
		// Default the batch max 
		this.backupBatchMax = 500;
	}

	/**
	 * Injected via Spring
	 * @param backupBatchMax
	 */
	public void setBackupBatchMax(Integer backupBatchMax) {
		this.backupBatchMax = backupBatchMax;
	}

	@Override
	public long getCount(UserInfo user, MigrationType type) {
		validateUser(user);
		// pass this to the dao.
		return migratableTableDao.getCount(type);
	}
	
	@Override
	public long getMaxId(UserInfo user, MigrationType type) {
		validateUser(user);
		return migratableTableDao.getMaxId(type);
	}

	@Override
	public RowMetadataResult getRowMetadaForType(UserInfo user,  MigrationType type, long limit, long offset) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// pass this to the dao.
		return migratableTableDao.listRowMetadata(type, limit, offset);
	}

	@Override
	public RowMetadataResult getRowMetadataDeltaForType(UserInfo user, MigrationType type, List<Long> idList) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Get the list from the DAO and convert to a result
		List<RowMetadata> list = migratableTableDao.listDeltaRowMetadata(type, idList);
		RowMetadataResult result = new RowMetadataResult();
		result.setList(list);
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@SuppressWarnings("unchecked")
	@Override
	public void writeBackupBatch(UserInfo user, MigrationType type, List<Long> rowIds, OutputStream out) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Get the database object from the dao
		MigratableDatabaseObject mdo = migratableTableDao.getObjectForType(type);
		// Forward to the generic method
		writeBackupBatch(mdo, type, rowIds, out);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> createOrUpdateBatch(UserInfo user, MigrationType type, InputStream in) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Get the database object from the dao
		MigratableDatabaseObject mdo = migratableTableDao.getObjectForType(type);
		return createOrUpdateBatch(mdo, type, in);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public int deleteObjectsById(UserInfo user, MigrationType type, List<Long> idList) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Delete must be done in reverse dependency order, so we must get the row metadata for 
		// the input list
		int count = 0;
		List<RowMetadata> list =  migratableTableDao.listDeltaRowMetadata(type, idList);
		if(list.size() > 0){
			// Bucket all data by the level in the tree
			ListBucketProvider provider = new ListBucketProvider();
			MigrationUtils.bucketByTreeLevel(list.iterator(), provider);
			// Now delete the buckets in reverse order
			// This will ensure children are deleted before their parents
			List<List<Long>> buckets = provider.getListOfBuckets();
			if(buckets.size() > 0){
				for(int i=buckets.size()-1; i>=0; i--){
					List<Long> bucket = buckets.get(i);
					count += migratableTableDao.deleteObjectsById(type, bucket);
				}
			}
		}
		return count;
	}
	
	/**
	 * Validate that the user is an administrator.
	 * 
	 * @param user
	 */
	private void validateUser(UserInfo user){
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(!user.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
	}
	
	/**
	 * The Generics version of the write to backup.
	 * @param mdo
	 * @param type
	 * @param rowIds
	 * @param out
	 */
	protected <D extends DatabaseObject<D>, B> void writeBackupBatch(MigratableDatabaseObject<D, B> mdo, MigrationType type, List<Long> rowIds, OutputStream out){
		// Get all of the data from the DAO batched.
		List<D> databaseList = getBackupDataBatched(mdo.getDatabaseObjectClass(), rowIds);
		// Translate to the backup objects
		MigratableTableTranslation<D, B> translator = mdo.getTranslator();
		List<B> backupList = new LinkedList<B>();
		for(D dbo: databaseList){
			backupList.add(translator.createBackupFromDatabaseObject(dbo));
		}
		// Now write the backup list to the stream
		// we use the table name as the Alias
		String alias = mdo.getTableMapping().getTableName();
		// Now write the backup to the stream
		BackupMarshalingUtils.writeBackupToStream(backupList, alias, out);
	}

	/**
	 * Get all of the backup data for a list of IDs using batching.
	 * @param mdo
	 * @param rowIds
	 * @return
	 */
	protected <D extends DatabaseObject<D>> List<D> getBackupDataBatched(Class<? extends D> clazz, List<Long> rowIds) {
		List<Long> batch = new LinkedList<Long>();
		List<D> results = new LinkedList<D>();
		Iterator<Long> it = rowIds.iterator();
		while(it.hasNext()){
			Long id = it.next();
			batch.add(id);
			if(batch.size() >= backupBatchMax){
				results.addAll(migratableTableDao.getBackupBatch(clazz, batch));
				batch.clear();
			}
		}
		if(batch.size() > 0){
			results.addAll(migratableTableDao.getBackupBatch(clazz, batch));
			batch.clear();
		}
		return results;
	}
	
	/**
	 * The Generics version of the create/update batch.
	 * @param mdo
	 * @param type
	 * @param batch
	 * @param in
	 */
	private <D extends DatabaseObject<D>, B> List<Long> createOrUpdateBatch(MigratableDatabaseObject<D, B> mdo, MigrationType type, InputStream in){
		// we use the table name as the Alias
		String alias = mdo.getTableMapping().getTableName();
		// Read the list from the stream
		@SuppressWarnings("unchecked")
		List<B> backupList = (List<B>) BackupMarshalingUtils.readBackupFromStream(mdo.getBackupClass(), alias, in);
		if(backupList != null && !backupList.isEmpty()){
			// Now translate from the backup objects to the database objects.
			MigratableTableTranslation<D, B> translator = mdo.getTranslator();
			List<D> databaseList = new LinkedList<D>();
			for(B backup: backupList){
				databaseList.add(translator.createDatabaseObjectFromBackup(backup));
			}
			// Now write the batch to the database
			return migratableTableDao.createOrUpdateBatch(databaseList);
		}else{
			return new LinkedList<Long>();
		}

	}

	@Override
	public List<MigrationType> getPrimaryMigrationTypes(UserInfo user) {
		validateUser(user);
		return migratableTableDao.getPrimaryMigrationTypes();
	}

	@Override
	public List<MigrationType> getSecondaryTypes(MigrationType type) {
		// Get the primary type.
		MigratableDatabaseObject primary = migratableTableDao.getObjectForType(type);
		List<MigratableDatabaseObject> secondary = primary.getSecondaryTypes();
		if(secondary != null){
			List<MigrationType> list = new LinkedList<MigrationType>();
			for(MigratableDatabaseObject mdo: secondary){
				list.add(mdo.getMigratableTableType());
			}
			return list;
		}
		return null;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAllData(UserInfo user) throws Exception {
		validateUser(user);
		// Delete all types in their reverse order
		for(int i=MigrationType.values().length-1; i>=0; i--){
			deleteAllForType(user, MigrationType.values()[i]);
		}
	}

	private void deleteAllForType(UserInfo user, MigrationType type){
		// First get all data for this type.
		RowMetadataResult result =  migratableTableDao.listRowMetadata(type, Long.MAX_VALUE, 0);
		List<RowMetadata> list =result.getList();
		if(list.size() > 0){
			// Create the list of IDs to delete
			List<Long> toDelete = new LinkedList<Long>();
			for(RowMetadata row: list){
				toDelete.add(row.getId());
			}
			deleteObjectsById(user, type, toDelete);
		}
	}
	

}
