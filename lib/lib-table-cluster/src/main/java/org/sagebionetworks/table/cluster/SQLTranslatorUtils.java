package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.model.*;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Helper methods to translate table SQL queries.
 * 
 * @author jmhill
 *
 */
public class SQLTranslatorUtils {

	private static final Function<ColumnModel, Long> MODEL_TO_ID = new Function<ColumnModel, Long>() {
		@Override
		public Long apply(ColumnModel input) {
			return Long.parseLong(input.getId());
		}
	};
	
	/**
	 * Translate the passed query model into output SQL.
	 * @param model The model representing a query.
	 * @param outputBuilder
	 * @param parameters
	 */
	public static boolean translate(QuerySpecification model, StringBuilder builder, final Map<String, Object> parameters,
			final Map<String, ColumnModel> columnNameToModelMap) {
		boolean hasGrouping = hasGrouping(model.getTableExpression());
		boolean isAggregated = model.getSelectList().isAggregate();
		
		if (!hasGrouping && !isAggregated) {
			// we need to add the row count and row version colums
			SelectList selectList = model.getSelectList();
			if (!BooleanUtils.isTrue(selectList.getAsterisk())) {
				List<DerivedColumn> selectColumns = Lists.newArrayListWithCapacity(selectList.getColumns().size());
				selectColumns.addAll(selectList.getColumns());
				selectColumns.add(SQLTranslatorUtils.createDerivedColumn(ROW_ID));
				selectColumns.add(SQLTranslatorUtils.createDerivedColumn(ROW_VERSION));
				selectList = new SelectList(selectColumns);
			}
			model = new QuerySpecification(model.getSetQuantifier(), selectList, model.getTableExpression());
		}

		SQLElement.ColumnConvertor columnConvertor = createColumnConvertor(parameters, columnNameToModelMap);
		model.toSQL(builder, columnConvertor);
		return isAggregated || hasGrouping;
	}

	public static SQLElement.ColumnConvertor createColumnConvertor(final Map<String, Object> parameters,
			final Map<String, ColumnModel> columnNameToModelMap) {
		SQLElement.ColumnConvertor columnConvertor = new SQLElement.ColumnConvertor() {
			private ColumnModel columnModelLHS = null;
			private Set<String> asColumns = Sets.newHashSet();
			private String currentTableName = null;
			private SQLClause currentClause = null;

			@Override
			public void convertTableName(String tableName, StringBuilder builder) {
				Long tableId = KeyFactory.stringToKey(tableName);
				currentTableName = SQLUtils.TABLE_PREFIX + tableId;
				builder.append(currentTableName);
			}
			
			@Override
			public void convertColumn(ColumnReference columnReference, StringBuilder builder) {
				String columnName = getStringValueOf(columnReference.getNameRHS());
				// Is this a reserved column name like ROW_ID or ROW_VERSION?
				if (TableConstants.isReservedColumnName(columnName)) {
					// use the returned reserve name in destination SQL.
					builder.append(columnName.toUpperCase());
				} else {
					// Not a reserved column name.
					// Lookup the ID for this column
					columnName = columnName.trim();
					ColumnModel column = columnNameToModelMap.get(columnName);
					if (column == null) {
						if (asColumns.contains(columnName)) {
							builder.append(columnName);
						} else {
							throw new IllegalArgumentException("Unknown column name: " + columnName);
						}
					} else {
						String subName = "";
						if (columnReference.getNameLHS() != null) {
							subName = getStringValueOf(columnReference.getNameLHS());
							// Remove double quotes if they are included.
							subName = subName.replaceAll("\"", "") + "_";
						}
						switch ( column.getColumnType()){
						case DOUBLE:
							SQLUtils.appendDoubleCase(column, subName, currentTableName, currentClause == SQLClause.SELECT, builder);
							break;
						default:
							builder.append(subName);
							SQLUtils.appendColumnName(column, builder);
							break;
						}
					}
				}
			}

			@Override
			public void addAsColumn(ColumnName columnName) {
				asColumns.add(getStringValueOf(columnName));
			}

			@Override
			public void setLHSColumn(ColumnReference columnReferenceLHS) {
				if (columnReferenceLHS == null) {
					this.columnModelLHS = null;
				} else {
					String columnName = getStringValueOf(columnReferenceLHS.getNameRHS());
					// Is this a reserved column name like ROW_ID or ROW_VERSION?
					if (TableConstants.isReservedColumnName(columnName)) {
						this.columnModelLHS = null;
					} else {
						// Not a reserved column name.
						// Lookup the ID for this column
						this.columnModelLHS = columnNameToModelMap.get(columnName.trim());
					}
				}
			}

			@Override
			public void convertParam(Number param, StringBuilder builder) {
				String bindKey = "b" + parameters.size();
				builder.append(":").append(bindKey);
				parameters.put(bindKey, param);
			}

			@Override
			public void convertNumberParam(String param, StringBuilder builder) {
				String bindKey = "b" + parameters.size();
				builder.append(":").append(bindKey);
				parameters.put(bindKey, param);
			}

			@Override
			public void convertParam(String value, StringBuilder builder) {
				if (columnModelLHS != null) {
					switch (columnModelLHS.getColumnType()) {
					case DATE:
						value = Long.toString(TimeUtils.parseSqlDate(value));
						break;
					default:
						break;
					}
				}
				String bindKey = "b" + parameters.size();
				builder.append(":").append(bindKey);
				parameters.put(bindKey, value);
			}

			@Override
			public void setCurrentClause(SQLClause clause) {
				if (clause != null && currentClause != null) {
					throw new IllegalStateException("Clauses cannot be nested");
				}
				currentClause = clause;
			}
		};
		return columnConvertor;
	}

