package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_COMPOSER_PART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_MULTIPART_COMPOSER_UPLOAD_PART_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE;

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
 * This table tracks the state of each part of a multi-part upload.
 *
 */
public class DBOMultipartUploadComposerPartState implements MigratableDatabaseObject<DBOMultipartUploadComposerPartState, DBOMultipartUploadComposerPartState> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("uploadId", COL_MULTIPART_COMPOSER_PART_UPLOAD_ID, true).withIsBackupId(true),
		new FieldColumn("partRangeLowerBound", COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND, true),
		new FieldColumn("partRangeUpperBound", COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND, true)
	};
	
	Long uploadId;
	Long partRangeLowerBound;
	Long partRangeUpperBound;

	public Long getUploadId() {
		return uploadId;
	}

	public void setUploadId(Long uploadId) {
		this.uploadId = uploadId;
	}

	public Long getPartRangeLowerBound() {
		return partRangeLowerBound;
	}

	public void setPartRangeLowerBound(Long lowerBound) {
		this.partRangeLowerBound = lowerBound;
	}

	public Long getPartRangeUpperBound() {
		return partRangeUpperBound;
	}

	public Long getSizeOfPart() {
		return partRangeUpperBound - partRangeLowerBound + 1;
	}

	public void setPartRangeUpperBound(Long upperBound) {
		this.partRangeUpperBound = upperBound;
	}

	@Override
	public TableMapping<DBOMultipartUploadComposerPartState> getTableMapping() {
		return new TableMapping<DBOMultipartUploadComposerPartState>() {

			@Override
			public DBOMultipartUploadComposerPartState mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOMultipartUploadComposerPartState dbo = new DBOMultipartUploadComposerPartState();
				dbo.setUploadId(rs.getLong(COL_MULTIPART_COMPOSER_PART_UPLOAD_ID));
				dbo.setPartRangeLowerBound(rs.getLong(COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND));
				dbo.setPartRangeUpperBound(rs.getLong(COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_MULTIPART_COMPOSER_UPLOAD_PART_STATE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMultipartUploadComposerPartState> getDBOClass() {
				return DBOMultipartUploadComposerPartState.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MULTIPART_UPLOAD_COMPOSER_PART_STATE;
	}

	@Override
	public MigratableTableTranslation<DBOMultipartUploadComposerPartState, DBOMultipartUploadComposerPartState> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOMultipartUploadComposerPartState> getBackupClass() {
		return DBOMultipartUploadComposerPartState.class;
	}

	@Override
	public Class<? extends DBOMultipartUploadComposerPartState> getDatabaseObjectClass() {
		return DBOMultipartUploadComposerPartState.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((partRangeUpperBound == null) ? 0 : partRangeUpperBound.hashCode());
		result = prime * result
				+ ((partRangeLowerBound == null) ? 0 : partRangeLowerBound.hashCode());
		result = prime * result
				+ ((uploadId == null) ? 0 : uploadId.hashCode());
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
		DBOMultipartUploadComposerPartState other = (DBOMultipartUploadComposerPartState) obj;
		if (!partRangeUpperBound.equals(other.partRangeUpperBound))
			return false;
		if (partRangeLowerBound == null) {
			if (other.partRangeLowerBound != null)
				return false;
		} else if (!partRangeLowerBound.equals(other.partRangeLowerBound))
			return false;
		if (uploadId == null) {
			if (other.uploadId != null)
				return false;
		} else if (!uploadId.equals(other.uploadId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOMultipartUploadPartState [uploadId=" + uploadId
				+ ", partRangeLowerBound=" + partRangeLowerBound
				+ ", partRangeUpperBound=" + partRangeUpperBound
				+ "]";
	}
}
