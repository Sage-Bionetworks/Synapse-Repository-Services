package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = TABLE_VERIFICATION_STATE)
public class DBOVerificationState implements
		MigratableDatabaseObject<DBOVerificationState, DBOVerificationState> {

	@Field(name = VERIFICATION_STATE_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = VERIFICATION_STATE_CREATED_BY, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long createdBy;
	
	@Field(name = VERIFICATION_STATE_CREATED_ON, backupId = false, primary = false, nullable = false)
	private Long createdOn;

	@Field(name = VERIFICATION_STATE_REASON, backupId = false, primary = false, nullable = true)
	private String reason;

	private static TableMapping<DBOVerificationState> TABLE_MAPPING = AutoTableMapping.create(DBOVerificationState.class);

	@Override
	public TableMapping<DBOVerificationState> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VERIFICATION_APPROVAL;
	}

	@Override
	public MigratableTableTranslation<DBOVerificationState, DBOVerificationState> getTranslator() {
		return new MigratableTableTranslation<DBOVerificationState, DBOVerificationState>() {

			@Override
			public DBOVerificationState createDatabaseObjectFromBackup(
					DBOVerificationState backup) {
				return backup;
			}

			@Override
			public DBOVerificationState createBackupFromDatabaseObject(
					DBOVerificationState dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBOVerificationState> getBackupClass() {
		return DBOVerificationState.class;
	}

	@Override
	public Class<? extends DBOVerificationState> getDatabaseObjectClass() {
		return DBOVerificationState.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
}
