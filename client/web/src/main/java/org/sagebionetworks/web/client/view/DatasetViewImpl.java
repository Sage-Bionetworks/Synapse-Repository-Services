package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.presenter.DatasetRow;

import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DatasetViewImpl extends Composite implements DatasetView {

	public interface Binder extends UiBinder<Widget, DatasetViewImpl> {
	}

	@UiField
	SpanElement titleSpan;
	@UiField
	SpanElement descriptionSpan;
	@UiField
	FlexTable middleFlexTable;
	@UiField
	FlexTable rightFlexTable;

	private Presenter presenter;

	@Inject
	public DatasetViewImpl(Binder uiBinder) {
		initWidget(uiBinder.createAndBindUi(this));

	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showErrorMessage(String message) {
		Window.alert(message);
	}

	@Override
	public void setDatasetRow(DatasetRow row) {
		// Clear everything
		clearAllFields();
		titleSpan.setInnerText(row.getName());
		descriptionSpan.setInnerHTML(row.getDesc1ription());
		// First row
		int rowIndex = 0;
		addRowToTable(rowIndex++, "Disease(s):", "Aging", middleFlexTable);
		// Second row
		addRowToTable(rowIndex++, "Species:", "Human, Mouse", middleFlexTable);
		// Third
		addRowToTable(rowIndex++, "Study size:", "200", middleFlexTable);
		// Forth
		addRowToTable(rowIndex++, "Tissue type(s):", "Brain", middleFlexTable);

		// Now fill out the right
		rowIndex = 0;
		// Fill in the right from the datast
		if(row.getCreatedOn() != null){
			addRowToTable(rowIndex++, "Posted:", DateTimeFormat.getMediumDateTimeFormat().format(row.getCreatedOn()), rightFlexTable);			
		}
		if(row.getModifiedColumn() != null){
			addRowToTable(rowIndex++, "Modified:", DateTimeFormat.getMediumDateTimeFormat().format(row.getModifiedColumn()), rightFlexTable);			
		}
		addRowToTable(rowIndex++, "Creator:", row.getCreator(), rightFlexTable);
		addRowToTable(rowIndex++, "Status:", row.getStatus(), rightFlexTable);

	}

	/**
	 * Add a row to the provided FlexTable.
	 * 
	 * @param key
	 * @param value
	 * @param table
	 */
	private static void addRowToTable(int row, String key, String value, FlexTable table) {
		table.setText(row, 0, key);
		table.getCellFormatter().addStyleName(row, 0, "boldRight");
		table.setText(row, 1, value);
	}

	private void clearAllFields() {
		titleSpan.setInnerText("");
		descriptionSpan.setInnerHTML("");
		middleFlexTable.clear();
		rightFlexTable.clear();
	}

}
