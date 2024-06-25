package org.sagebionetworks.table.cluster.description;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_SEARCH_CONTENT;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.SQLUtils.TableIndexType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.ValidateArgument;

public class MaterializedViewIndexDescription implements IndexDescription {

	private final IdAndVersion idAndVersion;
	private final List<BenefactorDescription> benefactorDescriptions;
	private final List<ColumnToAdd> buildColumnsToAddToSelect;
	private final List<TableDependency> orderedDependencies;
	private final String definingSql;
	
	/**
	 * 
	 * @param idAndVersion
	 * @param definingSql
	 * @param lookup
	 */
	public MaterializedViewIndexDescription(IdAndVersion idAndVersion, String definingSql, IndexDescriptionLookup lookup) {
		super();
		this.definingSql = definingSql;
		this.idAndVersion = idAndVersion;
		int index = 0;
		this.orderedDependencies = new ArrayList<>();
		for (TableNameCorrelation table : TableModelUtils.getQuerySpecification(definingSql)
				.createIterable(TableNameCorrelation.class)) {
			orderedDependencies.add(new TableDependency()
					.withIndexDescription(lookup.getIndexDescription(IdAndVersion.parse(table.getTableName().toSql())))
					.withIndexAlias(SQLUtils.getTableAliasForIndex(index))
					.withTableAlias(table.getTableAlias().orElse(null)));
			index++;
		}
		this.buildColumnsToAddToSelect = new ArrayList<>();
		this.benefactorDescriptions = new ArrayList<>();
		initializeBenefactors();
	}

	/**
	 * Initialize the benefactors for the select list and dependencies.
	 */
	void initializeBenefactors() {
		for (TableDependency dependency : this.orderedDependencies) {
			for (BenefactorDescription desc : dependency.getIndexDescription().getBenefactors()) {
				// The SQL translator will be able to translate from this table name to the
				// appropriate table alias.
				String dependencyTranslatedTableName = dependency.getTableAlias().isPresent()
						? dependency.getTableAlias().get()
						: SQLUtils.getTableNameForId(dependency.getIndexDescription().getIdAndVersion(),
								TableIndexType.INDEX);

				String selectColumnReference = dependencyTranslatedTableName + "." + desc.getBenefactorColumnName();
				String ifNullCheck = String.format("IFNULL( %s , -1)", selectColumnReference);
				buildColumnsToAddToSelect.add(new ColumnToAdd(dependency.getIndexDescription().getIdAndVersion(), ifNullCheck));
				String newBenefactorColumnName = desc.getBenefactorColumnName() + "_" + dependency.getIndexAlias();
				benefactorDescriptions
						.add(new BenefactorDescription(newBenefactorColumnName.toUpperCase(), desc.getBenefactorType()));
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
		builder.append(SQLUtils.getTableNameForId(idAndVersion, TableIndexType.INDEX));
		builder.append("( ");
		builder.append(ROW_ID).append(" BIGINT NOT NULL AUTO_INCREMENT, ");
		builder.append(ROW_VERSION).append(" BIGINT NOT NULL DEFAULT 0, ");
		builder.append(ROW_SEARCH_CONTENT).append(" MEDIUMTEXT NULL, ");
		StringBuilder benefactorIndicies = new StringBuilder();
		for (BenefactorDescription desc : benefactorDescriptions) {
			builder.append(desc.getBenefactorColumnName()).append(" BIGINT NOT NULL, ");
			benefactorIndicies.append(", KEY (").append(desc.getBenefactorColumnName()).append(")");
		}
		builder.append("PRIMARY KEY (").append("ROW_ID").append("), ");
		builder.append("FULLTEXT INDEX `" + ROW_SEARCH_CONTENT + "_INDEX` (" + ROW_SEARCH_CONTENT + ")");
		builder.append(benefactorIndicies.toString());
		builder.append(")");
		return builder.toString();
	}

	@Override
	public List<BenefactorDescription> getBenefactors() {
		return benefactorDescriptions;
	}

	@Override
	public TableType getTableType() {
		return TableType.materializedview;
	}

	@Override
	public List<ColumnToAdd> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag, boolean isAggregate) {
		ValidateArgument.required(context, "SqlContext");
		switch (context) {
		case build:
			if (isAggregate && !buildColumnsToAddToSelect.isEmpty()) {
				throw new IllegalArgumentException(TableConstants.DEFINING_SQL_WITH_GROUP_BY_ERROR);
			}
			return buildColumnsToAddToSelect;
		case query:
			if (isAggregate) {
				return Collections.emptyList();
			}
			return Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION));
		default:
			throw new IllegalArgumentException("Unknown context: " + context);
		}
	}

	@Override
	public List<IndexDescription> getDependencies() {
		return orderedDependencies.stream().map(d->d.getIndexDescription()).collect(Collectors.toList());
	}
	
	public String getDefiningSql(){
		return this.definingSql;
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
