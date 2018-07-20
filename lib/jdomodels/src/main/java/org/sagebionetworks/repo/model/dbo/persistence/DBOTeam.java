package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TEAM;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database Object for a Wiki Page.
 * @author John
 *
 */
public class DBOTeam implements MigratableDatabaseObject<DBOTeam, DBOTeam> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_TEAM_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_TEAM_ETAG).withIsEtag(true),
		new FieldColumn("properties", COL_TEAM_PROPERTIES)
	};
	
	private Long id;
	private String etag;
	private byte[] properties;

	@Override
	public TableMapping<DBOTeam> getTableMapping() {
		return new TableMapping<DBOTeam>(){
			@Override
			public DBOTeam mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTeam team = new DBOTeam();
				team.setId(rs.getLong(COL_TEAM_ID));
				team.setEtag(rs.getString(COL_TEAM_ETAG));

				java.sql.Blob blob = rs.getBlob(COL_TEAM_PROPERTIES);
				if(blob != null){
					team.setProperties(blob.getBytes(1, (int) blob.length()));
				}
				return team;
			}

			@Override
			public String getTableName() {
				return TABLE_TEAM;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_TEAM;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTeam> getDBOClass() {
				return DBOTeam.class;
			}
			
		};
	}



	public Long getId() {
		return id;
	}



	public void setId(Long id) {
		this.id = id;
	}



	public String getEtag() {
		return etag;
	}



	public void setEtag(String etag) {
		this.etag = etag;
	}



	public byte[] getProperties() {
		return properties;
	}



	public void setProperties(byte[] properties) {
		this.properties = properties;
	}



	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TEAM;
	}

	@Override
	public MigratableTableTranslation<DBOTeam, DBOTeam> getTranslator() {
		return new BasicMigratableTableTranslation<DBOTeam>();
	}

	@Override
	public Class<? extends DBOTeam> getBackupClass() {
		return DBOTeam.class;
	}

	@Override
	public Class<? extends DBOTeam> getDatabaseObjectClass() {
		return DBOTeam.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + Arrays.hashCode(properties);
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
		DBOTeam other = (DBOTeam) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!Arrays.equals(properties, other.properties))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return "DBOTeam [id=" + id + ", etag=" + etag + ", properties="
				+ Arrays.toString(properties) + "]";
	}
}
