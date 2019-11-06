package org.sagebionetworks.repo.manager.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeResponse;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.BatchChecksumResponse;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeRequest;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeResponse;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;

/**
 * Abstraction for the V2 migration manager.
 * 
 * @author John
 *
 */
public interface MigrationManager {

	/**
	 * The total number of rows in the table.
	 * 
	 * @return
	 */
	public long getCount(UserInfo user, MigrationType type);

	/**
	 * The max(id) of the table
	 * 
	 * @return
	 */
	public long getMaxId(UserInfo user, MigrationType type);

	/**
	 * The list of primary migration types represents types that either stand-alone
	 * or are the owner's of other types. Migration is driven off this list as
	 * secondary types are migrated with their primary owners.
	 * 
	 * @return
	 */
	public List<MigrationType> getPrimaryMigrationTypes(UserInfo user);

	/**
	 * The list of primary migration type names
	 * 
	 * @param user
	 * @return
	 */
	public List<String> getPrimaryMigrationTypeNames(UserInfo user);

	/**
	 * The list of all migration types
	 * 
	 * @param user
	 * @return
	 */
	public List<MigrationType> getMigrationTypes(UserInfo user);

	/**
	 * The list of all migration type names
	 * 
	 * @param user
	 * @return
	 */
	public List<String> getMigrationTypeNames(UserInfo user);

	/**
	 * If this object is the 'owner' of other object, then it is a primary type. All
	 * secondary types should be returned in their migration order. For example, if
	 * A owns B and B owns C (A->B->C) then A is the primary, and both B and C are
	 * secondary. For this case, return B followed by C. Both B and C must have a
	 * backup ID column that is a foreign key to the backup ID of A, as the IDs of A
	 * will drive the migration of B and C.
	 * 
	 * @return
	 */
	public List<MigrationType> getSecondaryTypes(MigrationType type);

	/**
	 * Returns true if mt is a primary or secondary migration type, false otherwise
	 * 
	 * @param user
	 * @param mt
	 * @return
	 */
	public boolean isMigrationTypeUsed(UserInfo user, MigrationType mt);

	public long getMinId(UserInfo user, MigrationType type);

	public MigrationRangeChecksum getChecksumForIdRange(UserInfo user, MigrationType type, String salt, long minId,
			long maxId);

	public MigrationTypeChecksum getChecksumForType(UserInfo user, MigrationType type);

	public MigrationTypeCount getMigrationTypeCount(UserInfo user, MigrationType type);

	public MigrationTypeCount processAsyncMigrationTypeCountRequest(final UserInfo user,
			final AsyncMigrationTypeCountRequest mReq);

	public MigrationTypeCounts processAsyncMigrationTypeCountsRequest(final UserInfo user,
			final AsyncMigrationTypeCountsRequest mReq);

	public MigrationTypeChecksum processAsyncMigrationTypeChecksumRequest(final UserInfo user,
			final AsyncMigrationTypeChecksumRequest mReq);

	public MigrationRangeChecksum processAsyncMigrationRangeChecksumRequest(final UserInfo user,
			final AsyncMigrationRangeChecksumRequest mReq);

	/**
	 * <p>
	 * See: PLFM-4729
	 * </p>
	 * <p>
	 * In order to correctly migrate a change to a secondary table, the etag of the
	 * corresponding row in the primary table must also be updated. If a foreign key
	 * constraint triggers the modification of a row in a secondary table by
	 * deleting the row ('ON DELETE CASCADE'), or setting a value to null ('ON
	 * DELETE SET NULL') without also updating the corresponding row in the primary
	 * table, then the change to the secondary table will not migrate.
	 * </p>
	 * <p>
	 * Therefore, we limit foreign key constraint on secondary tables to
	 * 'RESTRICTED' when the referenced table does not belong to the same primary
	 * table.
	 * </p>
	 * 
	 * @param keyInfoList
	 * @param tableNameToPrimaryGroup Mapping of the name of each secondary table to
	 *                                the set of table names that belong to the same
	 *                                primary table. Note: The primary table name is
	 *                                included in the set.
	 */
	public void validateForeignKeys();

	/**
	 * 
	 * @param user
	 * @param request
	 * @return
	 * @throws IOException
	 */
	public BackupTypeResponse backupRequest(UserInfo user, BackupTypeRangeRequest request) throws IOException;

	/**
	 * Restore the data from the provided migration backup file.
	 * 
	 * @param user
	 * @param req
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public RestoreTypeResponse restoreRequest(UserInfo user, RestoreTypeRequest req) throws IOException;

	/**
	 * Is the given type a bootstrap type?
	 * 
	 * @param type
	 * @return
	 */
	public boolean isBootstrapType(MigrationType type);

	/**
	 * A migration backup file will contain data for primary rows and all secondary
	 * rows associated with the primary rows. These backup files must be restored to
	 * the destination server within a single transaction to ensure all secondary
	 * rows are consistent with their primary data (see: PLFM-4832).
	 * 
	 * The total amount of time required to backup and restore all data is directly
	 * proportional to the number of rows contained in the backup. For example,
	 * reducing the number of rows by half can double the amount of time required to
	 * restore all data. On the other hand, if there are too many rows in a file,
	 * then the restore transaction will timeout while attempting to gather all of
	 * the required row locks (see: PLFM-4847).
	 * 
	 * The ideal backup file will include the largest number of rows while remaining
	 * under the transaction limits.
	 * 
	 * Each backup file is defined by a range of primary IDs. For types with no
	 * secondary data and contiguous IDs, the number of rows in the backup file will
	 * match the provided ID range. However, for types where there are large gaps in
	 * the ID (cases where a shared ID generator was used), a large ID range might
	 * only contain a few rows. For types, with many rows of secondary data, a small
	 * ID range might include a large numbers of rows.
	 * 
	 * This method is used to calculate a set of ranges, each of which will contain
	 * the optimal number of rows regardless of cardinality or ID gaps.
	 * 
	 * 
	 * @param user
	 * @param request
	 * @return
	 */
	public CalculateOptimalRangeResponse calculateOptimalRanges(UserInfo user, CalculateOptimalRangeRequest request);

	/**
	 * Calculate n number of checksums for the provided ID range based on the
	 * provided batch size.
	 * 
	 * @param user
	 * @param req
	 * @return
	 */
	public BatchChecksumResponse calculateBatchChecksums(UserInfo user, BatchChecksumRequest req);

	/**
	 * Restore from a stream.
	 * @param input
	 * @param primaryType
	 * @param backupAliasType
	 * @param batchSize
	 * @return
	 */
	RestoreTypeResponse restoreStream(InputStream input, MigrationType primaryType, BackupAliasType backupAliasType,
			long batchSize);

}
