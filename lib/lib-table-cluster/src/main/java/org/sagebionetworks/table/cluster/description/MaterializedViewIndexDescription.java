package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;

public class MaterializedViewIndexDescription implements IndexDescription {

	private final IdAndVersion idAndVersion;
	private final List<String> benefactorNames;

	/**
	 * 
	 * @param idAndVersion The IdAndVersion of this {@link MaterializedView} 
	 * @param dependencies Note: The order of this list should match the order of
	 *                     dependencies in the from clause.
	 */
	public MaterializedViewIndexDescription(IdAndVersion idAndVersion, List<IndexDescription> dependencies) {
		super();
		this.idAndVersion = idAndVersion;
		this.benefactorNames = new ArrayList<>();
		for (int i = 0; i < dependencies.size(); i++) {
			IndexDescription dependency = dependencies.get(i);
			for (String benefactorName : dependency.getBenefactorColumnNames()) {
				String tableAlias = SQLUtils.getTableAliasForIndex(i);
				benefactorNames.add(benefactorName + tableAlias);
			}
		}
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
		builder.append(ROW_ID).append(" BIGINT NOT NULL AUTO_INCREMENT, ");
		builder.append(ROW_VERSION).append(" BIGINT NOT NULL DEFAULT 0, ");
		StringBuilder benefactorIndicies = new StringBuilder();
		for (String benefactorNames : benefactorNames) {
			builder.append(benefactorNames).append(" BIGINT NOT NULL, ");
			benefactorIndicies.append(", KEY (").append(benefactorNames).append(")");
		}
		builder.append("PRIMARY KEY (").append("ROW_ID").append(")");
		builder.append(benefactorIndicies.toString());
		builder.append(")");
		return builder.toString();
	}

	@Override
	public List<String> getBenefactorColumnNames() {
		return benefactorNames;
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactorNames, idAndVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MaterializedViewIndexDescription)) {
			return false;
		}
		MaterializedViewIndexDescription other = (MaterializedViewIndexDescription) obj;
		return Objects.equals(benefactorNames, other.benefactorNames)
				&& Objects.equals(idAndVersion, other.idAndVersion);
	}

}
