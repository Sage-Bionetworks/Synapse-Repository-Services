package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;
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

@Table(name = TABLE_VERIFICATION_APPROVAL)
public class DBOVerificationApproval
		implements
		MigratableDatabaseObject<DBOVerificationApproval, DBOVerificationApproval> {

	@Field(name = VERIFICATION_APPROVAL_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = VERIFICATION_APPROVAL_CREATED_BY, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long createdBy;
	
	@Field(name = VERIFICATION_APPROVAL_CREATED_ON, backupId = false, primary = false, nullable = false)
	private Long createdOn;

	private static TableMapping<DBOVerificationApproval> TABLE_MAPPING = AutoTableMapping.create(DBOVerificationApproval.class);

	@Override
	public TableMapping<DBOVerificationApproval> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VERIFICATION_APPROVAL;
	}

	@Override
	public MigratableTableTranslation<DBOVerificationApproval, DBOVerificationApproval> getTranslator() {
		return new MigratableTableTranslation<DBOVerificationApproval, DBOVerificationApproval>() {

			@Override
			public DBOVerificationApproval createDatabaseObjectFromBackup(
					DBOVerificationApproval backup) {
				return backup;
			}

			@Override
			public DBOVerificationApproval createBackupFromDatabaseObject(
					DBOVerificationApproval dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBOVerificationApproval> getBackupClass() {
		return DBOVerificationApproval.class;
	}

	@Override
	public Class<? extends DBOVerificationApproval> getDatabaseObjectClass() {
		return DBOVerificationApproval.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
}
