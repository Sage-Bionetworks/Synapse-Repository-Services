package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_COLUMN_MODEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COLUMN_MODEL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigrateFromXStreamToJSON;
import org.sagebionetworks.repo.model.dbo.migration.XStreamToJsonTranslator;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.table.ColumnModel;

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
		new FieldColumn("json", COL_CM_JSON),
	};
	
	private Long id;
	private String name;
	private String hash;
	private byte[] bytes;
	private String json;

	@Override
	public TableMapping<DBOColumnModel> getTableMapping() {
		return new TableMapping<DBOColumnModel>(){
			@Override
			public DBOColumnModel mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOColumnModel model = new DBOColumnModel();
				model.setId(rs.getLong(COL_CM_ID));
				model.setName(rs.getString(COL_CM_NAME));
				model.setHash(rs.getString(COL_CM_HASH));
				model.setJson(rs.getString(COL_CM_JSON));
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

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	@Override
	public MigratableTableTranslation<DBOColumnModel, DBOColumnModel> getTranslator() {
		return new MigrateFromXStreamToJSON<DBOColumnModel>(
				XStreamToJsonTranslator.builder().setXStream(ColumnModelUtils.X_STREAM).setFromName("bytes")
						.setToName("json").setDboType(getBackupClass()).setDtoType(ColumnModel.class).build());
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
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		result = prime * result + Objects.hash(hash, id, json, name);
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
		return Arrays.equals(bytes, other.bytes) && Objects.equals(hash, other.hash) && Objects.equals(id, other.id)
				&& Objects.equals(json, other.json) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "DBOColumnModel [id=" + id + ", name=" + name + ", hash=" + hash + ", bytes=" + Arrays.toString(bytes)
				+ ", json=" + json + "]";
	}

}
