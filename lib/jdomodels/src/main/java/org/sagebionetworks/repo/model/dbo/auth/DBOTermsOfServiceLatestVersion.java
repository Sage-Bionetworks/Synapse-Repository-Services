package org.sagebionetworks.repo.model.dbo.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOTermsOfServiceLatestVersion implements DatabaseObject<DBOTermsOfServiceLatestVersion> {

	public static final Long LATEST_VERSION_ID = 0L;

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", SqlConstants.COL_TOS_LATEST_VERSION_ID, true),
		new FieldColumn("updatedOn", SqlConstants.COL_TOS_LATEST_VERSION_UPDATED_ON),
		new FieldColumn("version", SqlConstants.COL_TOS_LATEST_VERSION_VERSION)
	};
	
	private Long id;
	private Date updatedOn;
	private String version;
	
	public Long getId() {
		return id;
	}

	public DBOTermsOfServiceLatestVersion setId(Long id) {
		this.id = id;
		return this;
	}

	public Date getUpdatedOn() {
		return updatedOn;
	}

	public DBOTermsOfServiceLatestVersion setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public DBOTermsOfServiceLatestVersion setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public TableMapping<DBOTermsOfServiceLatestVersion> getTableMapping() {
		return new TableMapping<DBOTermsOfServiceLatestVersion>() {
			
			@Override
			public DBOTermsOfServiceLatestVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBOTermsOfServiceLatestVersion()
					.setId(rs.getLong(SqlConstants.COL_TOS_LATEST_VERSION_ID))
					.setUpdatedOn(rs.getTimestamp(SqlConstants.COL_TOS_LATEST_VERSION_UPDATED_ON))
					.setVersion(rs.getString(SqlConstants.COL_TOS_LATEST_VERSION_VERSION));
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_TOS_LATEST_VERSION;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_TABLE_TOS_LATEST_VERSION;
			}
			
			@Override
			public Class<? extends DBOTermsOfServiceLatestVersion> getDBOClass() {
				return DBOTermsOfServiceLatestVersion.class;
			}
		};
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, updatedOn, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOTermsOfServiceLatestVersion)) {
			return false;
		}
		DBOTermsOfServiceLatestVersion other = (DBOTermsOfServiceLatestVersion) obj;
		return Objects.equals(id, other.id) && Objects.equals(updatedOn, other.updatedOn) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return String.format("DBOTermsOfServiceLatestVersion [id=%s, updatedOn=%s, version=%s]", id, updatedOn, version);
	}

}
