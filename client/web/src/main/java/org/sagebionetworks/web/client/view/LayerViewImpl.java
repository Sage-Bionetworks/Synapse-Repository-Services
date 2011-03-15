package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.presenter.LayerRow;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableView;

import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LayerViewImpl extends Composite implements LayerView {

	private final int DESCRIPTION_SUMMARY_LENGTH = 50; // characters for summary

	public interface Binder extends UiBinder<Widget, LayerViewImpl> {
	}

	@UiField
	FlowPanel overviewPanel;
	@UiField
	SpanElement titleSpan;
	@UiField
	FlexTable middleFlexTable;
	@UiField
	FlexTable rightFlexTable;
	@UiField 
	SimplePanel previewTablePanel;	

	private Presenter presenter;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private QueryServiceTableView dynamicTableView;

	@Inject
	public LayerViewImpl(Binder uiBinder, final PreviewDisclosurePanel previewDisclosurePanel) {
		initWidget(uiBinder.createAndBindUi(this));
		this.previewDisclosurePanel = previewDisclosurePanel;
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
	public void setLayerRow(LayerRow row) {
		// Clear everything
		clearAllFields();
		titleSpan.setInnerText(row.getName());

		// set descriptions
		String description = row.getDescription();
		int summaryLength = description.length() >= DESCRIPTION_SUMMARY_LENGTH ? DESCRIPTION_SUMMARY_LENGTH
				: description.length();
		previewDisclosurePanel.init("Expand",
				description.substring(0, summaryLength), description);
		overviewPanel.add(previewDisclosurePanel);

		// fake junk metadata:
		
		// First row
		int rowIndex = 0;
		addRowToTable(rowIndex++, "Disease(s):", "Aging", middleFlexTable);
		// Second row
		addRowToTable(rowIndex++, "Species:", "Human, Mouse", middleFlexTable);
		// Third
		addRowToTable(rowIndex++, "Study size:", "200", middleFlexTable);
		// Forth
		addRowToTable(rowIndex++, "Tissue type(s):", "Brain", middleFlexTable);


	}

	/**
	 * Add a row to the provided FlexTable.
	 * 
	 * @param key
	 * @param value
	 * @param table
	 */
	private static void addRowToTable(int row, String key, String value,
			FlexTable table) {
		table.setText(row, 0, key);
		table.getCellFormatter().addStyleName(row, 0, "boldRight");
		table.setText(row, 1, value);
	}

	private void clearAllFields() {
		titleSpan.setInnerText("");
		middleFlexTable.clear();
		rightFlexTable.clear();
	}

}