	/**
	 * Get the string value of a ColumnName.
	 * @param columnName
	 * @return
	 */
	public static String getStringValueOf(ColumnName columnName){
		if(columnName == null) throw new IllegalArgumentException("ColumName cannot be null");
		return getStringValueOf(columnName.getIdentifier());
	}
	
	/**
	 * Get the string value of an Identifier.
	 * @param identifier
	 * @return
	 */
	public static String getStringValueOf(Identifier identifier){
		if(identifier == null) throw new IllegalArgumentException("Identifier cannot be null");
		return getStringValueOf(identifier.getActualIdentifier());
	}
	
	/**
	 * Get the string value of an ActualIdentifier.
	 * @param actualIdentifier
	 * @return
	 */
	public static String getStringValueOf(ActualIdentifier actualIdentifier) {
		if(actualIdentifier == null) throw new IllegalArgumentException("ActualIdentifier cannot be null");
		if(actualIdentifier.getDelimitedIdentifier() != null){
			return actualIdentifier.getDelimitedIdentifier();
		}else{
			return actualIdentifier.getRegularIdentifier();
		}
	}



	/**
	 * Get a ValueExpressionPrimary from a ValueExpression
	 * @param valueExpression
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(DerivedColumn derivedColumn){
		if(derivedColumn == null) throw new IllegalArgumentException("DerivedColumn cannot be null");
		return getValueExpressionPrimary(derivedColumn.getValueExpression());
	}
	
	/**
	 * Get a ValueExpressionPrimary from a ValueExpression
	 * @param valueExpression
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(ValueExpression valueExpression){
		ValidateArgument.required(valueExpression, "valueExpression");
		if (valueExpression.getStringValueExpression() != null) {
			return getValueExpressionPrimary(valueExpression.getStringValueExpression());
		} else if (valueExpression.getNumericValueExpression() != null) {
			NumericValueExpression numericValueExpression = valueExpression.getNumericValueExpression();
			Term term = numericValueExpression.getTerm();
			ValidateArgument.required(term, "term");
			Factor factor = term.getFactor();
			ValidateArgument.required(factor, "factor");
			NumericPrimary numericPrimary = factor.getNumericPrimary();
			ValidateArgument.required(numericPrimary, "numericPrimary");
			if (numericPrimary.getValueExpressionPrimary() != null) {
				return numericPrimary.getValueExpressionPrimary();
			} else {
				return null;
			}
		} else {
			throw new IllegalArgumentException(
					"Either ValueExpression.stringValueExpression or ValueExpression.numericValueExpression is required");
		}
	}
	/**
	 * Get a ValueExpressionPrimary from a StringValueExpression
	 * @param stringValueExpression
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(StringValueExpression stringValueExpression){
		if(stringValueExpression == null) throw new IllegalArgumentException("StringValueExpression cannot be null");
		return getValueExpressionPrimary(stringValueExpression.getCharacterValueExpression());
	}

	/**
	 * Get a ValueExpressionPrimary from a CharacterValueExpression
	 * @param characterValueExpression
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(
			CharacterValueExpression characterValueExpression) {
		if(characterValueExpression == null) throw new IllegalArgumentException("CharacterValueExpression cannot be null");
		return getValueExpressionPrimary(characterValueExpression.getCharacterFactor());
	}

	/**
	 * Get a ValueExpressionPrimary from a CharacterFactor
	 * @param characterFactor
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(
			CharacterFactor characterFactor) {
		if(characterFactor == null) throw new IllegalArgumentException("CharacterFactor cannot be null");
		return getValueExpressionPrimary(characterFactor.getCharacterPrimary());
	}

	/**
	 * Get a ValueExpressionPrimary from a CharacterPrimary
	 * @param characterPrimary
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(
			CharacterPrimary characterPrimary) {
		if(characterPrimary == null) throw new IllegalArgumentException("CharacterPrimary cannot be null");
		return characterPrimary.getValueExpressionPrimary();
	}
	
	/**
	 * Get a ValueExpressionPrimary from a Pattern.
	 * @param pattern
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(Pattern pattern) {
		if(pattern == null) throw new IllegalArgumentException("Pattern cannot be null");
		return getValueExpressionPrimary(pattern.getCharacterValueExpression());
	}
	
	/**
	 * Get a ValueExpressionPrimary from an EscapeCharacter.
	 * @param pattern
	 * @return
	 */
	public static ValueExpressionPrimary getValueExpressionPrimary(EscapeCharacter escape) {
		if(escape == null) throw new IllegalArgumentException("EscapeCharacter cannot be null");
		return getValueExpressionPrimary(escape.getCharacterValueExpression());
	}

