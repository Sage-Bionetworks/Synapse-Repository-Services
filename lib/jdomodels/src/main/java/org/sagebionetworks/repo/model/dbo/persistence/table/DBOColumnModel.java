package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_BYTES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_COLUMN_MODEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COLUMN_MODEL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
/**
 * Database Object (DBO) for the Table Column Model.
 * @author John
 *
 */
public class DBOColumnModel implements MigratableDatabaseObject<DBOColumnModel, DBOColumnModel> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_CM_ID, true).withIsBackupId(true),
		new FieldColumn("name", COL_CM_NAME),
		new FieldColumn("hash", COL_CM_HASH),
		new FieldColumn("bytes", COL_CM_BYTES),
	};
	
	private Long id;
	private String name;
	private String hash;
	private byte[] bytes;

	@Override
	public TableMapping<DBOColumnModel> getTableMapping() {
		return new TableMapping<DBOColumnModel>(){
			@Override
			public DBOColumnModel mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOColumnModel model = new DBOColumnModel();
				model.setId(rs.getLong(COL_CM_ID));
				model.setName(rs.getString(COL_CM_NAME));
				model.setHash(rs.getString(COL_CM_HASH));
				java.sql.Blob blob = rs.getBlob(COL_CM_BYTES);
				if(blob != null){
					model.setBytes(blob.getBytes(1, (int) blob.length()));
				}
				return model;
			}

			@Override
			public String getTableName() {
				return TABLE_COLUMN_MODEL;
			}

			@Override
			public String getDDLFileName() {
				return DDL_COLUMN_MODEL;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOColumnModel> getDBOClass() {
				return DBOColumnModel.class;
			}
			
		};
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.COLUMN_MODEL;
	}

	@Override
	public MigratableTableTranslation<DBOColumnModel, DBOColumnModel> getTranslator() {
		return new MigratableTableTranslation<DBOColumnModel, DBOColumnModel>() {
			
			@Override
			public DBOColumnModel createDatabaseObjectFromBackup(DBOColumnModel backup) {
				return backup;
			}
			
			@Override
			public DBOColumnModel createBackupFromDatabaseObject(DBOColumnModel dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOColumnModel> getBackupClass() {
		return DBOColumnModel.class;
	}

	@Override
	public Class<? extends DBOColumnModel> getDatabaseObjectClass() {
		return DBOColumnModel.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		List<MigratableDatabaseObject> seconday = new LinkedList<MigratableDatabaseObject>();
		seconday.add(new DBOBoundColumn());
		seconday.add(new DBOBoundColumnOrdinal());
		return seconday;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
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
		DBOColumnModel other = (DBOColumnModel) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		return true;
	}

}
