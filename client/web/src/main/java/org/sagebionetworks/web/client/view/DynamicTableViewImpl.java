package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.shared.HeaderData;

import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DynamicTableViewImpl extends Composite implements DynamicTableView {

	public interface Binder extends UiBinder<Widget, DynamicTableViewImpl> {}

	// To pass the cell table the CSS, we need to call a constructor
	// so we are providing the instance of this class
	@UiField
	CellTable<Map<String, Object>> cellTable;
	@UiField
	SimplePager pager;
	ImagePrototypeSingleton prototype;

	// How many columns are we currently rendering
	int columnCount = 0;

	// Any column that is sortable will have sortable header.
	private List<SortableHeader> sortableHeaders = new ArrayList<SortableHeader>();
	private Presenter presenter;
	private ColumnFactory columnFactory;

	/**
	 * Gin will inject all of the params.
	 * 
	 * @param cellTableResource
	 */
	@Inject
	public DynamicTableViewImpl(final Binder uiBinder,ImagePrototypeSingleton prototype,ColumnFactory columnFactory) {
		// Create the tables
//		cellTable = new CellTable<Map<String, Object>>(10, cellTableResource);
		// Use the xml script to load the rest of the view.
		initWidget(uiBinder.createAndBindUi(this));
		this.prototype = prototype;
		this.columnFactory = columnFactory;
	}
	
	private SortableHeader createHeader(String display, String key){
		final SortableHeader header = new SortableHeader(display, prototype, key);
		sortableHeaders.add(header);
		header.setUpdater(new ValueUpdater<String>() {
			@Override
			public void update(String value) {
				presenter.toggleSort(header.getColumnKey());
			}
		});
		return header;
	}

	/**
	 * Remove all columns from the table.
	 */
	private void removeAllColumns() {
		for (int i = 0; i < columnCount; i++) {
			cellTable.removeColumn(i);
		}
		// Clear all header data
		sortableHeaders.clear();
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void setRows(RowData data) {
		// update the table
		cellTable.setRowCount(data.getTotalCount(), true);
		// Push the data into the widget.
		cellTable.setRowData(data.getOffset(), data.getRows());
		cellTable.setPageSize(data.getLimit());
		cellTable.setPageStart(data.getOffset());
		// Update the sorting
		updateSortColumns(data.getSortKey(), data.isAscending());
	}
	
	public void updateSortColumns(String sortKey, boolean ascending){
		// Set the sorting state
		for(SortableHeader header: sortableHeaders){
			// If the sort key is null then turn off sorting
			if(sortKey != null && sortKey.equals(header.getColumnKey())){
				header.setSorting(true);
				header.setSortAscending(ascending);
			}else{
				header.setSorting(false);
			}
		}
		// Need to re-draw the headers
		cellTable.redrawHeaders();
	}

	@Override
	public void showMessage(String message) {
		Window.alert(message);
	}

	@Override
	public void setColumns(List<HeaderData> list) {
		removeAllColumns();
		// Now add each column from
		for(int i=0; i<list.size(); i++){
			HeaderData meta = list.get(i);
			// Now create the column.
			Column<Map<String, Object>, ?> column = columnFactory.createColumn(meta);
			// Add the column to the table;
			// Is this column sortable
			boolean isSortable = true; // at the moment all columns are sortable.
			if(isSortable){
				// the header is a sortable object
				SortableHeader header = createHeader(meta.getDisplayName(), meta.getId());
				cellTable.addColumn(column, header);
			}else{
				// The header is a string
				cellTable.addColumn(column, meta.getDisplayName());
			}
		}
		// Keep the column count
		columnCount = list.size();
	}

}