	static boolean hasGrouping(TableExpression tableExpression) {
		ValidateArgument.required(tableExpression, "TableExpression");
		boolean hasGrouping = false;
		if (tableExpression.getGroupByClause() != null) {
			hasGrouping = true;
		}
		return hasGrouping;
	}

	/**
	 * Get the string value from SignedValueSpecification
	 * 
	 * @param signedValueSpecification
	 * @return
	 */
	public static String getStringValueOf(SignedValueSpecification signedValueSpecification) {
		if (signedValueSpecification == null)
			throw new IllegalArgumentException("SignedValueSpecification cannot be null");
		return getStringValueOf(signedValueSpecification.getSignedLiteral());
	}

	/**
	 * Get the string value from SignedLiteral
	 * 
	 * @param signedLiteral
	 * @return
	 */
	public static String getStringValueOf(SignedLiteral signedLiteral) {
		if (signedLiteral.getGeneralLiteral() != null)
			return signedLiteral.getGeneralLiteral();
		if (signedLiteral.getSignedNumericLiteral() != null)
			return signedLiteral.getSignedNumericLiteral().toString();
		throw new IllegalArgumentException("SignedLiteral must have either a GeneralLiteral or SignedNumericLiteral");
	}
	
	/**
	 * Get the list of column IDs that are referenced in the select calsue.
	 * 
	 * @param allColumns
	 * @param selectList
	 * @return
	 */
	public static List<ColumnModel> getSelectColumns(SelectList selectList, Map<String, ColumnModel> columnNameToModelMap) {
		if (columnNameToModelMap == null)
			throw new IllegalArgumentException("All columns cannot be null");
		if(selectList == null) throw new IllegalArgumentException();
		if(selectList.getAsterisk() != null){
			// All of the columns will be returned.
			return new ArrayList<ColumnModel>(columnNameToModelMap.values());
		}else{
			List<ColumnModel> selectColumnModels = new LinkedList<ColumnModel>();
			for(DerivedColumn dc: selectList.getColumns()){
				ValueExpressionPrimary primary = getValueExpressionPrimary(dc.getValueExpression());
				if (primary != null && primary.getColumnReference() != null) {
					String key = getStringValueOf(primary.getColumnReference().getNameRHS());
					ColumnModel column = columnNameToModelMap.get(key);
					if (column != null) {
						selectColumnModels.add(column);
					}
				}
			}
			return selectColumnModels;
		}
	}

	public static DerivedColumn createDerivedColumn(String columnName) {
		return new DerivedColumn(new ValueExpression(new StringValueExpression(new CharacterValueExpression(new CharacterFactor(
				new CharacterPrimary(new ValueExpressionPrimary(new ColumnReference(null, new ColumnName(new Identifier(new ActualIdentifier(
						columnName, null)))))))))), null);
	}

	public static DerivedColumn createDerivedColumn(MysqlFunction mysqlFunction) {
		return new DerivedColumn(new ValueExpression(new NumericValueExpression(new Term(new Factor(new NumericPrimary(
				new NumericValueFunction(mysqlFunction)))))), null);
	}
}
