package org.sagebionetworks.repo.model.grid;

import java.util.List;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.CellValueOperator;
import org.sagebionetworks.repo.model.grid.query.Query;
import org.sagebionetworks.repo.model.grid.query.QueryRequest;
import org.sagebionetworks.repo.model.grid.query.RowIdFilter;
import org.sagebionetworks.repo.model.grid.query.RowIsValidFilter;
import org.sagebionetworks.repo.model.grid.query.RowSelectionFilter;
import org.sagebionetworks.repo.model.grid.query.RowValidationResultFilter;
import org.sagebionetworks.repo.model.grid.query.SelectAll;
import org.sagebionetworks.repo.model.grid.query.SelectByName;
import org.sagebionetworks.repo.model.grid.query.SelectSelection;
import org.sagebionetworks.repo.model.grid.query.ValidationOperator;
import org.sagebionetworks.repo.model.grid.query.function.CountStar;
import org.sagebionetworks.repo.model.grid.update.GridUpdateRequest;
import org.sagebionetworks.repo.model.grid.update.LiteralSetValue;
import org.sagebionetworks.repo.model.grid.update.OnMatchFailure;
import org.sagebionetworks.repo.model.grid.update.OnMissingValue;
import org.sagebionetworks.repo.model.grid.update.TemplateSetValue;
import org.sagebionetworks.repo.model.grid.update.Update;
import org.sagebionetworks.repo.model.grid.update.UpdateBatch;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Provides conformance-checked example query and update JSON for the grid agent's
 * instructions. Every example is built from the real request POJOs and serialized via
 * {@link JDOSecondaryPropertyUtils}, so the examples cannot drift from the model.
 * <p>
 * The examples are returned as clean nested JSON (no escaping, no XStream) for use in the
 * Spring AI grid specialist prompts. The legacy Bedrock agent instructions are still
 * produced by {@link GridExampleJson} until the Bedrock agent is removed.
 */
public class GridExamples {

	/**
	 * A single example: a natural-language description paired with the exact request JSON.
	 */
	public static class Example {
		private final String description;
		private final String json;

		Example(String description, String json) {
			this.description = description;
			this.json = json;
		}

		public String getDescription() {
			return description;
		}

		/**
		 * @return the request as clean nested JSON.
		 */
		public String getJson() {
			return json;
		}
	}

	static Example query(String description, QueryRequest request) {
		return new Example(description, JDOSecondaryPropertyUtils.createJSONFromObject(request));
	}

	static Example update(String description, GridUpdateRequest request) {
		return new Example(description, JDOSecondaryPropertyUtils.createJSONFromObject(request));
	}

	/**
	 * @return an ordered list of query examples covering the supported select items,
	 *         filters, and operators.
	 */
	public static List<Example> getQueryExamples() {
		return List.of(
				query("Return up to 10 rows selecting all columns (no filters).",
						new QueryRequest().setQuery(
								new Query().setColumnSelection(List.of(new SelectAll())).setLimit(10L))),

				query("Return up to 10 rows selecting the species and weight columns by name (no filters).",
						new QueryRequest().setQuery(new Query()
								.setColumnSelection(List.of(new SelectByName().setColumnName("species"),
										new SelectByName().setColumnName("weight")))
								.setLimit(10L))),

				query("Return the total number of rows currently in the grid (single count value).",
						new QueryRequest().setQuery(
								new Query().setColumnSelection(List.of(new CountStar())).setLimit(1L))),

				query("Return up to 50 rows selecting all columns where age > 25 AND JSON schema validation is invalid "
						+ "(isValid = false). includeValidationMessages=true provides the detailed error messages "
						+ "needed to diagnose the problem.",
						new QueryRequest().setQuery(new Query().setColumnSelection(List.of(new SelectAll()))
								.setFilters(List.of(
										new CellValueFilter().setColumnName("age")
												.setOperator(CellValueOperator.GREATER_THAN).setValue(25),
										new RowIsValidFilter().setValue(false)))
								.setLimit(50L).setIncludeValidationMessages(true))),

				query("Return up to 5 rows selecting all columns where color is one of \"red\", or \"green\".",
						new QueryRequest().setQuery(new Query().setColumnSelection(List.of(new SelectAll()))
								.setFilters(List.of(new CellValueFilter().setColumnName("color")
										.setOperator(CellValueOperator.IN)
										.setValue(new JSONArray(List.of("red", "green")))))
								.setLimit(5L))),

				query("Count the currently selected rows whose validation error message contains 'expected type' "
						+ "(SQL LIKE pattern using % wildcards).",
						new QueryRequest().setQuery(new Query().setColumnSelection(List.of(new CountStar()))
								.setFilters(List.of(new RowSelectionFilter().setIsSelected(true),
										new RowValidationResultFilter().setOperator(ValidationOperator.LIKE)
												.setValidationResultValue("%expected type%")))
								.setLimit(1L))),

				query("Return up to 10 rows selecting only the columns currently selected by the user.",
						new QueryRequest().setQuery(
								new Query().setColumnSelection(List.of(new SelectSelection())).setLimit(10L))),

				query("Return up to 10 rows where subspecies is undefined and species is not null.",
						new QueryRequest().setQuery(new Query().setColumnSelection(List.of(new SelectAll()))
								.setFilters(List.of(
										new CellValueFilter().setColumnName("subspecies")
												.setOperator(CellValueOperator.IS_UNDEFINED),
										new CellValueFilter().setColumnName("species")
												.setOperator(CellValueOperator.IS_NOT_NULL)))
								.setLimit(10L))));
	}

