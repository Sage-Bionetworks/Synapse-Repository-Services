package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * The single row recording how far this stack has read the signing service's envelope status changes.
 * <p>
 * This table does not migrate. A stack starting without it reads back over a lookback window instead,
 * which is safe because the envelopes already recorded do migrate.
 */
public class DBOEDucEnvelopeStatusCheckpoint implements DatabaseObject<DBOEDucEnvelopeStatusCheckpoint> {

	/**
	 * The ID of the only row.
	 */
	public static final long CHECKPOINT_ID = 1L;

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", SqlConstants.COL_EDUC_ENVELOPE_STATUS_CHECKPOINT_ID, true),
			new FieldColumn("lastQueriedOn", SqlConstants.COL_EDUC_ENVELOPE_STATUS_CHECKPOINT_LAST_QUERIED_ON)
	};

	private static final TableMapping<DBOEDucEnvelopeStatusCheckpoint> TABLE_MAPPING = new TableMapping<DBOEDucEnvelopeStatusCheckpoint>() {

		@Override
		public DBOEDucEnvelopeStatusCheckpoint mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOEDucEnvelopeStatusCheckpoint dbo = new DBOEDucEnvelopeStatusCheckpoint();
			dbo.setId(rs.getLong(SqlConstants.COL_EDUC_ENVELOPE_STATUS_CHECKPOINT_ID));
			dbo.setLastQueriedOn(rs.getTimestamp(SqlConstants.COL_EDUC_ENVELOPE_STATUS_CHECKPOINT_LAST_QUERIED_ON));
			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_EDUC_ENVELOPE_STATUS_CHECKPOINT;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_EDUC_ENVELOPE_STATUS_CHECKPOINT;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOEDucEnvelopeStatusCheckpoint> getDBOClass() {
			return DBOEDucEnvelopeStatusCheckpoint.class;
		}
	};

	private Long id;
	private Timestamp lastQueriedOn;

	@Override
	public TableMapping<DBOEDucEnvelopeStatusCheckpoint> getTableMapping() {
		return TABLE_MAPPING;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Timestamp getLastQueriedOn() {
		return lastQueriedOn;
	}

	public void setLastQueriedOn(Timestamp lastQueriedOn) {
		this.lastQueriedOn = lastQueriedOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, lastQueriedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOEDucEnvelopeStatusCheckpoint)) {
			return false;
		}
		DBOEDucEnvelopeStatusCheckpoint other = (DBOEDucEnvelopeStatusCheckpoint) obj;
		return Objects.equals(id, other.id) && Objects.equals(lastQueriedOn, other.lastQueriedOn);
	}

	@Override
	public String toString() {
		return "DBOEDucEnvelopeStatusCheckpoint [id=" + id + ", lastQueriedOn=" + lastQueriedOn + "]";
	}
}
