package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.view.table.ColumnFactory;
import org.sagebionetworks.web.shared.HeaderData;

import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RangeChangeEvent.Handler;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.inject.Inject;

public class DynamicTableViewImpl extends Composite implements DynamicTableView {

	@UiTemplate("DynamicTableViewImpl.ui.xml")
	public interface Binder extends UiBinder<Widget, DynamicTableViewImpl> {}

	@UiField
	SimplePanel tablePanel;
	
	CellTable<Map<String, Object>> cellTable;
	
	ImagePrototypeSingleton prototype;

	// How many columns are we currently rendering
	int columnCount = 0;

	// Any column that is sortable will have sortable header.
	private List<SortableHeader> sortableHeaders = new ArrayList<SortableHeader>();
	private Presenter presenter;
	private ColumnFactory columnFactory;
	private CellTableProvider tableProvider;
	// These two lists of handlers come from the outside.
	// Each time we create a new CellTable we need to ensure that handlers
	// are removed from the old table and registered with the new table.
	private List<RangeChangeHandlerWrapper> rangeEventHandlerList = new ArrayList<RangeChangeHandlerWrapper>();
	private List<RowCountChangeHandlerWrapper> rowCountEventHandlerList = new ArrayList<RowCountChangeHandlerWrapper>();
	
	/**
	 * Gin will inject all of the params.
	 * 
	 * @param cellTableResource
	 */
	@Inject
	public DynamicTableViewImpl(final Binder uiBinder,ImagePrototypeSingleton prototype,ColumnFactory columnFactory, CellTableProvider provider) {
		// Use the xml script to load the rest of the view.
		initWidget(uiBinder.createAndBindUi(this));
		this.prototype = prototype;
		this.columnFactory = columnFactory;
		this.tableProvider = provider;
		removeAllColumns();
	}
	
	/**
	 * Public for testing
	 * @param display
	 * @param key
	 * @return
	 */
	public SortableHeader createHeader(String display, String key){
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
		if(cellTable != null){
			cellTable.removeFromParent();
		}
		// Create the tables
		cellTable = tableProvider.createNewTable();
		tablePanel.add(cellTable);
		
		// Add all of the range handlers to the new table
		for(RangeChangeHandlerWrapper wrapped: rangeEventHandlerList){
			wrapped.removeFromOldAndAddToNewListner(cellTable);
		}
		// Add all of the row count handlers to the new table.
		for(RowCountChangeHandlerWrapper wrapped: rowCountEventHandlerList){
			wrapped.removeFromOldAndAddToNewListner(cellTable);
		}

		// The pager will trigger these
		cellTable.addRangeChangeHandler(new Handler() {
			@Override
			public void onRangeChange(RangeChangeEvent event) {
				Range newRange = event.getNewRange();
				presenter.pageTo(newRange.getStart(), newRange.getLength());
			}
		});
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
			String sortId = meta.getSortId();
			if(sortId != null){
				// the header is a sortable object
				SortableHeader header = createHeader(meta.getDisplayName(), sortId);
				cellTable.addColumn(column, header);
			}else{
				// The header is a string
				cellTable.addColumn(column, meta.getDisplayName());
			}
		}
		// Keep the column count
		columnCount = list.size();
	}
	
	
	public int getColumnCount(){
		return columnCount;
	}
	

	/**
	 * The following methods are from com.google.gwt.view.client.HasRows
	 */
	@Override
	public HandlerRegistration addRangeChangeHandler(RangeChangeEvent.Handler handler) {
		// Add this handler to the list
		RangeChangeHandlerWrapper wrapper = new RangeChangeHandlerWrapper(cellTable, handler);
		rangeEventHandlerList.add(wrapper);
		return wrapper.getRegistration();
	}

	@Override
	public HandlerRegistration addRowCountChangeHandler(RowCountChangeEvent.Handler handler) {
		// Add this handler to the list.
		RowCountChangeHandlerWrapper wrapper =new RowCountChangeHandlerWrapper(cellTable, handler);
		rowCountEventHandlerList.add(wrapper);
		return wrapper.getRegistration();
	}

	@Override
	public int getRowCount() {
		// Pass along to the current table
		return cellTable.getRowCount();
	}

	@Override
	public Range getVisibleRange() {
		// Pass along to the current table
		return cellTable.getVisibleRange();
	}

	@Override
	public boolean isRowCountExact() {
		// Pass along to the current table
		return cellTable.isRowCountExact();
	}

	@Override
	public void setRowCount(int count) {
		// Pass along to the current table
		cellTable.setRowCount(count);
	}

	@Override
	public void setRowCount(int count, boolean isExact) {
		// Pass along to the current table
		cellTable.setRowCount(count, isExact);
	}

	@Override
	public void setVisibleRange(int start, int length) {
		// Pass along to the current table
		cellTable.setVisibleRange(start, length);
	}

	@Override
	public void setVisibleRange(Range range) {
		// Pass along to the current table
		cellTable.setVisibleRange(range);
	}


}
