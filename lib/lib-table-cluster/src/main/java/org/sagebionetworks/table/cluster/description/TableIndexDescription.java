package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_SEARCH_CONTENT;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableIndexType;
import org.sagebionetworks.table.query.model.SqlContext;

public class TableIndexDescription implements IndexDescription {
	
	private final IdAndVersion idAndVersion;
	
	public TableIndexDescription(IdAndVersion idAndVersion) {
		super();
		this.idAndVersion = idAndVersion;
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
		return TableType.table;
	}
	
	@Override
	public List<ColumnToAdd> getColumnNamesToAddToSelect(SqlContext type, boolean includeEtags, boolean isAggregate) {
		if(!SqlContext.query.equals(type)) {
			throw new IllegalArgumentException("Only 'query' is supported for tables");
		}
		if(isAggregate) {
			return Collections.emptyList();
		}
		return Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION));
	}
	
	@Override
	public List<IndexDescription> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		return Objects.hash(idAndVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TableIndexDescription)) {
			return false;
		}
		TableIndexDescription other = (TableIndexDescription) obj;
		return Objects.equals(idAndVersion, other.idAndVersion);
	}

	@Override
	public String toString() {
		return "TableIndexDescription [idAndVersion=" + idAndVersion + "]";
	}
}
