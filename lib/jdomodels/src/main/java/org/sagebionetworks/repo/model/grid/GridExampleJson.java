package org.sagebionetworks.repo.model.grid;

import java.io.StringWriter;
import java.util.List;

import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.CellValueOperator;
import org.sagebionetworks.repo.model.grid.query.Query;
import org.sagebionetworks.repo.model.grid.query.RowIsValidFilter;
import org.sagebionetworks.repo.model.grid.query.RowSelectionFilter;
import org.sagebionetworks.repo.model.grid.query.RowValidationResultFilter;
import org.sagebionetworks.repo.model.grid.query.SelectAll;
import org.sagebionetworks.repo.model.grid.query.ValidationOperator;
import org.sagebionetworks.repo.model.grid.query.function.CountStar;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

/**
 * This utility is used to prepare examples of the grid Query JSON for the grid
 * agent's instructions.
 */
public class GridExampleJson {

	public static void main(String[] args) {
		CellValueFilter cell = new CellValueFilter().setColumnName("someInt").setValue(List.of(12L))
				.setOperator(CellValueOperator.GREATER_THAN);
		System.out.println(JDOSecondaryPropertyUtils.createJSONFromObject(cell));

		RowIsValidFilter rowIsValid = new RowIsValidFilter().setValue(false);
		System.out.println(JDOSecondaryPropertyUtils.createJSONFromObject(rowIsValid));

		RowValidationResultFilter val2 = new RowValidationResultFilter().setOperator(ValidationOperator.LIKE)
				.setValidationResultValue("%expected type%");
		System.out.println(JDOSecondaryPropertyUtils.createJSONFromObject(val2));
		RowSelectionFilter sel = new RowSelectionFilter().setIsSelected(true);
		System.out.println(JDOSecondaryPropertyUtils.createJSONFromObject(sel));

		Query query = new Query().setColumnSelection(List.of(new CountStar()))
				.setFilters(
						List.of(new CellValueFilter().setColumnName("age").setOperator(CellValueOperator.GREATER_THAN)
								.setValue(List.of(25L)), new RowIsValidFilter().setValue(false)))
				.setLimit(1L);
		System.out.println(JDOSecondaryPropertyUtils.createJSONFromObject(query));

		query = new Query().setColumnSelection(List.of(new SelectAll()))
				.setFilters(
						List.of(new CellValueFilter().setColumnName("age").setOperator(CellValueOperator.GREATER_THAN)
								.setValue(List.of(25L)), new RowIsValidFilter().setValue(false)))
				.setLimit(1L);
		System.out.println(JDOSecondaryPropertyUtils.createJSONFromObject(query));

		System.out.println(JDOSecondaryPropertyUtils.createJSONFromObject(query));

		UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(QueryExamples.class, QueryExample.class)
				.alias("query_examples", QueryExamples.class).alias("query_example", QueryExample.class).build();
		StringWriter writer = new StringWriter();
		X_STREAM.toXML(new QueryExamples().setExamples(
				//
				new QueryExample().setDescription("Select all columns and all rows with a limit of 10")
						.setQuery_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new Query().setColumnSelection(List.of(new SelectAll())).setLimit(10L))),
				//
				new QueryExample().setDescription("Count the number for rows that are in the grid")
						.setQuery_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new Query().setColumnSelection(List.of(new CountStar())).setLimit(1L))),
				//
				new QueryExample().setDescription(
						"Select all columns for rows where age is greater than 25 that also have invalid JSON schema validation results where 'isValid' is false.")
						.setQuery_json(JDOSecondaryPropertyUtils
								.createJSONFromObject(new Query().setColumnSelection(List.of(new SelectAll()))
										.setFilters(List.of(new CellValueFilter()
												.setColumnName("age").setOperator(CellValueOperator.GREATER_THAN)
												.setValue(List.of(25)), new RowIsValidFilter().setValue(false)))
										.setLimit(50L))),
				new QueryExample().setDescription(
						"Count the number of rows from the user's currently selected rows that have a JSON schema type validation error message containing the the phrase :'expected type'")
						.setQuery_json(JDOSecondaryPropertyUtils
								.createJSONFromObject(new Query().setColumnSelection(List.of(new CountStar()))
										.setFilters(List.of(new RowSelectionFilter().setIsSelected(true),
												new RowValidationResultFilter().setOperator(ValidationOperator.LIKE)
														.setValidationResultValue("%expected type%")))
										.setLimit(1L)))
		//
		//
		), writer);
		String unquoted = writer.toString().replaceAll("&quot;", "\"").replaceAll("&apos;", "'");
		System.out.print(unquoted);

	}

	public static class QueryExamples {
		private QueryExample[] examples;

		public QueryExample[] getExamples() {
			return examples;
		}

		public QueryExamples setExamples(QueryExample... examples) {
			this.examples = examples;
			return this;
		}

	}

	public static class QueryExample {
		private String description;
		private String query_json;

		public String getDescription() {
			return description;
		}

		public QueryExample setDescription(String description) {
			this.description = description;
			return this;
		}

		public String getQuery_json() {
			return query_json;
		}

		public QueryExample setQuery_json(String query_json) {
			this.query_json = query_json;
			return this;
		}

	}

}
