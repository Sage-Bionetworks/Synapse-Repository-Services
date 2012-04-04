package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PREVIEW_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PREVIEW_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PREVIEW_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_PREVIEW_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PREVIEW_BLOB;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * DBO for preview blobs.
 * 
 * @author John
 * 
 */
public class DBOPreviewBlob implements DatabaseObject<DBOPreviewBlob> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("ownerId", COL_PREVIEW_OWNER_ID, true),
			new FieldColumn("tokenId", COL_PREVIEW_TOKEN_ID, true), 
			new FieldColumn("previewBlob", COL_PREVIEW_BLOB), 
	};

	@Override
	public TableMapping<DBOPreviewBlob> getTableMapping() {
		return new TableMapping<DBOPreviewBlob>() {

			@Override
			public DBOPreviewBlob mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOPreviewBlob preview = new DBOPreviewBlob();
				preview.setOwnerId(rs.getLong(COL_PREVIEW_OWNER_ID));
				preview.setTokenId(rs.getLong(COL_PREVIEW_TOKEN_ID));
				java.sql.Blob blob = rs.getBlob(COL_PREVIEW_BLOB);
				if(blob != null){
					preview.setPreviewBlob(blob.getBytes(1, (int) blob.length()));
				}
				return preview;
			}

			@Override
			public String getTableName() {
				return TABLE_PREVIEW_BLOB;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_PREVIEW_BLOB;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOPreviewBlob> getDBOClass() {
				return DBOPreviewBlob.class;
			}
		};
	}

	private Long ownerId;
	private Long tokenId;
	private byte[] previewBlob;

	public Long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}
	public Long getTokenId() {
		return tokenId;
	}
	public void setTokenId(Long tokenId) {
		this.tokenId = tokenId;
	}
	public byte[] getPreviewBlob() {
		return previewBlob;
	}
	public void setPreviewBlob(byte[] previewBlob) {
		this.previewBlob = previewBlob;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result + Arrays.hashCode(previewBlob);
		result = prime * result + ((tokenId == null) ? 0 : tokenId.hashCode());
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
		DBOPreviewBlob other = (DBOPreviewBlob) obj;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (!Arrays.equals(previewBlob, other.previewBlob))
			return false;
		if (tokenId == null) {
			if (other.tokenId != null)
				return false;
		} else if (!tokenId.equals(other.tokenId))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBOPreviewBlob [ownerId=" + ownerId + ", tokenId=" + tokenId
				+ ", previewBlob=" + Arrays.toString(previewBlob) + "]";
	}
}
