package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelMapper;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SelectColumnAndModel;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.model.*;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.ValidateArgument;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
	 * @param isAggregatedResult
	 * @return
	 */
	public static ColumnMapper getSelectColumns(SelectList selectList, LinkedHashMap<String, ColumnModel> columnNameToModelMap,
			boolean isAggregatedResult) {
		ValidateArgument.required(columnNameToModelMap, "all columns");
		ValidateArgument.required(selectList, "selectList");
		if (selectList.getAsterisk() != null) {
			// All of the columns will be returned.
			return TableModelUtils.createColumnMapperFromColumnModels(columnNameToModelMap, isAggregatedResult);
		} else {
			List<SelectColumnAndModel> selectColumnModels = Lists.newArrayListWithCapacity(selectList.getColumns().size());
			for (DerivedColumn dc : selectList.getColumns()) {
				SelectColumn selectColumn = new SelectColumn();
				if (dc.getAsClause() != null) {
					selectColumn.setName(dc.getAsClause().toString());
				} else {
					ToUnquotedStringVisitor visitor = new ToUnquotedStringVisitor();
					dc.getValueExpression().doVisit(visitor);
					selectColumn.setName(visitor.getSql());
				}
				ColumnModel columnModel = null;
				ValueExpressionPrimary primary = getValueExpressionPrimary(dc.getValueExpression());
				if (primary != null && primary.getColumnReference() != null) {
					String key = getStringValueOf(primary.getColumnReference().getNameRHS());
					columnModel = columnNameToModelMap.get(key);
					if (columnModel != null) {
						selectColumn.setColumnType(columnModel.getColumnType());
						if (!isAggregatedResult) {
							// we only want to refer to the column model for non-aggregated results
							selectColumn.setId(columnModel.getId());
						}
					}
				}
				selectColumnModels.add(TableModelUtils.createSelectColumnAndModel(selectColumn, columnModel));
			}
			return TableModelUtils.createColumnMapper(selectColumnModels);
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
