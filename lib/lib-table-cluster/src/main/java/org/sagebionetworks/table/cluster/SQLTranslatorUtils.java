package org.sagebionetworks.table.cluster;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SelectColumnAndModel;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.model.CharacterFactor;
import org.sagebionetworks.table.query.model.CharacterPrimary;
import org.sagebionetworks.table.query.model.CharacterValueExpression;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.Factor;
import org.sagebionetworks.table.query.model.Identifier;
import org.sagebionetworks.table.query.model.MysqlFunction;
import org.sagebionetworks.table.query.model.NumericPrimary;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.NumericValueFunction;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.StringValueExpression;
import org.sagebionetworks.table.query.model.Term;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;
import org.sagebionetworks.table.query.model.visitors.ColumnTypeVisitor;
import org.sagebionetworks.table.query.model.visitors.ToTranslatedSqlVisitor;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.collect.Lists;

/**
 * Helper methods to translate table SQL queries.
 * 
 * @author jmhill
 *
 */
public class SQLTranslatorUtils {

	/**
	 * Translate the passed query model into output SQL.
	 * 
	 * @param model The model representing a query.
	 * @param outputBuilder
	 * @param parameters
	 */
	public static String translate(QuerySpecification model, final Map<String, Object> parameters,
			final Map<String, ColumnModel> columnNameToModelMap) {
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		model.doVisit(visitor);
		return visitor.getSql();
	}

	/**
	 * Get the list of column IDs that are referenced in the select calsue.
	 * 
	 * @param allColumns
	 * @param selectList
	 * @param isAggregatedResult
	 * @return
	 */
	public static ColumnMapper getSelectColumns(SelectList selectList, LinkedHashMap<String, ColumnModel> columnNameToModelMap,
			boolean isAggregatedResult) {
		ValidateArgument.required(columnNameToModelMap, "all columns");
		ValidateArgument.required(selectList, "selectList");
		if (selectList.getAsterisk() != null) {
			throw new IllegalStateException("The columns should have been expanded before getting here");
		} else {
			List<SelectColumnAndModel> selectColumnModels = Lists.newArrayListWithCapacity(selectList.getColumns().size());
			for (DerivedColumn dc : selectList.getColumns()) {
				SelectColumn selectColumn = new SelectColumn();
				selectColumn.setName(dc.getColumnName());

				ColumnTypeVisitor visitor = new ColumnTypeVisitor(columnNameToModelMap, isAggregatedResult);
				dc.doVisit(visitor);
				ColumnModel columnModel = visitor.getColumnModel();
				selectColumn.setColumnType(visitor.getColumnType());
				if (columnModel != null) {
					selectColumn.setId(columnModel.getId());
				}
				selectColumnModels.add(TableModelUtils.createSelectColumnAndModel(selectColumn, columnModel));
			}
			return TableModelUtils.createColumnMapper(selectColumnModels);
		}
	}

	public static DerivedColumn createDerivedColumn(String columnName) {
		return new DerivedColumn(new ValueExpression(new StringValueExpression(new CharacterValueExpression(new CharacterFactor(
				new CharacterPrimary(new ValueExpressionPrimary(new ColumnReference(null, new ColumnName(new Identifier(
						SqlElementUntils.createActualIdentifier(columnName)))))))))), null);
	}

	public static DerivedColumn createDerivedColumn(MysqlFunction mysqlFunction) {
		return new DerivedColumn(new ValueExpression(new NumericValueExpression(new Term(new Factor(new NumericPrimary(
				new NumericValueFunction(mysqlFunction)))))), null);
	}
}
