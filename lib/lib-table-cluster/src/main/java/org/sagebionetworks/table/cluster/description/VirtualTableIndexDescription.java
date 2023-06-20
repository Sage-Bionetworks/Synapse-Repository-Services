package org.sagebionetworks.table.cluster.description;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.ValidateArgument;

public class VirtualTableIndexDescription implements IndexDescription {

	private final IdAndVersion idAndVersion;
	private final String definingSql;
	private final List<IndexDescription> sources;

	public VirtualTableIndexDescription(IdAndVersion idAndVersion, String definingSql, IndexDescriptionLookup lookup) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(definingSql, "definingSql");
		ValidateArgument.required(lookup, "IndexDescriptionLookup");

		this.idAndVersion = idAndVersion;
		this.definingSql = definingSql;
		try {
			List<IdAndVersion> sourceIds = new TableQueryParser(definingSql).queryExpression()
					.stream(TableNameCorrelation.class).map((tnc) -> IdAndVersion.parse(tnc.toSql()))
					.collect(Collectors.toList());
			if(sourceIds.size() != 1) {
				throw new IllegalArgumentException(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE);
			}
			sourceIds.stream().filter(s -> s.equals(idAndVersion)).findFirst().ifPresent(s -> {
				throw new IllegalArgumentException("Defining SQL cannot reference itself");
			});
			sources = sourceIds.stream().map((id) -> lookup.getIndexDescription(id)).collect(Collectors.toList());
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public IdAndVersion getIdAndVersion() {
		return idAndVersion;
	}

	@Override
	public TableType getTableType() {
		return TableType.virtualtable;
	}

	@Override
	public String getCreateOrUpdateIndexSql() {
		throw new UnsupportedOperationException("Cannot create or update a VirtualTable index");
	}

	@Override
	public List<BenefactorDescription> getBenefactors() {
		return Collections.emptyList();
	}

	@Override
	public List<ColumnToAdd> getColumnNamesToAddToSelect(SqlContext context, boolean includeEtag, boolean isAggregate) {
		return Collections.emptyList();
	}

	@Override
	public List<IndexDescription> getDependencies() {
		return sources;
	}

	@Override
	public String preprocessQuery(String sql) {
		return String.format("WITH %s AS (%s) %s", idAndVersion.toString(), definingSql, sql);
	}

}
