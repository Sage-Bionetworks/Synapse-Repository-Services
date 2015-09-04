package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_FILE_ASSOC_FILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_FILE_ASSOC_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_FILE_ASSOCIATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_ID_SEQUENCE;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * This table tracks files handle associations with tables.
 *
 */
@Table(name = TABLE_TABLE_FILE_ASSOCIATION)
public class DBOTableFileAssociation implements MigratableDatabaseObject<DBOTableFileAssociation, DBOTableFileAssociation> {

	private static TableMapping<DBOTableFileAssociation> tableMapping = AutoTableMapping.create(DBOTableFileAssociation.class);

	@Field(name = COL_TABLE_FILE_ASSOC_TABLE_ID, nullable = false, primary=true, backupId = true)
	@ForeignKey (table = TABLE_TABLE_ID_SEQUENCE, field= COL_ID_SEQUENCE_TABLE_ID, cascadeDelete=true )
	private Long tableId;
	
	// There are currently 70 tests that fail if we set this s key.
	@Field(name = COL_TABLE_FILE_ASSOC_FILE_ID, nullable = false, primary=true)	
//	@ForeignKey (table = TABLE_FILES, field= COL_FILES_ID, cascadeDelete=true )
	private Long fileHandleId;

	
	
	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	@Override
	public TableMapping<DBOTableFileAssociation> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TABLE_FILE_ASSOCIATION;
	}

	@Override
	public MigratableTableTranslation<DBOTableFileAssociation, DBOTableFileAssociation> getTranslator() {
		return new MigratableTableTranslation<DBOTableFileAssociation, DBOTableFileAssociation>(){

			@Override
			public DBOTableFileAssociation createDatabaseObjectFromBackup(
					DBOTableFileAssociation backup) {
				return backup;
			}

			@Override
			public DBOTableFileAssociation createBackupFromDatabaseObject(
					DBOTableFileAssociation dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOTableFileAssociation> getBackupClass() {
		return DBOTableFileAssociation.class;
	}

	@Override
	public Class<? extends DBOTableFileAssociation> getDatabaseObjectClass() {
		return DBOTableFileAssociation.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOTableFileAssociation other = (DBOTableFileAssociation) obj;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (tableId == null) {
			if (other.tableId != null)
				return false;
		} else if (!tableId.equals(other.tableId))
			return false;
		return true;
	}
	
}
