package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHENTICATED_ON;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAuthenticatedOn implements MigratableDatabaseObject<DBOAuthenticatedOn, DBOAuthenticatedOn> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("principalId", COL_AUTHENTICATED_ON_PRINCIPAL_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_AUTHENTICATED_ON_ETAG).withIsEtag(true),
			new FieldColumn("authenticatedOn", COL_AUTHENTICATED_ON_AUTHENTICATED_ON)
	};
	
	private Long principalId;
	private String etag;
	private Date authenticatedOn;

	@Override
	public TableMapping<DBOAuthenticatedOn> getTableMapping() {
		return new TableMapping<DBOAuthenticatedOn>() {

			@Override
			public DBOAuthenticatedOn mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAuthenticatedOn dbo = new DBOAuthenticatedOn();
				dbo.setPrincipalId(rs.getLong(COL_AUTHENTICATED_ON_PRINCIPAL_ID));
				dbo.setEtag(rs.getString(COL_AUTHENTICATED_ON_ETAG));
				Timestamp ts = rs.getTimestamp(COL_AUTHENTICATED_ON_AUTHENTICATED_ON);
				dbo.setAuthenticatedOn(ts==null ? null : new Date(ts.getTime()));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_AUTHENTICATED_ON;
			}

			@Override
			public String getDDLFileName() {
				return DDL_AUTHENTICATED_ON;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAuthenticatedOn> getDBOClass() {
				return DBOAuthenticatedOn.class;
			}
		};
	}
	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Long getPrincipalId() {
		return principalId;
	}
	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}


	public Date getAuthenticatedOn() {
		return authenticatedOn;
	}

	public void setAuthenticatedOn(Date authenticatedOn) {
		this.authenticatedOn = authenticatedOn;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.AUTHENTICATED_ON;
	}

	@Override
	public MigratableTableTranslation<DBOAuthenticatedOn, DBOAuthenticatedOn> getTranslator() {
		return new BasicMigratableTableTranslation<DBOAuthenticatedOn>();	
	}

	@Override
	public Class<? extends DBOAuthenticatedOn> getBackupClass() {
		return DBOAuthenticatedOn.class;
	}

	@Override
	public Class<? extends DBOAuthenticatedOn> getDatabaseObjectClass() {
		return DBOAuthenticatedOn.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authenticatedOn == null) ? 0 : authenticatedOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
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
		DBOAuthenticatedOn other = (DBOAuthenticatedOn) obj;
		if (authenticatedOn == null) {
			if (other.authenticatedOn != null)
				return false;
		} else if (!authenticatedOn.equals(other.authenticatedOn))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOAuthenticatedOn [principalId=" + principalId + ", etag=" + etag + ", authenticatedOn="
				+ authenticatedOn + "]";
	}



}