	/**
	 * @return an ordered list of update examples covering literal and template set values,
	 *         batch updates, and the distinction between an omitted value (undefined) and an
	 *         explicit JSON null.
	 */
	public static List<Example> getUpdateExamples() {
		return List.of(
				update("Set age = 25 for rows where age is currently null.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new LiteralSetValue().setColumnName("age").setValue(25)))
								.setFilters(List.of(new CellValueFilter().setColumnName("age")
										.setOperator(CellValueOperator.IS_NULL))))))),

				update("For rows where height > 12, set type = 'tall' and footing = null (explicit JSON null); "
						+ "cap updates at 10 rows.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new LiteralSetValue().setColumnName("type").setValue("tall"),
										new SetValueWithNull().setColumnName("footing")))
								.setFilters(List.of(new CellValueFilter().setColumnName("height")
										.setOperator(CellValueOperator.GREATER_THAN).setValue(12)))
								.setLimit(10L))))),

				update("Set name = 'Dave' for all currently selected rows.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new LiteralSetValue().setColumnName("name").setValue("Dave")))
								.setFilters(List.of(new RowSelectionFilter().setIsSelected(true))))))),

				update("When asked to update specific rows given their row IDs, use a RowIdFilter. A row ID is the "
						+ "compact `replicaId.sequenceNumber` form a query returns for each row (e.g. 123.456), not a "
						+ "column value. For example, set status = true only for the rows whose IDs are 123.456 and "
						+ "123.789.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new LiteralSetValue().setColumnName("status").setValue(true)))
								.setFilters(List.of(new RowIdFilter().setRowIdsIn(List.of("123.456", "123.789")))))))),

				update("Set color to undefined (omit the value property) for rows where material is null.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new LiteralSetValue().setColumnName("color")))
								.setFilters(List.of(new CellValueFilter().setColumnName("material")
										.setOperator(CellValueOperator.IS_NULL))))))),

				update("Batch update: (1) Set status = 'active' where age > 18, (2) Set category = 'senior' where "
						+ "age >= 65, (3) Set discount = 0.15 for all selected rows.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(
								new Update()
										.setSet(List.of(
												new LiteralSetValue().setColumnName("status").setValue("active")))
										.setFilters(List.of(new CellValueFilter().setColumnName("age")
												.setOperator(CellValueOperator.GREATER_THAN).setValue(18))),
								new Update()
										.setSet(List.of(
												new LiteralSetValue().setColumnName("category").setValue("senior")))
										.setFilters(List.of(new CellValueFilter().setColumnName("age")
												.setOperator(CellValueOperator.GREATER_THAN_OR_EQUALS).setValue(65))),
								new Update()
										.setSet(List.of(
												new LiteralSetValue().setColumnName("discount").setValue(0.15)))
										.setFilters(List.of(new RowSelectionFilter().setIsSelected(true))))))),

				update("Use a template to combine firstName and lastName columns into fullName with a space separator.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new TemplateSetValue().setColumnName("fullName")
										.setSourceTemplate("{firstName} {lastName}")
										.setOnMatchFailure(OnMatchFailure.SET_NULL)))
								.setFilters(List.of(new CellValueFilter().setColumnName("firstName")
										.setOperator(CellValueOperator.IS_NOT_NULL))))))),

				update("Extract domain from email using a regex pattern, treating missing email values as empty "
						+ "strings (which won't match the pattern).",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new TemplateSetValue().setColumnName("domain")
										.setSourceTemplate("{email}").setPattern("^.*@(.+)$")
										.setOnMatchFailure(OnMatchFailure.SET_NULL)
										.setOnMissingValue(OnMissingValue.USE_EMPTY_STRING)))
								.setFilters(List.of(new CellValueFilter().setColumnName("email")
										.setOperator(CellValueOperator.IS_NOT_NULL))))))),

				update("Build a full file path from bucket, folder, and filename columns; skip updating rows where any "
						+ "source column is missing.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new TemplateSetValue().setColumnName("fullPath")
										.setSourceTemplate("{bucket}/{folder}/{filename}")
										.setOnMissingValue(OnMissingValue.SKIP_UPDATE)))
								.setFilters(List.of(new CellValueFilter().setColumnName("fullPath")
										.setOperator(CellValueOperator.IS_NULL))))))),

				update("Reformat phone numbers from '(555) 123-4567' to '555-123-4567' by removing parentheses using "
						+ "regex replacement. This pattern assumes consistent formatting; rows with different formats "
						+ "will trigger onMatchFailure behavior.",
						new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
								.setSet(List.of(new TemplateSetValue().setColumnName("phone")
										.setSourceTemplate("{phone}")
										.setPattern("\\((\\d{3})\\)\\s*(\\d{3}-\\d{4})").setReplacement("$1-$2")
										.setOnMatchFailure(OnMatchFailure.SKIP_UPDATE)))
								.setFilters(List.of(new CellValueFilter().setColumnName("phone")
										.setOperator(CellValueOperator.LIKE).setValue("(%"))))))));
	}

	/**
	 * A {@link LiteralSetValue} that serializes an explicit JSON null for its {@code value}
	 * property. The generated POJO cannot distinguish an omitted value (undefined) from a
	 * null value, so this subclass forces the null to be written to demonstrate the
	 * "store JSON null" semantics in an example.
	 */
	private static class SetValueWithNull extends LiteralSetValue {

		@Override
		public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
			JSONObjectAdapter adapter = super.writeToJSONObject(writeTo);
			if (this.getValue() == null) {
				writeTo.putNull("value");
			}
			return adapter;
		}
	}
}
