package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOVerificationRetraction
		implements
		MigratableDatabaseObject<DBOVerificationRetraction, DBOVerificationRetraction> {

	@Field(name = VERIFICATION_RETRACTION_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = VERIFICATION_RETRACTION_CREATED_BY, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long createdBy;
	
	@Field(name = VERIFICATION_RETRACTION_CREATED_ON, backupId = false, primary = false, nullable = false)
	private Long createdOn;

	@Field(name = VERIFICATION_RETRACTION_SERIALIZED, backupId = false, primary = false, nullable = false, serialized="mediumblob")
	private byte[] serialized;

	private static TableMapping<DBOVerificationRetraction> TABLE_MAPPING = AutoTableMapping.create(DBOVerificationRetraction.class);

	@Override
	public TableMapping<DBOVerificationRetraction> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VERIFICATION_RETRACTION;
	}

	@Override
	public MigratableTableTranslation<DBOVerificationRetraction, DBOVerificationRetraction> getTranslator() {
		return new MigratableTableTranslation<DBOVerificationRetraction, DBOVerificationRetraction>() {

			@Override
			public DBOVerificationRetraction createDatabaseObjectFromBackup(
					DBOVerificationRetraction backup) {
				return backup;
			}

			@Override
			public DBOVerificationRetraction createBackupFromDatabaseObject(
					DBOVerificationRetraction dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBOVerificationRetraction> getBackupClass() {
		return DBOVerificationRetraction.class;
	}

	@Override
	public Class<? extends DBOVerificationRetraction> getDatabaseObjectClass() {
		return DBOVerificationRetraction.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOVerificationApproval());
		list.add(new DBOVerificationRetraction());
		return list;
	}
}
