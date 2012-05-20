package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * The datababase object for a node.
 * 
 * @author jmhill
 *
 */
public class DBONode implements DatabaseObject<DBONode> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_NODE_ID, true),
			new FieldColumn("parentId", COL_NODE_PARENT_ID),
			new FieldColumn("name", COL_NODE_NAME),
			new FieldColumn("currentRevNumber", COL_CURRENT_REV),
			new FieldColumn("description", COL_NODE_DESCRIPTION),
			new FieldColumn("eTag", COL_NODE_ETAG),
			new FieldColumn("createdBy", COL_NODE_CREATED_BY),
			new FieldColumn("createdOn", COL_NODE_CREATED_ON),
			new FieldColumn("nodeType", COL_NODE_TYPE),
			new FieldColumn("benefactorId", COL_NODE_BENEFACTOR_ID),
			};

	@Override
	public TableMapping<DBONode> getTableMapping() {
		return new TableMapping<DBONode>() {
			// Map a result set to this object
			@Override
			public DBONode mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBONode node = new DBONode();
				node.setId(rs.getLong(COL_NODE_ID));
				node.setParentId(rs.getLong(COL_NODE_PARENT_ID));
				if(rs.wasNull()){
					node.setParentId(null);
				}
				node.setName(rs.getString(COL_NODE_NAME));
				node.setCurrentRevNumber(rs.getLong(COL_CURRENT_REV));
				if(rs.wasNull()){
					node.setCurrentRevNumber(null);
				}
				java.sql.Blob blob = rs.getBlob(COL_NODE_DESCRIPTION);
				if(blob != null){
					node.setDescription(blob.getBytes(1, (int) blob.length()));
				}
				node.seteTag(rs.getLong(COL_NODE_ETAG));
				node.setCreatedBy(rs.getLong(COL_NODE_CREATED_BY));
				node.setCreatedOn(rs.getLong(COL_NODE_CREATED_ON));
				node.setNodeType(rs.getShort(COL_NODE_TYPE));
				node.setBenefactorId(rs.getLong(COL_NODE_BENEFACTOR_ID));
				// If the value was null we must set it to null
				if(rs.wasNull()){
					node.setBenefactorId(null);
				}
				return node;
			}

			@Override
			public String getTableName() {
				return TABLE_NODE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_NODE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBONode> getDBOClass() {
				return DBONode.class;
			}
		};
	}
	
	private Long id;
	private Long parentId;
	private String name;
	private Long currentRevNumber;
	private byte[] description;
	private Long eTag = null;
	private Long createdBy;
	private Long createdOn;
	private Short nodeType;	
	private Long benefactorId;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getParentId() {
		return parentId;
	}
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getCurrentRevNumber() {
		return currentRevNumber;
	}
	public void setCurrentRevNumber(Long currentRevNumber) {
		this.currentRevNumber = currentRevNumber;
	}
	public byte[] getDescription() {
		return description;
	}
	public void setDescription(byte[] description) {
		this.description = description;
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
	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}
	public Short getNodeType() {
		return nodeType;
	}
	public void setNodeType(Short nodeType) {
		this.nodeType = nodeType;
	}
	public Long getBenefactorId() {
		return benefactorId;
	}
	public void setBenefactorId(Long benefactorId) {
		this.benefactorId = benefactorId;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((benefactorId == null) ? 0 : benefactorId.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime
				* result
				+ ((currentRevNumber == null) ? 0 : currentRevNumber.hashCode());
		result = prime * result + Arrays.hashCode(description);
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((nodeType == null) ? 0 : nodeType.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
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
		DBONode other = (DBONode) obj;
		if (benefactorId == null) {
			if (other.benefactorId != null)
				return false;
		} else if (!benefactorId.equals(other.benefactorId))
			return false;
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
		if (currentRevNumber == null) {
			if (other.currentRevNumber != null)
				return false;
		} else if (!currentRevNumber.equals(other.currentRevNumber))
			return false;
		if (!Arrays.equals(description, other.description))
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (nodeType == null) {
			if (other.nodeType != null)
				return false;
		} else if (!nodeType.equals(other.nodeType))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "DBONode [id=" + id + ", parentId=" + parentId + ", name="
				+ name + ", currentRevNumber=" + currentRevNumber
				+ ", description=" + description + ", eTag=" + eTag
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", nodeType=" + nodeType + ", benefactorId=" + benefactorId
				+ "]";
	}


}
