package org.sagebionetworks.table.cluster.description;

import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.util.ValidateArgument;

/**
 * The minimal state needed to reconstruct an IndexDescription. Captured at snapshot
 * build time and stored so a future query can reconstruct the real IndexDescription
 * object (with all its type-specific behavior) from this state plus a lookup function.
 */
public class IndexDescriptionState {

	private final IdAndVersion idAndVersion;
	private final TableType tableType;
	private final String definingSql;
	private final Long indexVersion;

	public IndexDescriptionState(IdAndVersion idAndVersion, TableType tableType, String definingSql, Long indexVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(tableType, "tableType");
		this.idAndVersion = idAndVersion;
		this.tableType = tableType;
		this.definingSql = definingSql;
		this.indexVersion = indexVersion;
	}

	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}

	public TableType getTableType() {
		return tableType;
	}

	public Optional<String> getDefiningSql() {
		return Optional.ofNullable(definingSql);
	}

	public Optional<Long> getIndexVersion() {
		return Optional.ofNullable(indexVersion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(definingSql, idAndVersion, indexVersion, tableType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IndexDescriptionState)) {
			return false;
		}
		IndexDescriptionState other = (IndexDescriptionState) obj;
		return Objects.equals(definingSql, other.definingSql) && Objects.equals(idAndVersion, other.idAndVersion)
				&& Objects.equals(indexVersion, other.indexVersion) && tableType == other.tableType;
	}

	@Override
	public String toString() {
		return "IndexDescriptionState [idAndVersion=" + idAndVersion + ", tableType=" + tableType + ", definingSql="
				+ (definingSql != null ? definingSql.substring(0, Math.min(50, definingSql.length())) + "..." : "null")
				+ ", indexVersion=" + indexVersion + "]";
	}
}
