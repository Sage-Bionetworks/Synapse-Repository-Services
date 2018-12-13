package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE_TABLE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TABLE_ID_SEQUENCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_ID_SEQUENCE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Maps to the table used it issue row IDs for TableEntities
 * 
 * @author jmhill
 *
 */
public class DBOTableIdSequence implements MigratableDatabaseObject<DBOTableIdSequence, DBOTableIdSequence> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("tableId", COL_ID_SEQUENCE_TABLE_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_ID_SEQUENCE_TABLE_ETAG).withIsEtag(true),
		new FieldColumn("versionNumber", COL_ID_SEQUENCE_VERSION),
		new FieldColumn("sequence", COL_ID_SEQUENCE),
	};
	
	private Long tableId;
	private String etag;
	private Long versionNumber;
	private Long sequence;
	
	@Override
	public TableMapping<DBOTableIdSequence> getTableMapping() {

		return new TableMapping<DBOTableIdSequence>(){

			@Override
			public DBOTableIdSequence mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOTableIdSequence seq = new DBOTableIdSequence();
				seq.setTableId(rs.getLong(COL_ID_SEQUENCE_TABLE_ID));
				seq.setEtag(rs.getString(COL_ID_SEQUENCE_TABLE_ETAG));
				seq.setVersionNumber(rs.getLong(COL_ID_SEQUENCE_VERSION));
				seq.setSequence(rs.getLong(COL_ID_SEQUENCE));
				return seq;
			}

			@Override
			public String getTableName() {
				return TABLE_TABLE_ID_SEQUENCE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_TABLE_ID_SEQUENCE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTableIdSequence> getDBOClass() {
				return DBOTableIdSequence.class;
			}};
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public Long getSequence() {
		return sequence;
	}

	public void setSequence(Long sequence) {
		this.sequence = sequence;
	}

	public Long getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(Long versionNumber) {
		this.versionNumber = versionNumber;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TABLE_SEQUENCE;
	}

	@Override
	public MigratableTableTranslation<DBOTableIdSequence, DBOTableIdSequence> getTranslator() {
		return new BasicMigratableTableTranslation<DBOTableIdSequence>();
	}

	@Override
	public Class<? extends DBOTableIdSequence> getBackupClass() {
		return DBOTableIdSequence.class;
	}

	@Override
	public Class<? extends DBOTableIdSequence> getDatabaseObjectClass() {
		return DBOTableIdSequence.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOTableRowChange());
		return list;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((sequence == null) ? 0 : sequence.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
		result = prime * result
				+ ((versionNumber == null) ? 0 : versionNumber.hashCode());
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
		DBOTableIdSequence other = (DBOTableIdSequence) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (sequence == null) {
			if (other.sequence != null)
				return false;
		} else if (!sequence.equals(other.sequence))
			return false;
		if (tableId == null) {
			if (other.tableId != null)
				return false;
		} else if (!tableId.equals(other.tableId))
			return false;
		if (versionNumber == null) {
			if (other.versionNumber != null)
				return false;
		} else if (!versionNumber.equals(other.versionNumber))
			return false;
		return true;
	}
	
}
