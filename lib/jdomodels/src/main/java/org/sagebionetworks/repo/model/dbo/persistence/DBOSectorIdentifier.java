package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_SECRET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OAUTH_SECTOR_IDENTIFIER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_SECTOR_IDENTIFIER;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Contains information about comments posted to arbitrary objects
 */
public class DBOSectorIdentifier implements MigratableDatabaseObject<DBOSectorIdentifier, DBOSectorIdentifier> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("id", COL_OAUTH_SECTOR_IDENTIFIER_ID, true).withIsBackupId(true), 
		new FieldColumn("uri", COL_OAUTH_SECTOR_IDENTIFIER_URI), 
		new FieldColumn("secret", COL_OAUTH_SECTOR_IDENTIFIER_SECRET),
		new FieldColumn("createdBy", COL_OAUTH_SECTOR_IDENTIFIER_CREATED_BY),
		new FieldColumn("createdOn", COL_OAUTH_SECTOR_IDENTIFIER_CREATED_ON),
	};
	
	private Long id;
	private String uri;
	private String secret;
	private Long createdBy;
	private Long createdOn;
	
	@Override
	public TableMapping<DBOSectorIdentifier> getTableMapping() {
		return new TableMapping<DBOSectorIdentifier>() {

			@Override
			public DBOSectorIdentifier mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOSectorIdentifier dbo = new DBOSectorIdentifier();
				dbo.setId(rs.getLong(COL_OAUTH_SECTOR_IDENTIFIER_ID));
				dbo.setUri(rs.getString(COL_OAUTH_SECTOR_IDENTIFIER_URI));
				dbo.setSecret(rs.getString(COL_OAUTH_SECTOR_IDENTIFIER_SECRET));
				dbo.setCreatedOn(rs.getLong(COL_OAUTH_SECTOR_IDENTIFIER_CREATED_ON));
				dbo.setCreatedBy(rs.getLong(COL_OAUTH_SECTOR_IDENTIFIER_CREATED_BY));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_OAUTH_SECTOR_IDENTIFIER;
			}

			@Override
			public String getDDLFileName() {
				return DDL_OAUTH_SECTOR_IDENTIFIER;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSectorIdentifier> getDBOClass() {
				return DBOSectorIdentifier.class;
			}
		};
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OAUTH_SECTOR_IDENTIFIER;
	}


	static final MigratableTableTranslation<DBOSectorIdentifier, DBOSectorIdentifier> TRANSLATOR = 
			new BasicMigratableTableTranslation<DBOSectorIdentifier>() {};
	
	@Override
	public MigratableTableTranslation<DBOSectorIdentifier, DBOSectorIdentifier> getTranslator() {
		return TRANSLATOR;
	}


	@Override
	public Class<? extends DBOSectorIdentifier> getBackupClass() {
		return DBOSectorIdentifier.class;
	}


	@Override
	public Class<? extends DBOSectorIdentifier> getDatabaseObjectClass() {
		return DBOSectorIdentifier.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public String getUri() {
		return uri;
	}


	public void setUri(String uri) {
		this.uri = uri;
	}

	public Long getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}


	public Long getCreatedOn() {
		return createdOn;
	}


	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}




	public String getSecret() {
		return secret;
	}


	public void setSecret(String secret) {
		this.secret = secret;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((secret == null) ? 0 : secret.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		DBOSectorIdentifier other = (DBOSectorIdentifier) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (secret == null) {
			if (other.secret != null)
				return false;
		} else if (!secret.equals(other.secret))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DBOSectorIdentifier [id=" + id + ", uri=" + uri + ", secret=" + secret + ", createdBy=" + createdBy
				+ ", createdOn=" + createdOn + "]";
	}



}
