package org.sagebionetworks.repo.manager.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.pdfbox.io.IOUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.ForeignKeyInfo;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeListRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeResponse;
import org.sagebionetworks.repo.model.migration.ListBucketProvider;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationUtils;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


/**
 * Basic implementation of migration manager.
 * 
 * @author John
 *
 */
public class MigrationManagerImpl implements MigrationManager {
	
	public static final String BACKUP_KEY_TEMPLATE = "%1$s-%2$s-%3$s-%4$s.zip";
	public static String backupBucket = StackConfiguration.getSharedS3BackupBucket();
	public static String stack = StackConfiguration.singleton().getStack();
	public static String instance = StackConfiguration.getStackInstance();
	
	@Autowired
	MigratableTableDAO migratableTableDao;
	@Autowired
	StackStatusDao stackStatusDao;
	@Autowired
	BackupFileStream backupFileStream;
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	FileProvider fileProvider;
	
	/**
	 * The list of migration listeners
	 */
	List<MigrationTypeListener> migrationListeners;
	
	/**
	 * Migration types for principals.
	 */
	static final Set<MigrationType> PRINCIPAL_TYPES = Sets.newHashSet(
			MigrationType.PRINCIPAL, 
			MigrationType.CREDENTIAL,
			MigrationType.GROUP_MEMBERS
	);

	/**
	 * The maximum size of a backup batch.
	 */
	int backupBatchMax = 500;
	
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
	public long getMinId(UserInfo user, MigrationType type) {
		validateUser(user);
		return migratableTableDao.getMinId(type);
	}

