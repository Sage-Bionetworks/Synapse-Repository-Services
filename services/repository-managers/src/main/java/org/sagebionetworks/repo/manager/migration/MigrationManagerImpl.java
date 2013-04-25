package org.sagebionetworks.repo.manager.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.MigatableTableDAO;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of migration manager.
 * 
 * @author John
 *
 */
public class MigrationManagerImpl implements MigrationManager {
	
	@Autowired
	MigatableTableDAO migratableTableDao;

	@Override
	public long getCount(UserInfo user, MigrationType type) {
		validateUser(user);
		// pass this to the dao.
		return migratableTableDao.getCount(type);
	}

	@Override
	public RowMetadataResult getRowMetadaForType(UserInfo user,  MigrationType type, long limit, long offset) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// pass this to the dao.
		return migratableTableDao.listRowMetadata(type, limit, offset);
	}

	@Override
	public RowMetadataResult getRowMetadataDeltaForType(UserInfo user, MigrationType type, List<String> idList) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Get the list from the DAO and convert to a result
		List<RowMetadata> list = migratableTableDao.listDeltaRowMetadata(type, idList);
		RowMetadataResult result = new RowMetadataResult();
		result.setList(list);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void writeBackupBatch(UserInfo user, MigrationType type, List<String> rowIds, OutputStream out) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Get the database object from the dao
		MigratableDatabaseObject mdo = migratableTableDao.getObjectForType(type);
		// Forward to the generic method
		writeBackupBatch(mdo, type, rowIds, out);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <B> int[] createOrUpdateBatch(UserInfo user,	MigrationType type, InputStream in) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Get the database object from the dao
		MigratableDatabaseObject mdo = migratableTableDao.getObjectForType(type);
		return createOrUpdateBatch(mdo, type, in);
	}

	@Override
	public int deleteObjectsById(UserInfo user, MigrationType type,  List<String> idList) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		return migratableTableDao.deleteObjectsById(type, idList);
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
	private <D extends DatabaseObject<D>, B> void writeBackupBatch(MigratableDatabaseObject<D, B> mdo, MigrationType type, List<String> rowIds, OutputStream out){
		// First get the database object from the database
		List<D> databaseList = migratableTableDao.getBackupBatch(mdo.getDatabaseObjectClass(), rowIds);
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
	 * The Generics version of the create/update batch.
	 * @param mdo
	 * @param type
	 * @param batch
	 * @param in
	 */
	private <D extends DatabaseObject<D>, B> int[] createOrUpdateBatch(MigratableDatabaseObject<D, B> mdo, MigrationType type, InputStream in){
		// we use the table name as the Alias
		String alias = mdo.getTableMapping().getTableName();
		// Read the list from the stream
		@SuppressWarnings("unchecked")
		List<B> backupList = (List<B>) BackupMarshalingUtils.readBacckupFromStream(mdo.getBackupClass(), alias, in);
		// Now translate from the backup objects to the database objects.
		MigratableTableTranslation<D, B> translator = mdo.getTranslator();
		List<D> databaseList = new LinkedList<D>();
		for(B backup: backupList){
			databaseList.add(translator.createDatabaseObjectFromBackup(backup));
		}
		// Now write the batch to the database
		return migratableTableDao.createOrUpdateBatch(databaseList);
	}

}
