package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_CURRENT_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_PENDING_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STACK_STATUS_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_STACK_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STACK_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOStackStatus implements DatabaseObject<DBOStackStatus> {
	
	/**
	 * This is the only id used by the table.
	 */
	public static final Long STATUS_ID = new Long(0);
	
	public static final String DEFAULT_MESSAGE = "Synapse is ready for both READ and WRITE";


	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_STACK_STATUS_ID, true),
			new FieldColumn("status", COL_STACK_STATUS_STATUS), 
			new FieldColumn("currentMessage", COL_STACK_STATUS_CURRENT_MESSAGE), 
			new FieldColumn("pendingMessage", COL_STACK_STATUS_PENDING_MESSAGE), 
	};

	@Override
	public TableMapping<DBOStackStatus> getTableMapping() {
		return new TableMapping<DBOStackStatus>() {

			@Override
			public DBOStackStatus mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOStackStatus status = new DBOStackStatus();
				status.setId(rs.getLong(COL_STACK_STATUS_ID));
				status.setStatus(rs.getString(COL_STACK_STATUS_STATUS));
				status.setCurrentMessage(rs.getString(COL_STACK_STATUS_CURRENT_MESSAGE));
				if(rs.wasNull()){
					status.setCurrentMessage(null);
				}
				status.setPendingMessage(rs.getString(COL_STACK_STATUS_PENDING_MESSAGE));
				if(rs.wasNull()){
					status.setPendingMessage(null);
				}
				return status;
			}

			@Override
			public String getTableName() {
				return TABLE_STACK_STATUS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_STACK_STATUS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOStackStatus> getDBOClass() {
				return DBOStackStatus.class;
			}
		};
	}

	private Long id;
	private String status;
	private String currentMessage;
	private String pendingMessage;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getCurrentMessage() {
		return currentMessage;
	}
	public void setCurrentMessage(String currentMessage) {
		this.currentMessage = currentMessage;
	}
	public String getPendingMessage() {
		return pendingMessage;
	}
	public void setPendingMessage(String pendingMessage) {
		this.pendingMessage = pendingMessage;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((currentMessage == null) ? 0 : currentMessage.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((pendingMessage == null) ? 0 : pendingMessage.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		DBOStackStatus other = (DBOStackStatus) obj;
		if (currentMessage == null) {
			if (other.currentMessage != null)
				return false;
		} else if (!currentMessage.equals(other.currentMessage))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (pendingMessage == null) {
			if (other.pendingMessage != null)
				return false;
		} else if (!pendingMessage.equals(other.pendingMessage))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBOStackStatus [id=" + id + ", status=" + status
				+ ", currentMessage=" + currentMessage + ", pendingMessage="
				+ pendingMessage + "]";
	}
	
}
