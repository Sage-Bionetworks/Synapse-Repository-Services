package org.sagebionetworks.table.cluster.resolution;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.table.cluster.SQLTranslatorUtils;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;
import org.sagebionetworks.table.query.model.WithListElement;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Resolves the columns of a parsed, untranslated query to the columns they read by name and correlation,
 * never by position. For example, in
 * {@code select a.foo as f, b.bar as b, 100 as num from syn123 a join syn456 b on (a.id = b.id)} the
 * column {@code f} reads syn123.foo, {@code b} reads syn456.bar, and {@code num} is a literal that reads
 * no column.
 * <p>
 * A VirtualTable's defining SQL inlined as the single element of the query's WITH list is resolved
 * through: a reference to one of the inlined object's columns carries, as its definition, the select
 * column of the defining SQL that produces it.
 * <p>
 * The query must not be translated while it is being resolved, because translation renames the tables and
 * columns that resolution matches on. Each {@link ResolvedColumn#getElement()} is the query's own select
 * column, so a caller may resolve first and then act on the same elements after translation.
 */
public class ColumnSourceResolver {

	private final QuerySpecification outerQuery;
	private final Map<QuerySpecification, TableAndColumnMapper> mappers;
	private final IdAndVersion inlinedObjectId;
	private final QuerySpecification inlinedQuery;
	private final Map<String, List<ResolvedColumn>> inlinedColumnsByName;

	/**
	 * @param model          the parsed query. It must not have been translated.
	 * @param schemaProvider resolves the schema of each object the query reads.
	 * @throws IllegalArgumentException if the query has more than one WITH element.
	 */
	public ColumnSourceResolver(QueryExpression model, SchemaProvider schemaProvider) {
		ValidateArgument.required(model, "model");
		ValidateArgument.required(schemaProvider, "schemaProvider");
		WithListElement inlined = model.getWithListElements().map(ColumnSourceResolver::getSingleElement).orElse(null);
		this.outerQuery = model.getNonJoinQueryExpression().getFirstElementOfType(QuerySpecification.class);
		// Elements compare by their SQL text, so two identical query parts would collide in a regular map.
		this.mappers = new IdentityHashMap<>();
		model.stream(QuerySpecification.class)
				.forEach(query -> mappers.put(query, new TableAndColumnMapper(query, schemaProvider)));

		if (inlined == null) {
			this.inlinedObjectId = null;
			this.inlinedQuery = null;
			this.inlinedColumnsByName = Map.of();
		} else {
			this.inlinedObjectId = IdAndVersion.parse(inlined.getIdentifier().toSql());
			this.inlinedQuery = inlined.getFirstElementOfType(QuerySpecification.class);
			this.inlinedColumnsByName = resolveInlinedColumns();
		}
	}

	private static WithListElement getSingleElement(List<WithListElement> elements) {
		if (elements.size() != 1) {
			throw new IllegalArgumentException("A CTE must have one and only one inner query.");
		}
		return elements.get(0);
	}

	/**
	 * The outer query reads the inlined object's bound columns by name, and each bound column is named for
	 * the defining SQL's select column that produces it, so the name is the pairing key.
	 */
	private Map<String, List<ResolvedColumn>> resolveInlinedColumns() {
		TableAndColumnMapper mapper = getMapper(inlinedQuery);
		// A 'select *' carries no select columns of its own, so resolve the columns it expands to.
		List<DerivedColumn> columns = BooleanUtils.isTrue(inlinedQuery.getSelectList().getAsterisk())
				? mapper.buildSelectAllColumns().getColumns()
				: inlinedQuery.getSelectList().getColumns();
		Map<String, List<ResolvedColumn>> byName = new LinkedHashMap<>();
		for (DerivedColumn column : columns) {
			ResolvedColumn resolved = resolve(column, mapper);
			byName.computeIfAbsent(resolved.getOutputName(), name -> new ArrayList<>()).add(resolved);
		}
		return byName;
	}

	/**
	 * @return the first query specification of the query's non-join expression: the query that produces
	 *         the results.
	 */
	public QuerySpecification getOuterQuery() {
		return outerQuery;
	}

	/**
	 * @return the id of the object whose defining SQL is inlined as the query's single WITH element, or
	 *         empty when nothing is inlined.
	 */
	public Optional<IdAndVersion> getInlinedObjectId() {
		return Optional.ofNullable(inlinedObjectId);
	}

	/**
	 * @return the inlined defining SQL, or empty when nothing is inlined.
	 */
	public Optional<QuerySpecification> getInlinedQuery() {
		return Optional.ofNullable(inlinedQuery);
	}

	/**
	 * @return the select columns of the inlined defining SQL, or an empty list when nothing is inlined.
	 */
	public List<ResolvedColumn> getInlinedColumns() {
		return inlinedColumnsByName.values().stream().flatMap(List::stream).collect(Collectors.toList());
	}

	/**
	 * @param name the name of one of the inlined object's columns.
	 * @return the select column of the inlined defining SQL that produces the named column, or empty when
	 *         no select column produces it.
	 * @throws IllegalStateException if more than one select column produces the name, since the bound
	 *                               schema of the inlined object cannot have two columns of that name.
	 */
	public Optional<ResolvedColumn> getInlinedColumn(String name) {
		ValidateArgument.required(name, "name");
		List<ResolvedColumn> matches = inlinedColumnsByName.getOrDefault(name, List.of());
		if (matches.size() > 1) {
			throw new IllegalStateException(
					"The defining SQL of " + inlinedObjectId + " produces more than one column named '" + name + "'");
		}
		return matches.stream().findFirst();
	}

	/**
	 * @param query one of the query specifications of the resolved query.
	 * @return the mapper that resolves the column references of the given query specification.
	 */
	public TableAndColumnMapper getMapper(QuerySpecification query) {
		ValidateArgument.required(query, "query");
		TableAndColumnMapper mapper = mappers.get(query);
		if (mapper == null) {
			throw new IllegalArgumentException("The query specification is not part of the resolved query");
		}
		return mapper;
	}

	/**
	 * @param column a select column of one of the query specifications of the resolved query.
	 * @return how the column's value is derived and the columns it reads.
	 */
	public ResolvedColumn resolve(DerivedColumn column) {
		ValidateArgument.required(column, "column");
		return resolve(column, getMapper(getQuerySpecification(column)));
	}

	/**
	 * @param reference a column reference anywhere in the resolved query.
	 * @return the column the reference reads, or empty when it matches no column of the tables it can read.
	 * @throws IllegalStateException if the reference reads a column of the inlined object that its defining
	 *                               SQL does not produce exactly once.
	 */
	public Optional<ColumnSource> resolve(ColumnReference reference) {
		ValidateArgument.required(reference, "reference");
		return resolve(reference, getMapper(getQuerySpecification(reference)));
	}

	/**
	 * @param source a column read by the query.
	 * @return the base columns the given column is computed from: the column itself when its table's
	 *         defining SQL is not inlined, otherwise the base columns of every input of its definition.
	 */
	public List<ColumnSource> getLeafSources(ColumnSource source) {
		ValidateArgument.required(source, "source");
		if (source.getDefinition().isEmpty()) {
			return List.of(source);
		}
		LinkedHashSet<ColumnSource> leaves = new LinkedHashSet<>();
		for (ColumnSource input : source.getDefinition().get().getInputs()) {
			leaves.addAll(getLeafSources(input));
		}
		return new ArrayList<>(leaves);
	}

	private static QuerySpecification getQuerySpecification(org.sagebionetworks.table.query.model.Element element) {
		return element.getContext(QuerySpecification.class).orElseThrow(
				() -> new IllegalArgumentException("The element is not part of a query: " + element.toSql()));
	}

	private Optional<ColumnSource> resolve(ColumnReference reference, TableAndColumnMapper mapper) {
		return mapper.lookupColumnReferenceMatch(reference).map(match -> {
			IdAndVersion table = match.getTableInfo().getTableIdAndVersion();
			ResolvedColumn definition = null;
			// The defining SQL reads the inlined object's sources, never the inlined object itself, so only a
			// reference outside the defining SQL can read one of its columns.
			if (table.equals(inlinedObjectId) && mapper != mappers.get(inlinedQuery)
					&& match.getColumnTranslationReference() instanceof SchemaColumnTranslationReference) {
				String name = match.getColumnTranslationReference().getUserQueryColumnName();
				definition = getInlinedColumn(name).orElseThrow(() -> new IllegalStateException(
						"The defining SQL of " + inlinedObjectId + " does not produce the column '" + name + "'"));
			}
			return new ColumnSource(table, match.getColumnTranslationReference(), definition);
		});
	}

	private ResolvedColumn resolve(DerivedColumn column, TableAndColumnMapper mapper) {
		return resolve(column, mapper, reference -> resolve(reference, mapper));
	}

	/**
	 * Resolve a select column of a single query specification without following any inlined defining SQL,
	 * so no input carries a definition.
	 *
	 * @param column a select column of the query specification the mapper was built from.
	 * @param mapper resolves the column references of that query specification.
	 * @return how the column's value is derived and the columns it reads.
	 */
	public static ResolvedColumn resolveWithoutInlining(DerivedColumn column, TableAndColumnMapper mapper) {
		ValidateArgument.required(column, "column");
		ValidateArgument.required(mapper, "mapper");
		return resolve(column, mapper, reference -> mapper.lookupColumnReferenceMatch(reference)
				.map(match -> new ColumnSource(match.getTableInfo().getTableIdAndVersion(),
						match.getColumnTranslationReference(), null)));
	}

	private static ResolvedColumn resolve(DerivedColumn column, TableAndColumnMapper mapper,
			Function<ColumnReference, Optional<ColumnSource>> referenceResolver) {
		SetFunctionSpecification setFunction = column.getFirstElementOfType(SetFunctionSpecification.class);
		List<ColumnReference> references = column.stream(ColumnReference.class).collect(Collectors.toList());
		DerivationKind kind = classify(column, setFunction, references);
		SetFunctionType setFunctionType = setFunction == null ? null : setFunction.getSetFunctionType();
		boolean wholeCount = SQLTranslatorUtils.isWholeCount(column);

		// A row metadata column (e.g. ROW_ID) is not a column of any schema, so it is not an input.
		LinkedHashSet<ColumnSource> inputs = new LinkedHashSet<>();
		for (ColumnReference reference : references) {
			referenceResolver.apply(reference)
					.filter(source -> source.getReference() instanceof SchemaColumnTranslationReference)
					.ifPresent(inputs::add);
		}
		String outputName = SQLTranslatorUtils.getSelectColumns(column, mapper).getName();
		return new ResolvedColumn(column, outputName, kind, setFunctionType, wholeCount, new ArrayList<>(inputs));
	}

	/**
	 * An aggregate anywhere in the value makes it an aggregate. A value that reads exactly one column,
	 * unchanged by any function, cast, or arithmetic, is an identity of that column; an 'as' alias renames
	 * the output but does not change the value.
	 */
	private static DerivationKind classify(DerivedColumn column, SetFunctionSpecification setFunction,
			List<ColumnReference> references) {
		if (setFunction != null) {
			return DerivationKind.AGGREGATE;
		}
		if (references.isEmpty()) {
			return DerivationKind.LITERAL;
		}
		if (references.size() == 1 && column.getValueExpression().toSql().equals(references.get(0).toSql())) {
			return DerivationKind.IDENTITY;
		}
		return DerivationKind.EXPRESSION;
	}

}
