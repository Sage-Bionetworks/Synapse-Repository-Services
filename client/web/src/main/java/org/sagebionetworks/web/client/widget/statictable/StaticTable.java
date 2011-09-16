package org.sagebionetworks.web.client.widget.statictable;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.widget.statictable.StaticTableView.StaticSelectionMode;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class StaticTable implements StaticTableView.Presenter {
	
	private StaticTableView view;	
	
	@Inject
	public StaticTable(StaticTableView view) {
		this.view = view;
		view.setPresenter(this);
	}

	public void clear() {
		this.view.clear();
	}
	
	public void setDataAndColumnsInOrder(List<Map<String,String>> rows, List<StaticTableColumn> columnsInOrder) {
		this.view.setDataAndColumnsInOrder(rows, columnsInOrder);
	}
	
	public void setDataAndColumnOrder(List<Map<String,String>> rows, List<String> columnOrder) {
		this.view.setDataAndColumnOrder(rows, columnOrder);
	}
	
	public void setData(List<Map<String,String>> rows) {
		this.view.setData(rows);
	}
	
	public void setColumnOrder(List<String> columnOrder) {
		this.view.setColumnOrder(columnOrder);
	}
	
	public void setDimensions(int width, int height) {
		this.view.setDimensions(width, height);
	}
	
	public void setTitle(String title) {
		this.view.setTitleText(title);
	}
	
	public void setSelectionMode(StaticSelectionMode selectionMode) {
		this.view.setSelectionMode(selectionMode);
	}
	
	public Widget asWidget() {
		view.setPresenter(this);
		return view.asWidget();
	}	
	
	public void addSelectionListener(SelectionChangedListener<BaseModelData> listener) {
		this.view.addSelectionListener(listener);
	}

	public String getSelectedColumn() {
		return this.view.getSelectedColumn();
	}

}
