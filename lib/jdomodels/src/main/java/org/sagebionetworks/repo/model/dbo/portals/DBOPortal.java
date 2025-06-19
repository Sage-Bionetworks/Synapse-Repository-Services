package org.sagebionetworks.repo.model.dbo.portals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOPortal implements MigratableDatabaseObject<DBOPortal, DBOPortal> {
	
	public static final Long SYNAPSE_PORTAL_ID = 1L;
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", SqlConstants.COL_PORTAL_ID, true).withIsBackupId(true),
		new FieldColumn("etag", SqlConstants.COL_PORTAL_ETAG).withIsEtag(true),
		new FieldColumn("createdBy", SqlConstants.COL_PORTAL_CREATED_BY),
		new FieldColumn("createdOn", SqlConstants.COL_PORTAL_CREATED_ON),
		new FieldColumn("modifiedBy", SqlConstants.COL_PORTAL_MODIFIED_BY),
		new FieldColumn("modifiedOn", SqlConstants.COL_PORTAL_MODIFIED_ON),
		new FieldColumn("name", SqlConstants.COL_PORTAL_NAME),
		new FieldColumn("endpoint", SqlConstants.COL_PORTAL_ENDPOINT)		
	};

	private static final TableMapping<DBOPortal> TABLE_MAPPING = new TableMapping<DBOPortal>() {

		@Override
		public DBOPortal mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new DBOPortal()
				.setId(rs.getLong(SqlConstants.COL_PORTAL_ID))
				.setEtag(rs.getString(SqlConstants.COL_PORTAL_ETAG))
				.setCreatedBy(rs.getLong(SqlConstants.COL_PORTAL_CREATED_BY))
				.setCreatedOn(rs.getTimestamp(SqlConstants.COL_PORTAL_CREATED_ON))
				.setModifiedBy(rs.getLong(SqlConstants.COL_PORTAL_MODIFIED_BY))
				.setModifiedOn(rs.getTimestamp(SqlConstants.COL_PORTAL_MODIFIED_ON))
				.setName(rs.getString(SqlConstants.COL_PORTAL_NAME))
				.setEndpoint(rs.getString(SqlConstants.COL_PORTAL_ENDPOINT));
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_PORTAL;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_PORTAL;
		}

		@Override
		public Class<? extends DBOPortal> getDBOClass() {
			return DBOPortal.class;
		}
	};

	private Long id;
	private String etag;
	private Long createdBy;
	private Timestamp createdOn;
	private Long modifiedBy;
	private Timestamp modifiedOn;
	private String name;
	private String endpoint;

	public DBOPortal() { }

	public Long getId() {
		return id;
	}

	public DBOPortal setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOPortal setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOPortal setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public DBOPortal setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public DBOPortal setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public DBOPortal setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	public String getName() {
		return name;
	}

	public DBOPortal setName(String name) {
		this.name = name;
		return this;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public DBOPortal setEndpoint(String endpoint) {
		this.endpoint = endpoint;
		return this;
	}

	@Override
	public TableMapping<DBOPortal> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PORTAL;
	}

	@Override
	public MigratableTableTranslation<DBOPortal, DBOPortal> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOPortal> getBackupClass() {
		return DBOPortal.class;
	}

	@Override
	public Class<? extends DBOPortal> getDatabaseObjectClass() {
		return DBOPortal.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, endpoint, etag, id, modifiedBy, modifiedOn, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOPortal)) {
			return false;
		}
		DBOPortal other = (DBOPortal) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn) && Objects.equals(endpoint, other.endpoint)
			&& Objects.equals(etag, other.etag) && Objects.equals(id, other.id) && Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modifiedOn, other.modifiedOn)
			&& Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return String.format("DBOPortal [id=%s, etag=%s, createdBy=%s, createdOn=%s, modifiedBy=%s, modifiedOn=%s, name=%s, endpoint=%s]", id, etag, createdBy, createdOn,
			modifiedBy, modifiedOn, name, endpoint);
	}

}
