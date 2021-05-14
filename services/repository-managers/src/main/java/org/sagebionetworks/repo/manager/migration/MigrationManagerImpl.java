package org.sagebionetworks.repo.manager.migration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.ForeignKeyInfo;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupManifest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeResponse;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.BatchChecksumResponse;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeRequest;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeResponse;
import org.sagebionetworks.repo.model.migration.IdRange;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.RangeChecksum;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;
import org.sagebionetworks.repo.model.migration.TypeData;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.collect.Iterables;


/**
 * Basic implementation of migration manager.
 * 
 * @author John
 *
 */
public class MigrationManagerImpl implements MigrationManager {
	
	public static final String BACKUP_KEY_TEMPLATE = "%1$s-%2$s-%3$s-%4$s.zip";
	public static final String MANIFEST_KEY_TEMPLATE = "%1$s/manifest.json";
	public static String backupBucket = StackConfigurationSingleton.singleton().getSharedS3BackupBucket();
	public static String stack = StackConfigurationSingleton.singleton().getStack();
	public static String instance = StackConfigurationSingleton.singleton().getStackInstance();
	
	@Autowired
	private MigratableTableDAO migratableTableDao;
	@Autowired
	private StackStatusDao stackStatusDao;
	@Autowired
	private BackupFileStream backupFileStream;
	@Autowired
	private SynapseS3Client s3Client;
	@Autowired
	private FileProvider fileProvider;

	/**
	 * The list of migration listeners
	 */
	List<MigrationTypeListener> migrationListeners;
	
	/**
	 * Migration types for principals.
	 */
	static Set<MigrationType> PRINCIPAL_TYPES;

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
	
	/**
	 * <p>
	 * This method will provide basic statistics about the table associated with
	 * each of the provided migration types. These statistics include, counts,
	 * minimum ID, and maximum ID, all of which are used by the migration process.
	 * <p>
	 * The changes table captures generic change information about most objects. The
	 * migration of changes from production to staging drives event notification to
	 * secondary index building workers such as entity search and user search. If a
	 * change migrates before the data associated with the change, the workers on
	 * staging will be notified of the change but all data associated with the
	 * change will either be missing or stale. This is what caused PLFM-6638.
	 * <p>
	 * Our solution to this problem has been to gather a high-water-mark for each
	 * table at the beginning of the migration process. So all additions/updates
	 * that occur during the migration process will not be migrated. The returned
	 * maxid from this method is used as the high-water-mark.
	 * <p>
	 * Hoverer, it still takes time to gather the statistics for each table. If we
	 * do not get the high-water-mark for the changes table first, then a race
	 * condition is possible. Consider the following events:
	 * <ol>
	 * <li>Migration gathers the maxid for PRINCIPAL</li>
	 * <li>A new users creates an account, thereby adding data to PRINCIPAL</li>
	 * <li>Migration gathers the maxid for the CHANGES</li>
	 * </ol>
	 * This will result in the change event from the new users to migrate, but the
	 * data associated with the new user will not migrate. As a result, the worker
	 * on staging encounters a NotFoundExcption when attempting to build index
	 * information for the new user.
	 * <p>
	 * To avoid this last race condition, we gather the statics for the changes
	 * table before all other tables.
	 * 
	 */
	@Override
	public MigrationTypeCounts processAsyncMigrationTypeCountsRequest(final UserInfo user,
			final AsyncMigrationTypeCountsRequest request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getTypes(), "request.types");
		validateUser(user);

