package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;

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
		builder.append(SQLUtils.getTableNameForId(idAndVersion, TableType.INDEX));
		builder.append("( ");
		builder.append(ROW_ID).append(" BIGINT NOT NULL, ");
		builder.append(ROW_VERSION).append(" BIGINT NOT NULL, ");
		builder.append("PRIMARY KEY (").append("ROW_ID").append(")");
		builder.append(")");
		return builder.toString();
	}

	@Override
	public List<BenefactorDescription> getBenefactorColumnNames() {
		return Collections.emptyList();
	}
	
	@Override
	public EntityType getTableType() {
		return EntityType.table;
	}

	@Override
	public boolean isEtagColumnIncluded() {
		return false;
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
