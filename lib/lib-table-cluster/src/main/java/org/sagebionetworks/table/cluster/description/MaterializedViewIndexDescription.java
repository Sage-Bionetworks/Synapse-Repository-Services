package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.util.ValidateArgument;

public class MaterializedViewIndexDescription implements IndexDescription {

	private final IdAndVersion idAndVersion;
	private final List<BenefactorDescription> benefactorDescriptions;
	private final List<String> buildColumnsToAddToSelect;
	private final List<IndexDescription> orderedDependencies;

	/**
	 * 
	 * @param idAndVersion The IdAndVersion of this {@link MaterializedView}
	 * @param dependencies Note: The order of this list should match the order of
	 *                     dependencies in the from clause.
	 */
	public MaterializedViewIndexDescription(IdAndVersion idAndVersion, List<IndexDescription> dependencies) {
		super();
		this.idAndVersion = idAndVersion;
		// The order of the provided dependencies is nondeterministic.  By ordering the generated DDL is stable.
		this.orderedDependencies = dependencies.stream().sorted().collect(Collectors.toList());
		this.buildColumnsToAddToSelect = new ArrayList<>();
		this.benefactorDescriptions = new ArrayList<>();
		initializeBenefactors();
	}

	/**
	 * Initialize the benefactors for the select list and dependencies.
	 */
	void initializeBenefactors() {
		for (IndexDescription dependency : this.orderedDependencies) {
			for (BenefactorDescription desc : dependency.getBenefactors()) {
				// The SQL translator will be able to translate from this table name to the appropriate table alias.
				String dependencyTranslatedTableName = SQLUtils.getTableNameForId(dependency.getIdAndVersion(), TableType.INDEX);
				String selectColumnReference = dependencyTranslatedTableName + "." + desc.getBenefactorColumnName();
				String ifNullCheck = String.format("IFNULL( %s , -1)",selectColumnReference);
				buildColumnsToAddToSelect.add(ifNullCheck);
				String newBenefactorColumnName = desc.getBenefactorColumnName() + "_" + dependencyTranslatedTableName;
				benefactorDescriptions
						.add(new BenefactorDescription(newBenefactorColumnName, desc.getBenefactorType()));
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
	public List<String> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag, boolean isAggregate) {
		ValidateArgument.required(context, "SqlContext");
		switch (context) {
		case build:
			if(isAggregate && !buildColumnsToAddToSelect.isEmpty()) {
				throw new IllegalArgumentException(TableConstants.DEFINING_SQL_WITH_GROUP_BY_ERROR);
			}
			return buildColumnsToAddToSelect;
		case query:
			if(isAggregate) {
				return Collections.emptyList();
			}
			return Arrays.asList(ROW_ID, ROW_VERSION);
		default:
			throw new IllegalArgumentException("Unknown context: " + context);
		}
	}

	@Override
	public List<IndexDescription> getDependencies() {
		return orderedDependencies;
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefactorDescriptions, buildColumnsToAddToSelect, idAndVersion, orderedDependencies);
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
				&& Objects.equals(buildColumnsToAddToSelect, other.buildColumnsToAddToSelect)
				&& Objects.equals(idAndVersion, other.idAndVersion)
				&& Objects.equals(orderedDependencies, other.orderedDependencies);
	}

	@Override
	public String toString() {
		return "MaterializedViewIndexDescription [idAndVersion=" + idAndVersion + ", benefactorDescriptions="
				+ benefactorDescriptions + ", buildColumnsToAddToSelect=" + buildColumnsToAddToSelect
				+ ", orderedDependencies=" + orderedDependencies + "]";
	}

}
