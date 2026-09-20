package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COLUMN_PROVENANCE_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COLUMN_PROVENANCE_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COLUMN_PROVENANCE_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_COLUMN_PROVENANCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COLUMN_PROVENANCE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Stores the serialized {@code ColumnProvenance} document for one version of a defining-SQL object
 * (MaterializedView, VirtualTable, SearchIndex). Note: column provenance does not migrate — it is
 * fully derivable from the defining SQL and bound schema and is lazily (re)populated per stack.
 */
public class DBOColumnProvenance implements DatabaseObject<DBOColumnProvenance> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("objectId", COL_COLUMN_PROVENANCE_OBJECT_ID, true),
			new FieldColumn("objectVersion", COL_COLUMN_PROVENANCE_OBJECT_VERSION, true),
			new FieldColumn("provenanceJson", COL_COLUMN_PROVENANCE_JSON),
	};

	private Long objectId;
	private Long objectVersion;
	private String provenanceJson;

	@Override
	public TableMapping<DBOColumnProvenance> getTableMapping() {
		return new TableMapping<DBOColumnProvenance>() {

			@Override
			public DBOColumnProvenance mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOColumnProvenance dbo = new DBOColumnProvenance();
				dbo.setObjectId(rs.getLong(COL_COLUMN_PROVENANCE_OBJECT_ID));
				dbo.setObjectVersion(rs.getLong(COL_COLUMN_PROVENANCE_OBJECT_VERSION));
				dbo.setProvenanceJson(rs.getString(COL_COLUMN_PROVENANCE_JSON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_COLUMN_PROVENANCE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_COLUMN_PROVENANCE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOColumnProvenance> getDBOClass() {
				return DBOColumnProvenance.class;
			}
		};
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public Long getObjectVersion() {
		return objectVersion;
	}

	public void setObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
	}

	public String getProvenanceJson() {
		return provenanceJson;
	}

	public void setProvenanceJson(String provenanceJson) {
		this.provenanceJson = provenanceJson;
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectId, objectVersion, provenanceJson);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOColumnProvenance)) {
			return false;
		}
		DBOColumnProvenance other = (DBOColumnProvenance) obj;
		return Objects.equals(objectId, other.objectId) && Objects.equals(objectVersion, other.objectVersion)
				&& Objects.equals(provenanceJson, other.provenanceJson);
	}

	@Override
	public String toString() {
		return "DBOColumnProvenance [objectId=" + objectId + ", objectVersion=" + objectVersion + ", provenanceJson="
				+ provenanceJson + "]";
	}

}
