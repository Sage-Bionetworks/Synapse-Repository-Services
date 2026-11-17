package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.JsonObjectSubject;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.schema.AnnotationsTranslator;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;

/**
 * {@link SourceWriter} strategy for an entity-view source. Writes happen
 * <em>in place</em>: changed cells are applied directly to the source rows as
 * annotations. The view's row/column membership is query-driven and cannot be
 * altered by pushing from the copy, so this writer cannot add or remove rows or
 * columns; attempts record an error message that is forwarded to the caller.
 */
public class InPlaceAnnotationSourceWriter implements SourceWriter {

	private final UserInfo user;
	private final AnnotationWriter annotationWriter;
	private final AnnotationsTranslator annotationsTranslator;
	private final GridRowValidator gridRowValidator;
	private final JsonSchema columnSizeSchema;
	private final List<String> errorMessages;

	/**
	 * @param columnSizeSchema the size limits of the view's columns, or null if no
	 *                         column is sized
	 */
	public InPlaceAnnotationSourceWriter(UserInfo user, AnnotationWriter annotationWriter,
			AnnotationsTranslator annotationsTranslator, GridRowValidator gridRowValidator,
			JsonSchema columnSizeSchema) {
		this.user = user;
		this.annotationWriter = annotationWriter;
		this.annotationsTranslator = annotationsTranslator;
		this.gridRowValidator = gridRowValidator;
		this.columnSizeSchema = columnSizeSchema;
		this.errorMessages = new ArrayList<>();
	}

	@Override
	public boolean canAddRemoveRows() {
		return false;
	}

	@Override
	public boolean canAddRemoveColumns() {
		return false;
	}

	@Override
	public void addNewRowToSource(RowSourceItem row) {
		errorMessages.add(String.format("Cannot add the row: '%s' to a source view.", row.getKey()));
	}

	@Override
	public void removeRow(RowSourceItem row) {
		errorMessages.add(String.format("Cannot remove the row: '%s' from a source view.", row.getKey()));
	}

	@Override
	public void addColumnToSource(String columnName) {
		errorMessages.add(String.format("Cannot add the column: '%s' to a source view.", columnName));
	}

	@Override
	public void removeColumn(String columnName) {
		errorMessages.add(String.format("Cannot remove the column: '%s' from a source view.", columnName));
	}

	@Override
	public void applyCellChangesFromCopyToSource(String rowId, Map<String, ConValue> changes) {
		try {
			validateColumnSizes(changes);
			Map<String, AnnotationsValue> changedCells = translateCellChanges(changes);
			annotationWriter.updateChangedAnnotations(user, rowId, changedCells);
		} catch (IllegalArgumentException e) {
			errorMessages.add(String.format("Failed to update row: '%s' in the source view.  Error message: %s", rowId,
					e.getMessage()));
			throw e;
		}
	}

	/**
	 * A value longer than its view column would be written to the entity, then
	 * fail the view once the annotation replicates, and with it every later sync
	 * (PLFM-10141). Rejects the row instead.
	 * 
	 * @param changes the cells to write
	 * @throws IllegalArgumentException if a value exceeds the size of its column
	 */
	void validateColumnSizes(Map<String, ConValue> changes) {
		if (columnSizeSchema == null) {
			return;
		}
		JSONObject subject = new JSONObject();
		changes.forEach((name, cv) -> {
			if (!isCleared(cv)) {
				subject.put(name, cv.getValue());
			}
		});
		if (subject.length() == 0) {
			return;
		}
		ValidationResults results = gridRowValidator
				.validateBatch(columnSizeSchema, List.of(new JsonObjectSubject(subject))).get(0);
		if (!Boolean.TRUE.equals(results.getIsValid())) {
			throw new IllegalArgumentException("A value exceeds the size of its view column: "
					+ String.join(", ", results.getAllValidationMessages()));
		}
	}

	/**
	 * @return true if the cell has no value, so its annotation is removed
	 */
	static boolean isCleared(ConValue cv) {
		return cv == null || ConType.UNDEFINED.equals(cv.getType()) || ConType.NULL.equals(cv.getType())
				|| cv.getValue() == null;
	}

	Map<String, AnnotationsValue> translateCellChanges(Map<String, ConValue> changes) {
		Map<String, AnnotationsValue> changedCells = new HashMap<>();
		for (Map.Entry<String, ConValue> e : changes.entrySet()) {
			ConValue cv = e.getValue();
			if (isCleared(cv)) {
				changedCells.put(e.getKey(), null);
			} else {
				JSONObject json = new JSONObject();
				json.put(e.getKey(), cv.getValue());
				changedCells.put(e.getKey(), annotationsTranslator.getAnnotationValueFromJsonObject(e.getKey(), json));
			}
		}
		return changedCells;
	}

	@Override
	public List<String> getErrorMessages() {
		return errorMessages;
	}

}