	@Override
	public RowMetadataResult getRowMetadaForType(UserInfo user,  MigrationType type, long limit, long offset) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// pass this to the dao.
		return migratableTableDao.listRowMetadata(type, limit, offset);
	}
	
	@Override
	public RowMetadataResult getRowMetadataByRangeForType(UserInfo user, MigrationType type, long minId, long maxId, long limit, long offset) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// pass this to the dao.
		return migratableTableDao.listRowMetadataByRange(type, minId, maxId, limit, offset);
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

	@Deprecated
	@WriteTransaction
	@SuppressWarnings("unchecked")
	@Override
	public void writeBackupBatch(UserInfo user, MigrationType type, List<Long> rowIds, Writer writer, BackupAliasType backupAliasType) {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Get the database object from the dao
		MigratableDatabaseObject mdo = migratableTableDao.getObjectForType(type);
		// Forward to the generic method
		writeBackupBatch(mdo, rowIds, writer, backupAliasType);
	}

	@Deprecated
	@SuppressWarnings("unchecked")
	@Override
	public List<Long> createOrUpdateBatch(UserInfo user, final MigrationType type, final InputStream in, BackupAliasType backupAliasType) throws Exception {
		validateUser(user);
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		return migratableTableDao.runWithForeignKeyIgnored((Callable<List<Long>>) () -> {
			// Get the database object from the dao
			MigratableDatabaseObject mdo = migratableTableDao.getObjectForType(type);
			return createOrUpdateBatch(mdo, in, backupAliasType);
		});
	}

	@Deprecated
	@WriteTransaction
	@Override
	public int deleteObjectsById(final UserInfo user, final MigrationType type, final List<Long> idList) throws Exception {
		validateUser(user);
		// Do deletes with the foreign key checks off.
		return migratableTableDao.runWithForeignKeyIgnored(() -> {
			// If this type has secondary types then delete them first
			List<MigratableDatabaseObject> secondary = migratableTableDao.getObjectForType(type).getSecondaryTypes();
			if(secondary != null){
				for(int i=secondary.size()-1; i >= 0; i--){
					MigrationType secondaryType = secondary.get(i).getMigratableTableType();
					// Fire the event before deleting the objects
					fireDeleteBatchEvent(secondaryType, idList);
					deleteObjectsById(user, secondaryType, idList);
				}
			}

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
						// Fire the event before deleting the objects
						fireDeleteBatchEvent(type, bucket);
						count += migratableTableDao.deleteObjectsById(type, bucket);
					}
				}
			}
			return count;
		});
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
	 * @param rowIds
	 * @param backupAliasType
	 */
	@Deprecated
	protected <D extends DatabaseObject<D>, B> void writeBackupBatch(
			MigratableDatabaseObject<D, B> mdo, List<Long> rowIds, Writer writer, BackupAliasType backupAliasType) {
		// Get all of the data from the DAO batched.
		List<D> databaseList = getBackupDataBatched(mdo.getDatabaseObjectClass(), rowIds);
		// Translate to the backup objects
		MigratableTableTranslation<D, B> translator = mdo.getTranslator();
		List<B> backupList = new LinkedList<B>();
		for(D dbo: databaseList){
			backupList.add(translator.createBackupFromDatabaseObject(dbo));
		}
		String alias = getAlias(mdo, backupAliasType);
		// Now write the backup list to the stream
		BackupMarshalingUtils.writeBackupToWriter(backupList, alias, writer);
	}

	/**
	 * Get all of the backup data for a list of IDs using batching.
	 * @param clazz
	 * @param rowIds
	 * @return
	 */
	@Deprecated
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
	 * @param in
	 * @param backupAliasType
	 */
	private <D extends DatabaseObject<D>, B> List<Long> createOrUpdateBatch(
			MigratableDatabaseObject<D, B> mdo, InputStream in, BackupAliasType backupAliasType) {
		// Read the list from the buffer
		String alias = getAlias(mdo, backupAliasType);
		List<? extends B> backupList = BackupMarshalingUtils.readBackupFromStream(mdo.getBackupClass(), alias, in);
		if(backupList != null && !backupList.isEmpty()){
			// Now translate from the backup objects to the database objects.
			MigratableTableTranslation<D, B> translator = mdo.getTranslator();
			List<D> databaseList = new LinkedList<>();
			for(B backup: backupList){
				databaseList.add(translator.createDatabaseObjectFromBackup(backup));
			}
			// Now write the batch to the database
			List<Long> results = migratableTableDao.createOrUpdateBatch(databaseList);
			// Let listeners know about the change
			fireCreateOrUpdateBatchEvent(mdo.getMigratableTableType(), databaseList);
			return results;
		}else{
			return new LinkedList<>();
		}
	}

	/**
	 * Returns the right alias to use in the XML backup file.
	 * @param mdo
	 * @param backupAliasType
	 * @return
	 */
	private String getAlias(MigratableDatabaseObject mdo, BackupAliasType backupAliasType) {
		if (backupAliasType == BackupAliasType.TABLE_NAME) {
			return mdo.getTableMapping().getTableName();
		} else if (backupAliasType == BackupAliasType.MIGRATION_TYPE_NAME) {
			return mdo.getMigratableTableType().name();
		} else {
			throw new IllegalStateException("This should never happen. Invalid BackupAliasType: " + backupAliasType);
		}
	}

	/**
	 * Fire a create or update event for a given migration type.
	 * @param type
	 * @param databaseList - The Database objects that were created or updated.
	 */
	@Deprecated
	private <D extends DatabaseObject<D>> void fireCreateOrUpdateBatchEvent(MigrationType type, List<D> databaseList){
		if(this.migrationListeners != null){
			// Let each listener handle the event
			for(MigrationTypeListener listener: this.migrationListeners){
				listener.afterCreateOrUpdate(type, databaseList);
			}
		}
	}
	
	/**
	 * Fire a create or update event for a given migration type.
	 * @param type
	 * @param databaseList - The Database objects that were created or updated.
	 */
	private void fireCreateOrUpdateEvent(MigrationType type, List<DatabaseObject<?>> databaseList){
		if(this.migrationListeners != null){
			// Let each listener handle the event
			for(MigrationTypeListener listener: this.migrationListeners){
				listener.afterCreateOrUpdate(type, databaseList);
			}
		}
	}
	
	/**
	 * Fire a delete event for a given migration type.
	 * @param type
	 * @param toDelete
	 */
	private void fireDeleteBatchEvent(MigrationType type, List<Long> toDelete){
		if(this.migrationListeners != null){
			// Let each listener handle the event
			for(MigrationTypeListener listener: this.migrationListeners){
				listener.beforeDeleteBatch(type, toDelete);
			}
		}
	}

	@Override
	public List<MigrationType> getPrimaryMigrationTypes(UserInfo user) {
		validateUser(user);
		return migratableTableDao.getPrimaryMigrationTypes();
	}

	@Override
	public List<String> getPrimaryMigrationTypeNames(UserInfo user) {
		validateUser(user);
		List<MigrationType> types = migratableTableDao.getPrimaryMigrationTypes();
		List<String> typeNames = new LinkedList<String>();
		for (MigrationType t: types) {
			typeNames.add(t.name());
		}
		return typeNames;
	}

	@Override
	public List<MigrationType> getMigrationTypes(UserInfo user) {
		validateUser(user);
		List<MigrationType> l = new LinkedList<MigrationType>(Arrays.asList(MigrationType.values()));
		return l;
	}

	@Override
	public List<String> getMigrationTypeNames(UserInfo user) {
		validateUser(user);
		List<MigrationType> types = new LinkedList<MigrationType>(Arrays.asList(MigrationType.values()));
		List<String> typeNames = new LinkedList<String>();
		for (MigrationType t: types) {
			typeNames.add(t.name());
		}
		return typeNames;
	}

	@Override
	public List<MigrationType> getSecondaryTypes(MigrationType type) {
		// Get the primary type.
		MigratableDatabaseObject primary = migratableTableDao.getObjectForType(type);
		List<MigrationType> list = new LinkedList<MigrationType>();
		List<MigratableDatabaseObject> secondary = primary.getSecondaryTypes();
		if(secondary != null){
			for(MigratableDatabaseObject mdo: secondary){
				list.add(mdo.getMigratableTableType());
			}
		}
		return list;
	}

	@WriteTransaction
	@Override
	public void deleteAllData(UserInfo user) throws Exception {
		validateUser(user);
		// Delete all types in their reverse order
		for(int i=MigrationType.values().length-1; i>=0; i--){
			if (this.isMigrationTypeUsed(user, MigrationType.values()[i])) {
				deleteAllForType(user, MigrationType.values()[i]);
			}
		}
	}

	private void deleteAllForType(UserInfo user, MigrationType type) throws Exception {
		// First get all data for this type.
		RowMetadataResult result =  migratableTableDao.listRowMetadata(type, Long.MAX_VALUE, 0);
		List<RowMetadata> list = result.getList();
		if (list.size() > 0) {
			// Create the list of IDs to delete
			List<Long> toDelete = new LinkedList<Long>();
			for(RowMetadata row: list){
				toDelete.add(row.getId());
			}
			deleteObjectsById(user, type, toDelete);
		}
	}
	
	/**
	 * 
	 * @param user
	 * @param mt
	 * @return true if mt is a primary or secondary type, false otherwise
	 */
	public boolean isMigrationTypeUsed(UserInfo user, MigrationType mt) {
		for (MigrationType t: this.getPrimaryMigrationTypes(user)) {
			if (mt.equals(t)) {
				return true;
			}
			// Check secondary types
			List<MigrationType> stList = this.getSecondaryTypes(t);
			if (stList != null) {
				for (MigrationType st: stList) {
					if (mt.equals(st)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Injected via Spring
	 * @param migrationListeners
	 */
	public void setMigrationListeners(List<MigrationTypeListener> migrationListeners) {
		this.migrationListeners = migrationListeners;
	}

	@Override
	public MigrationRangeChecksum getChecksumForIdRange(UserInfo user, MigrationType type,
			String salt, long minId, long maxId) {
		validateUser(user);
		String checksum = migratableTableDao.getChecksumForIdRange(type, salt, minId, maxId);
		MigrationRangeChecksum mrc = new MigrationRangeChecksum();
		mrc.setType(type);
		mrc.setMinid(minId);
		mrc.setMaxid(maxId);
		mrc.setChecksum(checksum);
		return mrc;
	}
	
	@Override
	public MigrationTypeChecksum getChecksumForType(UserInfo user, MigrationType type) {
		if (stackStatusDao.getCurrentStatus() == StatusEnum.READ_WRITE) { 
			throw new RuntimeException("API getChecksumForType() cannot be called in Read/Write mode");
		}
		validateUser(user);
		String checksum = migratableTableDao.getChecksumForType(type);
		MigrationTypeChecksum mtc = new MigrationTypeChecksum();
		mtc.setType(type);
		mtc.setChecksum(checksum);
		return mtc;
	}

	@Override
	public MigrationTypeCount getMigrationTypeCount(UserInfo user, MigrationType type) {
		validateUser(user);
		MigrationTypeCount mtc = migratableTableDao.getMigrationTypeCount(type);
		return mtc;
	}
	
	@Override
	public MigrationTypeCounts processAsyncMigrationTypeCountsRequest(
			final UserInfo user, final AsyncMigrationTypeCountsRequest mReq) {
		validateUser(user);
		List<MigrationTypeCount> res = new LinkedList<MigrationTypeCount>();
			for (MigrationType t: mReq.getTypes()) {
				MigrationTypeCount mtc = migratableTableDao.getMigrationTypeCount(t);
				res.add(mtc);
			}
			MigrationTypeCounts mtRes = new MigrationTypeCounts();
			mtRes.setList(res);
			return mtRes;
	}

	@Override
	public MigrationTypeCount processAsyncMigrationTypeCountRequest(
			final UserInfo user, final AsyncMigrationTypeCountRequest mReq) {
		String t = mReq.getType();
		MigrationType mt = MigrationType.valueOf(t);
		return getMigrationTypeCount(user, mt);
	}

	@Override
	public MigrationTypeChecksum processAsyncMigrationTypeChecksumRequest(
			final UserInfo user, final AsyncMigrationTypeChecksumRequest mReq) {
		String t = mReq.getType();
		MigrationType mt = MigrationType.valueOf(t);
		return getChecksumForType(user, mt);
	}

	@Override
	public MigrationRangeChecksum processAsyncMigrationRangeChecksumRequest(
			final UserInfo user, final AsyncMigrationRangeChecksumRequest mReq) {
		String t = mReq.getType();
		MigrationType mt = MigrationType.valueOf(t);
		String salt = mReq.getSalt();
		long minId = mReq.getMinId();
		long maxId = mReq.getMaxId();
		return getChecksumForIdRange(user, mt, salt, minId, maxId);
	}

	@Override
	public RowMetadataResult processAsyncMigrationRowMetadataRequest(
			final UserInfo user, final AsyncMigrationRowMetadataRequest mReq) {
		String t = mReq.getType();
		MigrationType mt = MigrationType.valueOf(t);
		Long minId = mReq.getMinId();
		Long maxId = mReq.getMaxId();
		Long limit = mReq.getLimit();
		Long offset = mReq.getOffset();
		return getRowMetadataByRangeForType(user, mt, minId, maxId, limit, offset);
	}

	@Override
	public void validateForeignKeys() {
		// lookup the all restricted foreign keys.
		List<ForeignKeyInfo> nonRestrictedForeignKeys = this.migratableTableDao.listNonRestrictedForeignKeys();
		// lookup the primary group for each secondary table.
		Map<String, Set<String>> tableNameToPrimaryGroup = this.migratableTableDao.mapSecondaryTablesToPrimaryGroups();
 		for(ForeignKeyInfo nonRestrictedForeignKey: nonRestrictedForeignKeys) {
			String tableName = nonRestrictedForeignKey.getTableName().toUpperCase();
			String refrencedTalbeName = nonRestrictedForeignKey.getReferencedTableName().toUpperCase();
			Set<String> tablesInPrimaryGroup = tableNameToPrimaryGroup.get(tableName);
			if(tablesInPrimaryGroup != null) {
				/*
				 * This is a secondary table so it can only have non-restricted references to
				 * tables within its same primary group.
				 */
				if (!tablesInPrimaryGroup.contains(refrencedTalbeName)) {
					throw new IllegalStateException("See: PLFM-4729. Table: " + tableName + " cannot have a 'ON DELETE "
							+ nonRestrictedForeignKey.getDeleteRule() + "' foreign key refrence to table: "
							+ refrencedTalbeName
							+ " becuase the refrenced table does not belong to the same primary table.");
				}
			}
		}
		
	}

	/**
	 * Called after Spring creates the manager.
	 */
	public void initialize() {
		// validate all of the foreign keys.
		validateForeignKeys();
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.MigrationManager#backupRequest(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.migration.BackupTypeRequest)
	 */
	@Override
	public BackupTypeResponse backupRequest(UserInfo user, BackupTypeListRequest request) throws IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(request, "Request");
		ValidateArgument.required(request.getAliasType(), "request.aliasType");
		ValidateArgument.required(request.getMigrationType(), "request.migrationType");
		ValidateArgument.required(request.getBatchSize(), "requset.batchSize");
		ValidateArgument.required(request.getRowIdsToBackup(), "request.rowIdsToBackup");
		if(request.getRowIdsToBackup().isEmpty()) {
			throw new IllegalArgumentException("Request.rowIdsToBackup cannot be empty.");
		}
		validateUser(user);
		// Start the stream for the primary
		Iterable<MigratableDatabaseObject<?, ?>> dataStream = this.migratableTableDao
				.streamDatabaseObjects(request.getMigrationType(), request.getRowIdsToBackup(), request.getBatchSize());
		// Concatenate all secondary data streams to the main stream.
		for (MigrationType secondaryType : getSecondaryTypes(request.getMigrationType())) {
			Iterable<MigratableDatabaseObject<?, ?>> secondaryStream = this.migratableTableDao
					.streamDatabaseObjects(secondaryType, request.getRowIdsToBackup(), request.getBatchSize());
			dataStream = Iterables.concat(dataStream, secondaryStream);
		}
		// Create the backup and upload it to S3.
		return backupStreamToS3(request.getMigrationType(), dataStream, request.getAliasType(), request.getBatchSize());
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.MigrationManager#backupRequest(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest)
	 */
	@Override
	public BackupTypeResponse backupRequest(UserInfo user, BackupTypeRangeRequest request) throws IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(request, "Request");
		ValidateArgument.required(request.getAliasType(), "request.aliasType");
		ValidateArgument.required(request.getMigrationType(), "request.migrationType");
		ValidateArgument.required(request.getBatchSize(), "requset.batchSize");
		ValidateArgument.required(request.getMinimumId(), "request.minimumId");
		ValidateArgument.required(request.getMaximumId(), "request.maximumId");
		validateUser(user);
		// Start the stream for the primary
		Iterable<MigratableDatabaseObject<?, ?>> dataStream = this.migratableTableDao
				.streamDatabaseObjects(request.getMigrationType(), request.getMinimumId(), request.getMaximumId(), request.getBatchSize());
		// Concatenate all secondary data streams to the main stream.
		for (MigrationType secondaryType : getSecondaryTypes(request.getMigrationType())) {
			Iterable<MigratableDatabaseObject<?, ?>> secondaryStream = this.migratableTableDao
					.streamDatabaseObjects(secondaryType, request.getMinimumId(), request.getMaximumId(), request.getBatchSize());
			dataStream = Iterables.concat(dataStream, secondaryStream);
		}
		// Create the backup and upload it to S3.
		return backupStreamToS3(request.getMigrationType(), dataStream, request.getAliasType(), request.getBatchSize());
	}
	
	/**
	 * Stream the data to a temporary file and upload it to S3.
	 * 
	 * @param type
	 * @param dataStream
	 * @param aliasType
	 * @return
	 * @throws IOException 
	 */
	public BackupTypeResponse backupStreamToS3(MigrationType type, Iterable<MigratableDatabaseObject<?, ?>> dataStream, BackupAliasType aliasType, long batchSize) throws IOException {
		// Stream all of the data to a local temporary file.
		File temp = fileProvider.createTempFile("MigrationBackup", ".zip");
		FileOutputStream fos = null;
		try {
			fos = fileProvider.createFileOutputStream(temp);
			backupFileStream.writeBackupFile(fos, dataStream, aliasType, batchSize);
			fos.flush();
			fos.close();
			// Upload the file to S3
			String key = createNewBackupKey(stack, instance, type);
			s3Client.putObject(backupBucket, key, temp);
			BackupTypeResponse response = new BackupTypeResponse();
			response.setBackupFileKey(key);
			return response;
		}finally {
			// Unconditionally close the stream
			IOUtils.closeQuietly(fos);
			// Delete the temporary file if it exists.
			if(temp != null) {
				temp.delete();
			}
		}
	}
	
	/**
	 * Create new key to to store a backup file in S3.
	 * 
	 * @param type
	 * @return
	 */
	public static String createNewBackupKey(String stack, String instance, MigrationType type) {
		return String.format(BACKUP_KEY_TEMPLATE, stack, instance, type, UUID.randomUUID().toString());
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.MigrationManager#restoreRequest(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.migration.RestoreTypeRequest)
	 */
	@Override
	public RestoreTypeResponse restoreRequest(UserInfo user, RestoreTypeRequest request) throws IOException {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(request, "Request");
		ValidateArgument.required(request.getAliasType(), "request.aliasType");
		ValidateArgument.required(request.getMigrationType(), "request.migrationType");
		ValidateArgument.required(request.getBatchSize(), "requset.batchSize");
		ValidateArgument.required(request.getBackupFileKey(), "request.backupFileKey");
		validateUser(user);
		// Stream all of the data to a local temporary file.
		File temp = fileProvider.createTempFile("MigrationRestore", ".zip");
		FileInputStream fis = null;
		try {
			// download the file from S3
			GetObjectRequest getObjectRequest = new GetObjectRequest(backupBucket, request.getBackupFileKey());
			// the client will write the file data to the temp file.
			s3Client.getObject(getObjectRequest, temp);
			fis = fileProvider.createFileInputStream(temp);
			// Stream over the resulting file.
			RestoreTypeResponse response = restoreStream(fis, request.getMigrationType(), request.getAliasType(), request.getBatchSize());
			// delete the file from S3
			s3Client.deleteObject(backupBucket, request.getBackupFileKey());
			return response;
		}finally {
			// Unconditionally close the stream
			IOUtils.closeQuietly(fis);
			// Delete the temporary file if it exists.
			if(temp != null) {
				temp.delete();
			}
		}
	}

	/**
	 * Restore all of the data from the provided stream.
	 * 
	 * Note: Since this method is used to restore entire tables in a single call
	 * it should not be annotated with a transaction.  Instead, each batch added to the table
	 * will is done so in a separate transaction.
	 * 
	 * @param fis
	 * @param migrationType
	 * @param aliasType
	 * @return
	 */
	public RestoreTypeResponse restoreStream(InputStream input, MigrationType primaryType,
			BackupAliasType backupAliasType, long batchSize) {
		RestoreTypeResponse response = new RestoreTypeResponse();
		if(!this.migratableTableDao.isMigrationTypeRegistered(primaryType)) {
			// ignore types that are not registered.
			response.setRestoredRowCount(0L);
			return response;
		}
		long rowCount = 0;
		List<MigrationType> secondaryTypes = getSecondaryTypes(primaryType);
		// Start reading the stream.
		Iterable<MigratableDatabaseObject<?,?>> iterable = this.backupFileStream.readBackupFile(input, backupAliasType);
		MigrationType currentType = primaryType;
		List<DatabaseObject<?>> currentBatch = new LinkedList<>();
		for(MigratableDatabaseObject<?,?> rowToRestore: iterable) {
			MigrationType rowType = rowToRestore.getMigratableTableType();
			if(!this.migratableTableDao.isMigrationTypeRegistered(rowType)) {
				// ignore types that are not registered.
				continue;
			}
			
			// If over the batch size or a type switch push the current batch.
			if(currentBatch.size() >= batchSize || !rowType.equals(currentType)) {
				restoreBatch(currentType, primaryType, secondaryTypes, currentBatch);
				currentBatch = new LinkedList<>();
			}
			currentType = rowType;
			currentBatch.add(rowToRestore);
			rowCount++;
		}
		// push the remaining rows
		restoreBatch(currentType, primaryType, secondaryTypes, currentBatch);
		// prepare the response.
		response.setRestoredRowCount(rowCount);
		return response;
	}

	/**
	 * Restore a single batch of rows for the given type.
	 * @param currentType
	 * @param secondaryTypes
	 * @param currentBatch
	 */
	public void restoreBatch(MigrationType currentType, MigrationType primaryType, List<MigrationType> secondaryTypes,
			List<DatabaseObject<?>> currentBatch) {
		if(!currentBatch.isEmpty()) {
			// push the data to the database
			List<Long> createdOrUpdatedIds = this.migratableTableDao.createOrUpdate(currentType, currentBatch);
			// Let listeners know about the change
			fireCreateOrUpdateEvent(currentType, currentBatch);
			if(currentType.equals(primaryType)) {
				// delete all secondary data for this type
				for(MigrationType secondaryType: secondaryTypes) {
					this.deleteByIdAndFireEvent(secondaryType, createdOrUpdatedIds);
				}
			}
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public int deleteById(UserInfo user, MigrationType type, List<Long> idList) {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(type, "MigrationType");
		ValidateArgument.required(idList, "Ids");
		validateUser(user);
		if(idList.isEmpty()) {
			return 0;
		}
		int deleteCount = 0;
		// delete secondary rows first
		List<MigrationType> secondaryTypes = getSecondaryTypes(type);
		for(MigrationType secondary: secondaryTypes) {
			deleteCount += deleteByIdAndFireEvent(secondary, idList);
		}
		// delete the primary data.
		deleteCount += deleteByIdAndFireEvent(type, idList);
		return deleteCount;
	}

	/**
	 * Filter the bootstrap principals IDs from the provided ID list.
	 * 
	 * @param type
	 * @param idList
	 * @return
	 */
	static List<Long> filterBootstrapPrincipals(MigrationType type, List<Long> idList){
		if(PRINCIPAL_TYPES.contains(type)) {
			List<Long> filtered = new LinkedList<>();
			for(Long id: idList) {
				// skip bootstrap principal ids.
				if(!AuthorizationConstants.BOOTSTRAP_PRINCIPAL.isBootstrapPrincipalId(id)) {
					filtered.add(id);
				}
			}
			return filtered;
		}else {
			// nothing to filter
			return idList;
		}
	}
	
	/**
	 * Delete rows for the given type with the given ids and then notify 
	 * migration listeners.
	 * 
	 * @param type
	 * @param idList
	 * @return
	 */
	protected int deleteByIdAndFireEvent(MigrationType type, List<Long> idList) {
		// filter the bootstrap principals as needed.
		idList = filterBootstrapPrincipals(type, idList);
		this.fireDeleteBatchEvent(type, idList);
		int deleteCount = this.migratableTableDao.deleteById(type, idList);
		return deleteCount;
	}

}
