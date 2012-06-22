/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_PARAMETERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * @author brucehoff
 *
 */
public class DBOAccessApproval implements AutoIncrementDatabaseObject<DBOAccessApproval> {
	private Long id;
	private Long eTag = 0L;
	private Long createdBy;
	private long createdOn;
	private Long modifiedBy;
	private long modifiedOn;
	private Long requirementId;
	private Long accessorId;
	private String approvalType;
	private byte[] approvalParameters;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ACCESS_APPROVAL_ID, true),
		new FieldColumn("eTag", COL_ACCESS_APPROVAL_ETAG),
		new FieldColumn("createdBy", COL_ACCESS_APPROVAL_CREATED_BY),
		new FieldColumn("createdOn", COL_ACCESS_APPROVAL_CREATED_ON),
		new FieldColumn("modifiedBy", COL_ACCESS_APPROVAL_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_ACCESS_APPROVAL_MODIFIED_ON),
		new FieldColumn("requirementId", COL_ACCESS_APPROVAL_REQUIREMENT_ID),
		new FieldColumn("accessorId", COL_ACCESS_APPROVAL_ACCESSOR_ID),
		new FieldColumn("approvalType", COL_ACCESS_APPROVAL_TYPE),
		new FieldColumn("approvalParameters", COL_ACCESS_APPROVAL_PARAMETERS)
		};


	@Override
	public TableMapping<DBOAccessApproval> getTableMapping() {
		return new TableMapping<DBOAccessApproval>() {
			// Map a result set to this object
			@Override
			public DBOAccessApproval mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAccessApproval aa = new DBOAccessApproval();
				aa.setId(rs.getLong(COL_ACCESS_APPROVAL_ID));
				aa.seteTag(rs.getLong(COL_ACCESS_APPROVAL_ETAG));
				aa.setCreatedBy(rs.getLong(COL_ACCESS_APPROVAL_CREATED_BY));
				aa.setCreatedOn(rs.getLong(COL_ACCESS_APPROVAL_CREATED_ON));
				aa.setModifiedBy(rs.getLong(COL_ACCESS_APPROVAL_MODIFIED_BY));
				aa.setModifiedOn(rs.getLong(COL_ACCESS_APPROVAL_MODIFIED_ON));
				aa.setRequirementId(rs.getLong(COL_ACCESS_APPROVAL_REQUIREMENT_ID));
				aa.setAccessorId(rs.getLong(COL_ACCESS_APPROVAL_ACCESSOR_ID));
				aa.setApprovalType(rs.getString(COL_ACCESS_APPROVAL_TYPE));
				java.sql.Blob blob = rs.getBlob(COL_ACCESS_APPROVAL_PARAMETERS);
				if(blob != null){
					aa.setApprovalParameters(blob.getBytes(1, (int) blob.length()));
				}
				return aa;
			}

			@Override
			public String getTableName() {
				return TABLE_ACCESS_APPROVAL;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ACCESS_APPROVAL;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAccessApproval> getDBOClass() {
				return DBOAccessApproval.class;
			}
		};
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public Long geteTag() {
		return eTag;
	}


	public void seteTag(Long eTag) {
		this.eTag = eTag;
	}


	public Long getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}


	public Long getModifiedBy() {
		return modifiedBy;
	}


	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}


	public Long getRequirementId() {
		return requirementId;
	}


	public void setRequirementId(Long requirementId) {
		this.requirementId = requirementId;
	}


	public Long getAccessorId() {
		return accessorId;
	}


	public void setAccessorId(Long accessorId) {
		this.accessorId = accessorId;
	}


	public String getApprovalType() {
		return approvalType;
	}


	public void setApprovalType(String approvalType) {
		this.approvalType = approvalType;
	}


	public byte[] getApprovalParameters() {
		return approvalParameters;
	}


	public void setApprovalParameters(byte[] approvalParameters) {
		this.approvalParameters = approvalParameters;
	}


	public long getCreatedOn() {
		return createdOn;
	}


	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}


	public long getModifiedOn() {
		return modifiedOn;
	}


	public void setModifiedOn(long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessorId == null) ? 0 : accessorId.hashCode());
		result = prime * result + Arrays.hashCode(approvalParameters);
		result = prime * result
				+ ((approvalType == null) ? 0 : approvalType.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + (int) (createdOn ^ (createdOn >>> 32));
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + (int) (modifiedOn ^ (modifiedOn >>> 32));
		result = prime * result
				+ ((requirementId == null) ? 0 : requirementId.hashCode());
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
		DBOAccessApproval other = (DBOAccessApproval) obj;
		if (accessorId == null) {
			if (other.accessorId != null)
				return false;
		} else if (!accessorId.equals(other.accessorId))
			return false;
		if (!Arrays.equals(approvalParameters, other.approvalParameters))
			return false;
		if (approvalType == null) {
			if (other.approvalType != null)
				return false;
		} else if (!approvalType.equals(other.approvalType))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn != other.createdOn)
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn != other.modifiedOn)
			return false;
		if (requirementId == null) {
			if (other.requirementId != null)
				return false;
		} else if (!requirementId.equals(other.requirementId))
			return false;
		return true;
	}


	
}
