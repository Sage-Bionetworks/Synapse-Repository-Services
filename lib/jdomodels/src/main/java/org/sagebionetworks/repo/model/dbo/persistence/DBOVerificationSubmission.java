package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_SERIALIZED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_VERIFICATION_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_SUBMISSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOVerificationSubmission implements
		MigratableDatabaseObject<DBOVerificationSubmission, DBOVerificationSubmission> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_VERIFICATION_SUBMISSION_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_VERIFICATION_SUBMISSION_ETAG).withIsEtag(true),
			new FieldColumn("createdBy", COL_VERIFICATION_SUBMISSION_CREATED_BY),
			new FieldColumn("createdOn", COL_VERIFICATION_SUBMISSION_CREATED_ON),
			new FieldColumn("serialized", COL_VERIFICATION_SUBMISSION_SERIALIZED),
	};

	private Long id;
	private String etag;
	private Long createdBy;
	private Long createdOn;
	private byte[] serialized;

	private static final MigratableTableTranslation<DBOVerificationSubmission, DBOVerificationSubmission> MIGRATION_MAPPER = new BasicMigratableTableTranslation<>();

	@Override
	public TableMapping<DBOVerificationSubmission> getTableMapping() {
		return new TableMapping<DBOVerificationSubmission>() {
			
			@Override
			public DBOVerificationSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOVerificationSubmission dbo = new DBOVerificationSubmission();
				dbo.setId(rs.getLong(COL_VERIFICATION_SUBMISSION_ID));
				dbo.setEtag(rs.getString(COL_VERIFICATION_SUBMISSION_ETAG));
				dbo.setCreatedBy(rs.getLong(COL_VERIFICATION_SUBMISSION_CREATED_BY));
				dbo.setCreatedOn(rs.getLong(COL_VERIFICATION_SUBMISSION_CREATED_ON));
				dbo.setSerialized(rs.getBytes(COL_VERIFICATION_SUBMISSION_SERIALIZED));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_VERIFICATION_SUBMISSION;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_VERIFICATION_SUBMISSION;
			}
			
			@Override
			public Class<? extends DBOVerificationSubmission> getDBOClass() {
				return DBOVerificationSubmission.class; 
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VERIFICATION_SUBMISSION;
	}

	@Override
	public MigratableTableTranslation<DBOVerificationSubmission, DBOVerificationSubmission> getTranslator() { return MIGRATION_MAPPER;}

	@Override
	public Class<? extends DBOVerificationSubmission> getBackupClass() {
		return DBOVerificationSubmission.class;
	}

	@Override
	public Class<? extends DBOVerificationSubmission> getDatabaseObjectClass() {
		return DBOVerificationSubmission.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOVerificationSubmissionFile());
		return list;
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

	public byte[] getSerialized() {
		return serialized;
	}

	public void setSerialized(byte[] serialized) {
		this.serialized = serialized;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(serialized);
		result = prime * result + Objects.hash(createdBy, createdOn, etag, id);
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
		DBOVerificationSubmission other = (DBOVerificationSubmission) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Arrays.equals(serialized, other.serialized);
	}

	@Override
	public String toString() {
		return "DBOVerificationSubmission [id=" + id + ", etag=" + etag + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", serialized=" + Arrays.toString(serialized) + "]";
	}

}