		Map<MigrationType, MigrationTypeCount> typeToResultMap = new HashMap<>(request.getTypes().size());
		// Gather changes first if requested.
		for (MigrationType type : request.getTypes()) {
			if (MigrationType.CHANGE.equals(type)) {
				typeToResultMap.put(MigrationType.CHANGE,
						migratableTableDao.getMigrationTypeCount(MigrationType.CHANGE));
				break;
			}
		}
		// Gather all non-changes
		for (MigrationType type : request.getTypes()) {
			if (!MigrationType.CHANGE.equals(type)) {
				typeToResultMap.put(type, migratableTableDao.getMigrationTypeCount(type));
			}
		}
		// return the results in the requested order
		List<MigrationTypeCount> results = new ArrayList<>(request.getTypes().size());
		for (MigrationType type : request.getTypes()) {
			results.add(typeToResultMap.get(type));
		}
		return new MigrationTypeCounts().setList(results);
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
			final UserInfo user, final AsyncMigrationTypeChecksumRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getMigrationType(), "request.migrationType");
		return getChecksumForType(user, request.getMigrationType());
	}

	@Override
	public MigrationRangeChecksum processAsyncMigrationRangeChecksumRequest(
			final UserInfo user, final AsyncMigrationRangeChecksumRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getMigrationType(), "request.migrationType");
		ValidateArgument.required(request.getSalt(), "request.salt");
		ValidateArgument.required(request.getMinId(), "request.minId");
		ValidateArgument.required(request.getMaxId(), "request.maxId");
		return getChecksumForIdRange(user, request.getMigrationType(), request.getSalt(), request.getMinId(), request.getMaxId());
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
		
		PRINCIPAL_TYPES = new HashSet<>();
		PRINCIPAL_TYPES.add(MigrationType.PRINCIPAL);
		PRINCIPAL_TYPES.addAll(getSecondaryTypes(MigrationType.PRINCIPAL));
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
		List<MigrationType> secondaryTypes = getSecondaryTypes(request.getMigrationType());
		BackupManifest manifest = new BackupManifest().setAliasType(request.getAliasType())
				.setBatchSize(request.getBatchSize()).setMaximumId(request.getMaximumId())
				.setMinimumId(request.getMinimumId())
				.setPrimaryType(migratableTableDao.getTypeData(request.getMigrationType()))
				.setSecondaryTypes(secondaryTypes.stream().map(t->migratableTableDao.getTypeData(t)).collect(Collectors.toList()));
		String backupKey = createNewBackupKey(stack, instance, request.getMigrationType());
		String manifestKey = createManifestKey(backupKey);
		putManifestToS3(manifestKey, manifest);
		// Start the stream for the primary
		Iterable<MigratableDatabaseObject<?, ?>> dataStream = this.migratableTableDao.streamDatabaseObjects(
				request.getMigrationType(), request.getMinimumId(), request.getMaximumId(), request.getBatchSize());
		// Concatenate all secondary data streams to the main stream.
		for (MigrationType secondaryType : secondaryTypes) {
			Iterable<MigratableDatabaseObject<?, ?>> secondaryStream = this.migratableTableDao.streamDatabaseObjects(
					secondaryType, request.getMinimumId(), request.getMaximumId(), request.getBatchSize());
			dataStream = Iterables.concat(dataStream, secondaryStream);
		}
		// Create the backup and upload it to S3.
		return backupStreamToS3(request.getMigrationType(), dataStream, request.getAliasType(), request.getBatchSize(),
				backupKey);
	}
	
	/**
	 * Put the given manifest to S3.
	 * @param manifestKey
	 * @param manifest
	 */
	void putManifestToS3(String manifestKey, BackupManifest manifest) {
		try {
			byte[] bytes = EntityFactory.createJSONStringForEntity(manifest).getBytes(StandardCharsets.UTF_8);
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(bytes.length);
			metadata.setContentDisposition("application/json name=manifest.json");
			metadata.setContentType("application/json");
			metadata.setContentEncoding("UTF-8");
			s3Client.putObject(new PutObjectRequest(backupBucket, manifestKey, new ByteArrayInputStream(bytes), metadata));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
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
	public BackupTypeResponse backupStreamToS3(MigrationType type, Iterable<MigratableDatabaseObject<?, ?>> dataStream, BackupAliasType aliasType, long batchSize, String key) throws IOException {
		// Stream all of the data to a local temporary file.
		File temp = fileProvider.createTempFile("MigrationBackup", ".zip");
		OutputStream fos = null;
		try {
			fos = fileProvider.createFileOutputStream(temp);
			backupFileStream.writeBackupFile(fos, dataStream, aliasType, batchSize);
			fos.flush();
			fos.close();
			// Upload the file to S3
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
	
	/**
	 * Create the manifest key for the given base key.
	 * @param baseKey
	 * @return
	 */
	public static String createManifestKey(String baseKey) {
		return String.format(MANIFEST_KEY_TEMPLATE, baseKey);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.MigrationManager#restoreRequest(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.migration.RestoreTypeRequest)
	 */
	@WriteTransaction // required see PLFM-4832
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
		try {
			// the client will write the file data to the temp file.
			s3Client.getObject(new GetObjectRequest(backupBucket, request.getBackupFileKey()), temp);
			try(InputStream fis = fileProvider.createFileInputStream(temp)){
				// Stream over the resulting file.
				return restoreStream(fis, getManifest(request));
			}
		}finally {
			// Delete the temporary file if it exists.
			if(temp != null) {
				temp.delete();
			}
		}
	}
	

	/**
	 * Get the manifest associated with the given request.
	 * 
	 * @param request
	 * @return
	 */
	public BackupManifest getManifest(RestoreTypeRequest request) {
		String manifestKey = createManifestKey(request.getBackupFileKey());
		if(s3Client.doesObjectExist(backupBucket, manifestKey)) {
			// all new backup requests will include a manifest in S3.
			try(InputStream in = s3Client.getObject(new GetObjectRequest(backupBucket, manifestKey)).getObjectContent()){
				String json = IOUtils.toString(in, StandardCharsets.UTF_8.name());
				return EntityFactory.createEntityFromJSONString(json, BackupManifest.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			/*
			 * Old backup requests do not include a manifest in S3. For such cases we must
			 * build a manifest using a combination of data from the restore request plus
			 * secondary data from this server. Note: This path will be removed when the code
			 * to generate manifests in S3 is deployed to production.
			 */
			return new BackupManifest().setAliasType(request.getAliasType()).setBatchSize(request.getBatchSize())
					.setMaximumId(request.getMaximumRowId()).setMinimumId(request.getMinimumRowId())
					.setPrimaryType(migratableTableDao.getTypeData(request.getMigrationType()))
					.setSecondaryTypes(getSecondaryTypes(request.getMigrationType()).stream()
							.map(t -> migratableTableDao.getTypeData(t)).collect(Collectors.toList()));
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
	@Override
	public RestoreTypeResponse restoreStream(InputStream input, BackupManifest manifest) {
		RestoreTypeResponse response = new RestoreTypeResponse();
		if(!this.migratableTableDao.isMigrationTypeRegistered(manifest.getPrimaryType().getMigrationType())) {
			// ignore types that are not registered.
			response.setRestoredRowCount(0L);
			return response;
		}
		
		// delete all primary and secondary data for the ranges defined in the manifest.
		deleteByRange(manifest);
		
		long rowCount = 0;
		// Start reading the stream.
		Iterable<MigratableDatabaseObject<?,?>> iterable = this.backupFileStream.readBackupFile(input, manifest.getAliasType());
		MigrationType currentType = manifest.getPrimaryType().getMigrationType();
		List<DatabaseObject<?>> currentBatch = new LinkedList<>();
		for(MigratableDatabaseObject<?,?> rowToRestore: iterable) {
			MigrationType rowType = rowToRestore.getMigratableTableType();
			if(!this.migratableTableDao.isMigrationTypeRegistered(rowType)) {
				// ignore types that are not registered.
				continue;
			}
			
			// If over the batch size or a type switch push the current batch.
			if(currentBatch.size() >= manifest.getBatchSize() || !rowType.equals(currentType)) {
				restoreBatch(currentType, currentBatch);
				currentBatch = new LinkedList<>();
			}
			currentType = rowType;
			currentBatch.add(rowToRestore);
			rowCount++;
		}
		// push the remaining rows
		restoreBatch(currentType, currentBatch);
		// prepare the response.
		response.setRestoredRowCount(rowCount);
		return response;
	}

	/**
	 * Restore a single batch of rows for the given type.
	 * @param currentType
	 * @param currentBatch
	 */
	void restoreBatch(MigrationType currentType,
			List<DatabaseObject<?>> currentBatch) {
		if(!currentBatch.isEmpty()) {
			// push the data to the database
			this.migratableTableDao.createOrUpdate(currentType, currentBatch);
			// Let listeners know about the change
			fireCreateOrUpdateEvent(currentType, currentBatch);
		}
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

	@Override
	public boolean isBootstrapType(MigrationType type) {
		return PRINCIPAL_TYPES.contains(type);
	}
	
	/**
	 * Delete all primary and secondary data for the given ID range.
	 * @param manifest manifest defines both the ranges and secondary types.
	 */
	void deleteByRange(BackupManifest manifest) {
		if(manifest.getMaximumId() != null && manifest.getMinimumId() != null) {
			long minimumId = manifest.getMinimumId();
			long maximumId = manifest.getMaximumId();
			if(manifest.getSecondaryTypes() != null) {
				for(TypeData secondaryType: manifest.getSecondaryTypes()) {
					if(migratableTableDao.isMigrationTypeRegistered(secondaryType.getMigrationType())){
						this.migratableTableDao.deleteByRange(secondaryType, minimumId, maximumId);
					}
				}
			}
			if(migratableTableDao.isMigrationTypeRegistered(manifest.getPrimaryType().getMigrationType())){
				this.migratableTableDao.deleteByRange(manifest.getPrimaryType(), minimumId, maximumId);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.MigrationManager#calculateRanges(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.migration.CalculateBackupRangeRequest)
	 */
	@Override
	public CalculateOptimalRangeResponse calculateOptimalRanges(UserInfo user, CalculateOptimalRangeRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getMigrationType(), "request.migrationType");
		ValidateArgument.required(request.getMinimumId(), "request.minimumId");
		ValidateArgument.required(request.getMaximumId(), "request.maximumId");
		ValidateArgument.required(request.getOptimalRowsPerRange(), "request.optimalRowsPerRange");
		ValidateArgument.required(user, "User");
		validateUser(user);
		List<IdRange> ranges = migratableTableDao.calculateRangesForType(request.getMigrationType(),
				request.getMinimumId(), request.getMaximumId(), request.getOptimalRowsPerRange());
		CalculateOptimalRangeResponse response = new CalculateOptimalRangeResponse();
		response.setRanges(ranges);
		response.setMigrationType(request.getMigrationType());
		return response;
	}

	@Override
	public BatchChecksumResponse calculateBatchChecksums(UserInfo user, BatchChecksumRequest request) {
		ValidateArgument.required(user, "User");
		validateUser(user);
		List<RangeChecksum> batches = migratableTableDao.calculateBatchChecksums(request);
		BatchChecksumResponse response = new BatchChecksumResponse();
		response.setCheksums(batches);
		response.setMigrationType(request.getMigrationType());
		return response;
	}

}
