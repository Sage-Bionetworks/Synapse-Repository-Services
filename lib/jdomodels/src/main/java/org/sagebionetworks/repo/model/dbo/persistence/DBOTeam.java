package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ICON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TEAM;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database Object for a Wiki Page.
 * 
 * @author John
 *
 */
public class DBOTeam implements MigratableDatabaseObject<DBOTeam, DBOTeam> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_TEAM_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_TEAM_ETAG).withIsEtag(true),
			new FieldColumn("icon", COL_TEAM_ICON).withHasFileHandleRef(true),
			new FieldColumn("properties", COL_TEAM_PROPERTIES) };

	private static final TableMapping<DBOTeam> TABLE_MAPPING = new TableMapping<DBOTeam>() {
		@Override
		public DBOTeam mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOTeam team = new DBOTeam();
			team.setId(rs.getLong(COL_TEAM_ID));
			team.setEtag(rs.getString(COL_TEAM_ETAG));
			team.setIcon(rs.getLong(COL_TEAM_ICON));
			
			if (rs.wasNull()) {
				team.setIcon(null);
			}

			java.sql.Blob blob = rs.getBlob(COL_TEAM_PROPERTIES);
			if (blob != null) {
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
	
	private static final MigratableTableTranslation<DBOTeam, DBOTeam> MIGRATION_MAPPER = new BasicMigratableTableTranslation<>();

	private Long id;
	private String etag;
	private Long icon;
	private byte[] properties;

	@Override
	public TableMapping<DBOTeam> getTableMapping() {
		return TABLE_MAPPING;
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

	public Long getIcon() {
		return icon;
	}
	
	public void setIcon(Long icon) {
		this.icon = icon;
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
		return MIGRATION_MAPPER;
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
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(properties);
		result = prime * result + Objects.hash(etag, icon, id);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOTeam other = (DBOTeam) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(icon, other.icon) && Objects.equals(id, other.id)
				&& Arrays.equals(properties, other.properties);
	}

	@Override
	public String toString() {
		return "DBOTeam [id=" + id + ", etag=" + etag + ", icon=" + icon + ", properties=" + Arrays.toString(properties) + "]";
	}

}
