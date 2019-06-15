package org.sagebionetworks.repo.model.dbo.file;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_MD5_HEX;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_PART_UPLOAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MULTIPART_UPLOAD_PART_STATE_DDL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MULTIPART_UPLOAD_PART_STATE;

import java.sql.Blob;
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
 * This table tracks the state of each part of a multi-part upload.
 *
 */
public class DBOMultipartUploadPartState implements MigratableDatabaseObject<DBOMultipartUploadPartState, DBOMultipartUploadPartState> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("uploadId", COL_MULTIPART_PART_UPLOAD_ID, true).withIsBackupId(true),
		new FieldColumn("partNumber", COL_MULTIPART_PART_NUMBER, true),
		new FieldColumn("partMD5Hex", COL_MULTIPART_PART_MD5_HEX),
		new FieldColumn("errorDetails", COL_MULTIPART_PART_ERROR_DETAILS),
	};
	
	Long uploadId;
	Integer partNumber;
	String partMD5Hex;
	byte[] errorDetails;
	
	public Long getUploadId() {
		return uploadId;
	}

	public void setUploadId(Long uploadId) {
		this.uploadId = uploadId;
	}

	public Integer getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(Integer partNumber) {
		this.partNumber = partNumber;
	}

	public String getPartMD5Hex() {
		return partMD5Hex;
	}

	public void setPartMD5Hex(String partMD5Hex) {
		this.partMD5Hex = partMD5Hex;
	}

	public byte[] getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(byte[] errorDetails) {
		this.errorDetails = errorDetails;
	}
	
	

	@Override
	public TableMapping<DBOMultipartUploadPartState> getTableMapping() {
		return new TableMapping<DBOMultipartUploadPartState>() {

			@Override
			public DBOMultipartUploadPartState mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOMultipartUploadPartState dbo = new DBOMultipartUploadPartState();
				dbo.setUploadId(rs.getLong(COL_MULTIPART_PART_UPLOAD_ID));
				dbo.setPartNumber(rs.getInt(COL_MULTIPART_PART_NUMBER));
				dbo.setPartMD5Hex(rs.getString(COL_MULTIPART_PART_MD5_HEX));
				Blob blob = rs.getBlob(COL_MULTIPART_PART_ERROR_DETAILS);
				if(blob  != null){
					dbo.setErrorDetails(blob.getBytes(1L, (int) blob.length()));
				}
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_MULTIPART_UPLOAD_PART_STATE;
			}

			@Override
			public String getDDLFileName() {
				return COL_MULTIPART_UPLOAD_PART_STATE_DDL;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOMultipartUploadPartState> getDBOClass() {
				return DBOMultipartUploadPartState.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MULTIPART_UPLOAD_PART_STATE;
	}

	@Override
	public MigratableTableTranslation<DBOMultipartUploadPartState, DBOMultipartUploadPartState> getTranslator() {
		return new BasicMigratableTableTranslation<DBOMultipartUploadPartState>();
	}

	@Override
	public Class<? extends DBOMultipartUploadPartState> getBackupClass() {
		return DBOMultipartUploadPartState.class;
	}

	@Override
	public Class<? extends DBOMultipartUploadPartState> getDatabaseObjectClass() {
		return DBOMultipartUploadPartState.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(errorDetails);
		result = prime * result
				+ ((partMD5Hex == null) ? 0 : partMD5Hex.hashCode());
		result = prime * result
				+ ((partNumber == null) ? 0 : partNumber.hashCode());
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
		DBOMultipartUploadPartState other = (DBOMultipartUploadPartState) obj;
		if (!Arrays.equals(errorDetails, other.errorDetails))
			return false;
		if (partMD5Hex == null) {
			if (other.partMD5Hex != null)
				return false;
		} else if (!partMD5Hex.equals(other.partMD5Hex))
			return false;
		if (partNumber == null) {
			if (other.partNumber != null)
				return false;
		} else if (!partNumber.equals(other.partNumber))
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
				+ ", partNumber=" + partNumber + ", partMD5Hex=" + partMD5Hex
				+ ", errorDetails=" + Arrays.toString(errorDetails) + "]";
	}

	
}
