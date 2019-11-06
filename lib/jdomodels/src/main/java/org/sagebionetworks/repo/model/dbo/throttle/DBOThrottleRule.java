package org.sagebionetworks.repo.model.dbo.throttle;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_CALL_PERIOD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_MAX_CALLS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_NORMALIZED_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_THROTTLE_RULES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_THROTTLE_RULES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;



/**
 * Data binding for the Throttle Rules table
 * @author zdong
 *
 */
public class DBOThrottleRule implements MigratableDatabaseObject<DBOThrottleRule, DBOThrottleRule>{
	
	private static final FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("throttleId", COL_THROTTLE_RULES_ID, true).withIsBackupId(true),
		new FieldColumn("normalizedUri", COL_THROTTLE_RULES_NORMALIZED_URI),
		new FieldColumn("maxCalls", COL_THROTTLE_RULES_MAX_CALLS),
		new FieldColumn("callPeriodSec", COL_THROTTLE_RULES_CALL_PERIOD),
		new FieldColumn("modifiedOn", COL_THROTTLE_RULES_MODIFIED_ON).withIsEtag(true)
	};
	
	private long throttleId;
	private String normalizedUri;
	private long maxCalls;
	private long callPeriodSec;
	private Date modifiedOn;
	
	@Override
	public String toString(){
		return "DBOThrottleRule [throttleId = " + throttleId + 
								", normalizedUrl = \"" + normalizedUri + "\"" +
								", maxCalls = " + maxCalls +
								", callPeriodSec = " + callPeriodSec +
								", modifiedOn = " + modifiedOn + "]";
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ){
			return false;
		}
		if(! (obj instanceof DBOThrottleRule) ){
			return false;
		}
		DBOThrottleRule other = (DBOThrottleRule) obj;
		
		return other == this //check same reference
			|| (other.throttleId == throttleId //check fields
				&&	Objects.equals(this.normalizedUri, other.normalizedUri)
				&&	other.maxCalls == this.maxCalls
				&&	other.callPeriodSec == this.callPeriodSec
				&&	Objects.equals(this.modifiedOn, other.modifiedOn) );
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(throttleId, normalizedUri, maxCalls, callPeriodSec, modifiedOn);
	};
	
	
	@Override
	public TableMapping<DBOThrottleRule> getTableMapping() {
		return new TableMapping<DBOThrottleRule>() {

			@Override
			public DBOThrottleRule mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOThrottleRule throttleRule = new DBOThrottleRule();
				throttleRule.setThrottleId(rs.getLong(COL_THROTTLE_RULES_ID));
				throttleRule.setNormalizedUri(rs.getString(COL_THROTTLE_RULES_NORMALIZED_URI));
				throttleRule.setMaxCalls(rs.getLong(COL_THROTTLE_RULES_MAX_CALLS));
				throttleRule.setCallPeriodSec(rs.getLong(COL_THROTTLE_RULES_CALL_PERIOD));
				throttleRule.setModifiedOn( new Date( rs.getTimestamp(COL_THROTTLE_RULES_MODIFIED_ON).getTime() ) );
				return throttleRule;
			}

			@Override
			public String getTableName() {
				return TABLE_THROTTLE_RULES;
			}

			@Override
			public String getDDLFileName() {
				return DDL_THROTTLE_RULES;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOThrottleRule> getDBOClass() {
				return DBOThrottleRule.class;
			}
		};
	}
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.THROTTLE_RULE;
	}
	@Override
	public MigratableTableTranslation<DBOThrottleRule, DBOThrottleRule> getTranslator() {
		return new BasicMigratableTableTranslation<DBOThrottleRule>();
	}
	@Override
	public Class<? extends DBOThrottleRule> getBackupClass() {
		return DBOThrottleRule.class;
	}
	@Override
	public Class<? extends DBOThrottleRule> getDatabaseObjectClass() {
		return DBOThrottleRule.class;
	}
	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		// TODO is this correct?
		return new LinkedList<MigratableDatabaseObject<?, ?>>();
	}
	public long getThrottleId() {
		return throttleId;
	}
	public void setThrottleId(long throttleId) {
		this.throttleId = throttleId;
	}
	public String getNormalizedUri() {
		return normalizedUri;
	}
	public void setNormalizedUri(String normalizedUri) {
		this.normalizedUri = normalizedUri;
	}
	public long getMaxCalls() {
		return maxCalls;
	}
	public void setMaxCalls(long maxCalls) {
		this.maxCalls = maxCalls;
	}
	public long getCallPeriodSec() {
		return callPeriodSec;
	}
	public void setCallPeriodSec(long callPeriodSec) {
		this.callPeriodSec = callPeriodSec;
	}
	public Date getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
}
