package org.sagebionetworks.web.client.presenter;

import java.util.LinkedHashSet;
import java.util.List;

import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.view.ColumnsPopupView;
import org.sagebionetworks.web.shared.ColumnsForType;
import org.sagebionetworks.web.shared.HeaderData;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class ColumnsPopupPresenter implements ColumnsPopupView.Presenter {
	
	
	private ColumnsPopupView view;
	private String type;
	private SearchServiceAsync service;
	List<HeaderData> defaultColumns;
	List<HeaderData> additionalColumns;
	LinkedHashSet<String> selection = new LinkedHashSet<String>();
	private List<String> inSelection;
	private ColumnSelectionChangeListener listner;

	@Inject
	public ColumnsPopupPresenter(SearchServiceAsync searchService, ColumnsPopupView columnPopupView){
		this.service = searchService;
		this.view = columnPopupView;	
		this.view.setPresenter(this);
	}

	@Override
	public void applySelectedColumns() {
		// Save the selection as a cookie
		
		
	}

	@Override
	public void setColumnSelected(String id, boolean selected) {
		if(selected){
			this.selection.add(id);
		}else{
			this.selection.remove(id);
		}

	}

	@Override
	public void showPopup(String type, List<String> selected, ColumnSelectionChangeListener listner) {
		// Start with a clean selection
		this.selection.clear();
		this.type = type;
		this.inSelection = selected;
		this.listner = listner;
		// First get the types from the server
		this.service.getColumnsForType(this.type, new AsyncCallback<ColumnsForType>() {
			
			@Override
			public void onSuccess(ColumnsForType result) {
				setColumnsFromServer(result);
				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showError(caught.getMessage());
				view.hide();
			}
		});
	}

	protected void setColumnsFromServer(ColumnsForType result) {
		// Build the selection model
		// First determine if we have a cookie
		// Prepare the column selection model
		defaultColumns = result.getDefaultColumns();
		additionalColumns = result.getAdditionalColumns();
		// If we were passed an input selection then use it
		if(this.inSelection != null && this.inSelection.size() > 0){
			for(String id: this.inSelection){
				this.selection.add(id);
			}
		}else{
			// All of the defaults are selected
			for(HeaderData header: defaultColumns){
				this.selection.add(header.getId());
			}
		}
		// set the columns on the view
		view.setColumns(defaultColumns, additionalColumns);
		// Now show the view
		view.show();
	}

	@Override
	public boolean isSelected(String name) {
		return selection.contains(name);
	}
	

}
