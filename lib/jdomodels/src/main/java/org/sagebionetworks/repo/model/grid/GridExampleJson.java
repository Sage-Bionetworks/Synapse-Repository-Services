package org.sagebionetworks.repo.model.grid;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
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
 * Utility to prepare example Query and Update JSON for the grid agent's
 * instructions.
 */
public class GridExampleJson {

	static String transformRequestObjectToEscapeObjectString(String originalRequest) {
		/*
		 * Bedrock Agent action groups do not support nested JSON objects in the request
		 * body crafted by the agent. As a workaround, we can instruct the agent to
		 * craft the nested request body properties as escaped JSON strings, which our
		 * JSON parser can handle. For more information, see PLFM-9355.
		 */
		JSONObject jsonObject = new JSONObject(originalRequest);

		// For each top-level JSON property key, if it is an object, transform it to a
		// string (Bedrock Agent action groups do not support nested objects)
		for (String key : jsonObject.keySet()) {
			Object value = jsonObject.get(key);
			if (value instanceof JSONObject) {
				JSONObject nestedObject = (JSONObject) value;
				jsonObject.put(key, nestedObject.toString());
			} else {
				jsonObject.put(key, value);
			}
		}
		return jsonObject.toString();
	}

	public static void main(String[] args) {
		UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(QueryExamples.class, QueryExample.class)
				.alias("query_examples", QueryExamples.class).alias("query_example", QueryExample.class)
				.alias("update_examples", UpdateExamples.class).alias("update_example", UpdateExample.class).build();

		StringWriter writer = new StringWriter();
		X_STREAM.toXML(new QueryExamples().setExamples(
				// Example 1
				new QueryExample().setDescription("Return up to 10 rows selecting all columns (no filters).")
						.setQuery_json(JDOSecondaryPropertyUtils.createJSONFromObject(new QueryRequest()
								.setQuery(new Query().setColumnSelection(List.of(new SelectAll())).setLimit(10L)))),
				// Example 2
				new QueryExample()
						.setDescription(
								"Return up to 10 rows selecting the species and weight columns by name (no filters).")
						.setQuery_json(
								JDOSecondaryPropertyUtils
										.createJSONFromObject(
												new QueryRequest().setQuery(new Query()
														.setColumnSelection(
																List.of(new SelectByName().setColumnName("species"),
																		new SelectByName().setColumnName("weight")))
														.setLimit(10L)))),
				// Example 3
				new QueryExample()
						.setDescription("Return the total number of rows currently in the grid (single count value).")
						.setQuery_json(JDOSecondaryPropertyUtils.createJSONFromObject(new QueryRequest()
								.setQuery(new Query().setColumnSelection(List.of(new CountStar())).setLimit(1L)))),
				// Example 4
				new QueryExample().setDescription(
						"Return up to 50 rows selecting all columns where age > 25 AND JSON schema validation is invalid (isValid = false), includeValidationMessages=true provides the detailed error messages needed to diagnose the problem.")
						.setQuery_json(
								JDOSecondaryPropertyUtils
										.createJSONFromObject(
												new QueryRequest().setQuery(
														new Query().setColumnSelection(List.of(new SelectAll()))
																.setFilters(List.of(
																		new CellValueFilter()
																				.setColumnName("age").setOperator(
																						CellValueOperator.GREATER_THAN)
																				.setValue(25),
																		new RowIsValidFilter().setValue(false)))
																.setLimit(50L).setIncludeValidationMessages(true)))),
				// Example 5
				new QueryExample().setDescription(
						"Return up to 5 rows selecting all columns where color is one of \"red\", or \"green\".")
						.setQuery_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new QueryRequest().setQuery(new Query().setColumnSelection(List.of(new SelectAll()))
										.setFilters(List.of(new CellValueFilter().setColumnName("color")
												.setOperator(CellValueOperator.IN)
												.setValue(new JSONArray(List.of("red", "green")))))
										.setLimit(50L)))),
				// Example 6
				new QueryExample().setDescription(
						"Count the currently selected rows whose validation error message contains 'expected type' (SQL LIKE pattern using % wildcards).")
						.setQuery_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new QueryRequest().setQuery(new Query().setColumnSelection(List.of(new CountStar()))
										.setFilters(List.of(new RowSelectionFilter().setIsSelected(true),
												new RowValidationResultFilter().setOperator(ValidationOperator.LIKE)
														.setValidationResultValue("%expected type%")))
										.setLimit(1L)))),
				// Example 6
				new QueryExample()
						.setDescription(
								"Return up to 10 rows selecting only the columns currently selected by the user.")
						.setQuery_json(JDOSecondaryPropertyUtils.createJSONFromObject(new QueryRequest().setQuery(
								new Query().setColumnSelection(List.of(new SelectSelection())).setLimit(10L)))),
				// Example 7
				new QueryExample()
						.setDescription("Return up to 10 rows where subspecies is undefined and species is not null.")
						// Hard-coded JSON because our auto-generated models do not distinguish between
						// null and undefined
						.setQuery_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new QueryRequest().setQuery(new Query().setColumnSelection(List.of(new SelectAll()))
										.setFilters(List.of(
												new CellValueFilter().setColumnName("subspecies")
														.setOperator(CellValueOperator.IS_UNDEFINED),
												new CellValueFilter().setColumnName("species")
														.setOperator(CellValueOperator.IS_NOT_NULL)))
										.setLimit(10L))))),
				writer);

		System.out.println(writerToString(writer));

		writer = new StringWriter();
		X_STREAM.toXML(new UpdateExamples().setExamples(
				// Update Example 1
				new UpdateExample().setDescription("Set age = 25 for rows where age is currently null.")
						.setUpdate_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
										.setSet(List.of(new LiteralSetValue().setColumnName("age").setValue(25)))
										.setFilters(List.of(new CellValueFilter().setColumnName("age")
												.setOperator(CellValueOperator.IS_NULL)))))))),
				// Update Example 2
				new UpdateExample().setDescription(
						"For rows where height > 12, set type = 'tall' and footing = null; cap updates at 10 rows.")
						.setUpdate_json(
								JDOSecondaryPropertyUtils
										.createJSONFromObject(new GridUpdateRequest().setUpdate(new UpdateBatch()
												.setBatch(List.of(new Update()
														.setSet(List.of(
																new LiteralSetValue().setColumnName("type")
																		.setValue("tall"),
																new SetValueWithNull().setColumnName("footing")))
														.setFilters(
																List.of(new CellValueFilter().setColumnName("height")
																		.setOperator(CellValueOperator.GREATER_THAN)
																		.setValue(12)))
														.setLimit(10L)))))),
				// Update Example 3
				new UpdateExample().setDescription("Set name = 'Dave' for all currently selected rows.")
						.setUpdate_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
										.setSet(List.of(new LiteralSetValue().setColumnName("name").setValue("Dave")))
										.setFilters(List.of(new RowSelectionFilter().setIsSelected(true)))))))),
				// Update Example 4
				new UpdateExample().setDescription(
						"When asked to update specific rows given their row IDs, use a RowIdFilter. A row ID is the compact `replicaId.sequenceNumber` form a query returns for each row (e.g. 123.456), not a column value. For example, set status = true only for the rows whose IDs are 123.456 and 123.789.")
						.setUpdate_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
										.setSet(List.of(new LiteralSetValue().setColumnName("status").setValue(true)))
										.setFilters(List.of(new RowIdFilter().setRowIdsIn(List.of("123.456", "123.789"))))))))),
				// Update Example 5
				new UpdateExample().setDescription("Set color to undefined for rows where material is null.")
						.setUpdate_json(JDOSecondaryPropertyUtils
								.createJSONFromObject(new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List
										.of(new Update().setSet(List.of(new LiteralSetValue().setColumnName("color")))
												.setFilters(List.of(new CellValueFilter().setColumnName("material")
														.setOperator(CellValueOperator.IS_NULL)))))))),
				// Update Example 6
				new UpdateExample().setDescription(
						"Batch update: (1) Set status = 'active' where age > 18, (2) Set category = 'senior' where age >= 65, (3) Set discount = 0.15 for all selected rows.")
						.setUpdate_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(
										// 1
										new Update()
												.setSet(List.of(new LiteralSetValue().setColumnName("status")
														.setValue("active")))
												.setFilters(List.of(new CellValueFilter().setColumnName("age")
														.setOperator(CellValueOperator.GREATER_THAN).setValue(18))),
										// 2
										new Update()
												.setSet(List.of(new LiteralSetValue().setColumnName("category")
														.setValue("senior")))
												.setFilters(List.of(new CellValueFilter().setColumnName("age")
														.setOperator(CellValueOperator.GREATER_THAN_OR_EQUALS)
														.setValue(65))),
										// 3
										new Update()
												.setSet(List.of(
														new LiteralSetValue().setColumnName("discount").setValue(0.15)))
												.setFilters(List.of(new RowSelectionFilter().setIsSelected(true)))))))),
				// Update Example 7
				new UpdateExample().setDescription(
						"Use a template to combine firstName and lastName columns into fullName with a space separator.")
						.setUpdate_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
										.setSet(List.of(new TemplateSetValue().setColumnName("fullName")
												.setSourceTemplate("{firstName} {lastName}")
												.setOnMatchFailure(OnMatchFailure.SET_NULL)))
										.setFilters(List.of(new CellValueFilter().setColumnName("firstName")
												.setOperator(CellValueOperator.IS_NOT_NULL)))))))),
				// Update Example 8
				new UpdateExample().setDescription(
						"Extract domain from email using regex pattern, treating missing email values as empty strings (which won't match the pattern)")
						.setUpdate_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
										.setSet(List.of(new TemplateSetValue().setColumnName("domain")
												.setSourceTemplate("{email}").setPattern("@(.+)$")
												.setOnMatchFailure(OnMatchFailure.SET_NULL)
												.setOnMissingValue(OnMissingValue.USE_EMPTY_STRING)))
										.setFilters(List.of(new CellValueFilter().setColumnName("email")
												.setOperator(CellValueOperator.IS_NOT_NULL)))))))),
				// Update Example 9
				new UpdateExample().setDescription(
						"Build full file path from bucket, folder, and filename columns; skip updating rows where any source column is missing.")
						.setUpdate_json(
								JDOSecondaryPropertyUtils
										.createJSONFromObject(
												new GridUpdateRequest()
														.setUpdate(new UpdateBatch().setBatch(List.of(new Update()
																.setSet(List.of(new TemplateSetValue()
																		.setColumnName("fullPath")
																		.setSourceTemplate(
																				"{bucket}/{folder}/{filename}")
																		.setOnMissingValue(OnMissingValue.SKIP_UPDATE)))
																.setFilters(List.of(new CellValueFilter()
																		.setColumnName("fullPath")
																		.setOperator(CellValueOperator.IS_NULL)))))))),
				// Update Example 10
				new UpdateExample().setDescription(
						"Reformat phone numbers from '(555) 123-4567' to '555-123-4567' by removing parentheses using regex replacement.  Note: This pattern assumes consistent formatting; rows with different formats will trigger onMatchFailure behavior. ")
						.setUpdate_json(JDOSecondaryPropertyUtils.createJSONFromObject(
								new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
										.setSet(List.of(new TemplateSetValue().setColumnName("phone")
												.setSourceTemplate("{phone}")
												.setPattern("\\((\\d{3})\\)\\s*(\\d{3}-\\d{4})").setReplacement("$1-$2")
												.setOnMatchFailure(OnMatchFailure.SKIP_UPDATE)))
										.setFilters(List.of(new CellValueFilter().setColumnName("phone")
												.setOperator(CellValueOperator.LIKE).setValue("(%"))))))))

		// end
		), writer);

		System.out.println(writerToString(writer));
	}

	static String writerToString(Writer writer) {
		return writer.toString().replaceAll("&quot;", "\"").replaceAll("&apos;", "'").replaceAll("&gt;", ">");
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
			this.query_json = transformRequestObjectToEscapeObjectString(query_json);
			return this;
		}
	}

	public static class UpdateExamples {
		private UpdateExample[] examples;

		public UpdateExample[] getExamples() {
			return examples;
		}

		public UpdateExamples setExamples(UpdateExample... examples) {
			this.examples = examples;
			return this;
		}
	}

	public static class UpdateExample {
		private String description;
		private String update_json;

		public String getDescription() {
			return description;
		}

		public UpdateExample setDescription(String description) {
			this.description = description;
			return this;
		}

		public String getUpdate_json() {
			return update_json;
		}

		public UpdateExample setUpdate_json(String update_json) {
			this.update_json = transformRequestObjectToEscapeObjectString(update_json);
			return this;
		}
	}

	private static class SetValueWithNull extends LiteralSetValue {

		@Override
		public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
			JSONObjectAdapter a = super.writeToJSONObject(writeTo);
			if (this.getValue() == null) {
				writeTo.putNull("value");
			}
			return a;
		}

	}

}
