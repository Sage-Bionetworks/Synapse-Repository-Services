package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;

public class MaterializedViewIndexDescription implements IndexDescription {

	private final IdAndVersion idAndVersion;
	private final List<BenefactorDescription> benefactorDescriptions;

	/**
	 * 
	 * @param idAndVersion The IdAndVersion of this {@link MaterializedView} 
	 * @param dependencies Note: The order of this list should match the order of
	 *                     dependencies in the from clause.
	 */
	public MaterializedViewIndexDescription(IdAndVersion idAndVersion, List<IndexDescription> dependencies) {
		super();
		this.idAndVersion = idAndVersion;
		this.benefactorDescriptions = new ArrayList<>();
		for (int i = 0; i < dependencies.size(); i++) {
			IndexDescription dependency = dependencies.get(i);
			for (BenefactorDescription desc : dependency.getBenefactors()) {
				String tableAlias = SQLUtils.getTableAliasForIndex(i);
				String newBenefactorColumnName = desc.getBenefactorColumnName() + tableAlias;
				benefactorDescriptions.add(new BenefactorDescription(newBenefactorColumnName, desc.getBenefactorType()));
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
		for (BenefactorDescription desc : benefactorDescriptions) {
			builder.append(desc.getBenefactorColumnName()).append(" BIGINT NOT NULL, ");
			benefactorIndicies.append(", KEY (").append(desc.getBenefactorColumnName()).append(")");
		}
		builder.append("PRIMARY KEY (").append("ROW_ID").append(")");
		builder.append(benefactorIndicies.toString());
		builder.append(")");
		return builder.toString();
	}

	@Override
	public List<BenefactorDescription> getBenefactors() {
		return benefactorDescriptions;
	}

	@Override
	public EntityType getTableType() {
		return EntityType.materializedview;
	}

	@Override
	public boolean isEtagColumnIncluded() {
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactorDescriptions, idAndVersion);
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
		return Objects.equals(benefactorDescriptions, other.benefactorDescriptions)
				&& Objects.equals(idAndVersion, other.idAndVersion);
	}

	@Override
	public String toString() {
		return "MaterializedViewIndexDescription [idAndVersion=" + idAndVersion + ", benefactorDescriptions="
				+ benefactorDescriptions + "]";
	}
	
}
