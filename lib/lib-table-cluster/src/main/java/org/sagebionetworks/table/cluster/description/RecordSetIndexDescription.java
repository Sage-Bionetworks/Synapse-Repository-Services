package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_SEARCH_CONTENT;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableIndexType;
import org.sagebionetworks.table.query.model.SqlContext;

public class RecordSetIndexDescription implements IndexDescription {

	private final IdAndVersion idAndVersion;
	private final Long lastTableChangeNumber;

	public RecordSetIndexDescription(IdAndVersion idAndVersion) {
		this(idAndVersion, null);
	}

	public RecordSetIndexDescription(IdAndVersion idAndVersion, Long lastTableChangeNumber) {
		this.idAndVersion = idAndVersion;
		this.lastTableChangeNumber = lastTableChangeNumber;
	}

	@Override
	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}

	@Override
	public String getCreateOrUpdateIndexSql() {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE IF NOT EXISTS ");
		builder.append(SQLUtils.getTableNameForId(idAndVersion, TableIndexType.INDEX));
		builder.append("( ");
		builder.append(ROW_ID).append(" BIGINT NOT NULL, ");
		builder.append(ROW_VERSION).append(" BIGINT NOT NULL, ");
		builder.append(ROW_SEARCH_CONTENT).append(" MEDIUMTEXT NULL, ");
		builder.append("PRIMARY KEY (").append("ROW_ID").append("), ");
		builder.append("FULLTEXT INDEX `" + ROW_SEARCH_CONTENT + "_INDEX` (" + ROW_SEARCH_CONTENT + ")");
		builder.append(")");
		return builder.toString();
	}

	@Override
	public List<BenefactorDescription> getBenefactors() {
		return Collections.emptyList();
	}

	@Override
	public TableType getTableType() {
		return TableType.recordset;
	}

	@Override
	public List<ColumnToAdd> getColumnNamesToAddToSelect(SqlContext type, boolean includeEtags, boolean isAggregate) {
		if (!SqlContext.query.equals(type)) {
			throw new IllegalArgumentException("Only 'query' is supported for record sets");
		}
		if (isAggregate) {
			return Collections.emptyList();
		}
		return Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION));
	}

	@Override
	public List<IndexDescription> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public Optional<Long> getLastTableChangeNumber() {
		return Optional.ofNullable(lastTableChangeNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idAndVersion, lastTableChangeNumber);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		RecordSetIndexDescription other = (RecordSetIndexDescription) obj;
		return Objects.equals(idAndVersion, other.idAndVersion)
				&& Objects.equals(lastTableChangeNumber, other.lastTableChangeNumber);
	}

	@Override
	public String toString() {
		return "RecordSetIndexDescription [idAndVersion=" + idAndVersion + ", lastTableChangeNumber="
				+ lastTableChangeNumber + "]";
	}
}
