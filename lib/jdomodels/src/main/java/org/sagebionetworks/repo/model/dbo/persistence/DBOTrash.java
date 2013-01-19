package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_TRASH_CAN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TRASH_CAN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.sagebionetworks.repo.model.dbo.AutoIncrementDatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * A trash item in the trash can. It allows us to keep track of who deleted the item and when.
 *
 * @author Eric Wu
 */
public class DBOTrash implements AutoIncrementDatabaseObject<DBOTrash> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("nodeId", COL_TRASH_CAN_NODE_ID, true),
		new FieldColumn("deletedBy", COL_TRASH_CAN_DELETED_BY),
		new FieldColumn("deletedOn", COL_TRASH_CAN_DELETED_ON)
	};

	@Override
	public TableMapping<DBOTrash> getTableMapping() {

		return new TableMapping<DBOTrash>() {

			@Override
			public DBOTrash mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTrash trash = new DBOTrash();
				trash.setNodeId(rs.getLong(COL_TRASH_CAN_NODE_ID));
				trash.setDeletedBy(rs.getLong(COL_TRASH_CAN_DELETED_BY));
				trash.setDeletedOn(rs.getTimestamp(COL_TRASH_CAN_DELETED_ON));
				return trash;
			}

			@Override
			public String getTableName() {
				return TABLE_TRASH_CAN;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_TRASH_CAN;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTrash> getDBOClass() {
				return DBOTrash.class;
			}};
	}

	@Override
	public Long getId() {
		return nodeId;
	}

	@Override
	public void setId(Long id) {
		this.nodeId = id;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Long getDeletedBy() {
		return deletedBy;
	}

	public void setDeletedBy(Long deletedBy) {
		this.deletedBy = deletedBy;
	}

	public Timestamp getDeletedOn() {
		return deletedOn;
	}

	public void setDeletedOn(Timestamp deletedOn) {
		this.deletedOn = deletedOn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((deletedBy == null) ? 0 : deletedBy.hashCode());
		result = prime * result
				+ ((deletedOn == null) ? 0 : deletedOn.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOTrash other = (DBOTrash) obj;
		if (deletedBy == null) {
			if (other.deletedBy != null) {
				return false;
			}
		} else if (!deletedBy.equals(other.deletedBy)) {
			return false;
		}
		if (deletedOn == null) {
			if (other.deletedOn != null) {
				return false;
			}
		} else if (!deletedOn.equals(other.deletedOn)) {
			return false;
		}
		if (nodeId == null) {
			if (other.nodeId != null) {
				return false;
			}
		} else if (!nodeId.equals(other.nodeId)) {
			return false;
		}
		return true;
	}

	private Long nodeId;          // The node that has been deleted into the trash can
	private Long deletedBy;       // The user who deleted this item
	private Timestamp deletedOn;  // The date and time when the deletion occurred
}
