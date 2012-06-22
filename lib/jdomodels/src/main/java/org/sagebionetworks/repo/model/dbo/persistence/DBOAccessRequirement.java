/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_PARAMETERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;

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
public class DBOAccessRequirement implements AutoIncrementDatabaseObject<DBOAccessRequirement> {
	private Long id;
	private Long eTag = 0L;
	private Long createdBy;
	private long createdOn;
	private Long modifiedBy;
	private long modifiedOn;
	private Long nodeId;
	private String accessType;
	private String requirementType;
	private byte[] requirementParameters;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ACCESS_REQUIREMENT_ID, true),
		new FieldColumn("eTag", COL_ACCESS_REQUIREMENT_ETAG),
		new FieldColumn("createdBy", COL_ACCESS_REQUIREMENT_CREATED_BY),
		new FieldColumn("createdOn", COL_ACCESS_REQUIREMENT_CREATED_ON),
		new FieldColumn("modifiedBy", COL_ACCESS_REQUIREMENT_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_ACCESS_REQUIREMENT_MODIFIED_ON),
		new FieldColumn("nodeId", COL_ACCESS_REQUIREMENT_NODE_ID),
		new FieldColumn("accessType", COL_ACCESS_REQUIREMENT_ACCESS_TYPE),
		new FieldColumn("requirementType", COL_ACCESS_REQUIREMENT_TYPE),
		new FieldColumn("requirementParameters", COL_ACCESS_REQUIREMENT_PARAMETERS)
		};


	@Override
	public TableMapping<DBOAccessRequirement> getTableMapping() {
		return new TableMapping<DBOAccessRequirement>() {
			// Map a result set to this object
			@Override
			public DBOAccessRequirement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAccessRequirement ar = new DBOAccessRequirement();
				ar.setId(rs.getLong(COL_ACCESS_REQUIREMENT_ID));
				ar.seteTag(rs.getLong(COL_ACCESS_REQUIREMENT_ETAG));
				ar.setCreatedBy(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_BY));
				ar.setCreatedOn(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_ON));
				ar.setModifiedBy(rs.getLong(COL_ACCESS_REQUIREMENT_MODIFIED_BY));
				ar.setModifiedOn(rs.getLong(COL_ACCESS_REQUIREMENT_MODIFIED_ON));
				ar.setNodeId(rs.getLong(COL_ACCESS_REQUIREMENT_NODE_ID));
				ar.setAccessType(rs.getString(COL_ACCESS_REQUIREMENT_ACCESS_TYPE));
				ar.setRequirementType(rs.getString(COL_ACCESS_REQUIREMENT_TYPE));
				java.sql.Blob blob = rs.getBlob(COL_ACCESS_REQUIREMENT_PARAMETERS);
				if(blob != null){
					ar.setRequirementParameters(blob.getBytes(1, (int) blob.length()));
				}
				return ar;
			}

			@Override
			public String getTableName() {
				return TABLE_ACCESS_REQUIREMENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ACCESS_REQUIREMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAccessRequirement> getDBOClass() {
				return DBOAccessRequirement.class;
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


	public Long getNodeId() {
		return nodeId;
	}


	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}


	public String getAccessType() {
		return accessType;
	}


	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}


	public String getRequirementType() {
		return requirementType;
	}


	public void setRequirementType(String requirementType) {
		this.requirementType = requirementType;
	}


	public byte[] getRequirementParameters() {
		return requirementParameters;
	}


	public void setRequirementParameters(byte[] requirementParameters) {
		this.requirementParameters = requirementParameters;
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
				+ ((accessType == null) ? 0 : accessType.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + (int) (createdOn ^ (createdOn >>> 32));
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + (int) (modifiedOn ^ (modifiedOn >>> 32));
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result + Arrays.hashCode(requirementParameters);
		result = prime * result
				+ ((requirementType == null) ? 0 : requirementType.hashCode());
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
		DBOAccessRequirement other = (DBOAccessRequirement) obj;
		if (accessType == null) {
			if (other.accessType != null)
				return false;
		} else if (!accessType.equals(other.accessType))
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
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (!Arrays.equals(requirementParameters, other.requirementParameters))
			return false;
		if (requirementType == null) {
			if (other.requirementType != null)
				return false;
		} else if (!requirementType.equals(other.requirementType))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DBOAccessRequirement [id=" + id + ", eTag=" + eTag
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", nodeId=" + nodeId + ", accessType=" + accessType
				+ ", requirementType=" + requirementType
				+ ", requirementParameters="
				+ Arrays.toString(requirementParameters) + "]";
	}

	

}
