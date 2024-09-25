package org.sagebionetworks.repo.model.dbo.agent;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_TRACE_JOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_TRACE_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_TRACE_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_AGENT_TRACE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AGENT_TRACE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOAgentTrace implements DatabaseObject<DBOAgentTrace> {

	private Long jobId;
	private Long timestamp;
	private String message;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("jobId", COL_AGENT_TRACE_JOB_ID).withIsPrimaryKey(true),
			new FieldColumn("timestamp", COL_AGENT_TRACE_TIMESTAMP).withIsPrimaryKey(true),
			new FieldColumn("message", COL_AGENT_TRACE_MESSAGE), };

	@Override
	public TableMapping<DBOAgentTrace> getTableMapping() {
		return new TableMapping<DBOAgentTrace>() {

			@Override
			public DBOAgentTrace mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBOAgentTrace().setJobId(rs.getLong(COL_AGENT_TRACE_JOB_ID))
						.setTimestamp(rs.getLong(COL_AGENT_TRACE_TIMESTAMP)).setMessage(COL_AGENT_TRACE_MESSAGE);
			}

			@Override
			public String getTableName() {
				return TABLE_AGENT_TRACE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_AGENT_TRACE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAgentTrace> getDBOClass() {
				return DBOAgentTrace.class;
			}
		};
	}

	public Long getJobId() {
		return jobId;
	}

	public DBOAgentTrace setJobId(Long jobId) {
		this.jobId = jobId;
		return this;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public DBOAgentTrace setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public DBOAgentTrace setMessage(String message) {
		this.message = message;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId, message, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOAgentTrace other = (DBOAgentTrace) obj;
		return Objects.equals(jobId, other.jobId) && Objects.equals(message, other.message)
				&& Objects.equals(timestamp, other.timestamp);
	}

	@Override
	public String toString() {
		return "DBOAgentTrace [jobId=" + jobId + ", timestamp=" + timestamp + ", message=" + message + "]";
	}

}
