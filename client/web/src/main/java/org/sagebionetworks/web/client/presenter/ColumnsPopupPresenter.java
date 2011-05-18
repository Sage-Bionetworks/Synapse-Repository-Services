package org.sagebionetworks.web.client.presenter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.view.ColumnsPopupView;
import org.sagebionetworks.web.shared.ColumnsForType;
import org.sagebionetworks.web.shared.HeaderData;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

/**
 * The presenter for the popup dialog used to select visible columns for a given type.
 * 
 * @author jmhill
 *
 */
public class ColumnsPopupPresenter implements ColumnsPopupView.Presenter {
	
	
	private ColumnsPopupView view;
	private String type;
	private SearchServiceAsync service;
	List<HeaderData> defaultColumns;
	List<HeaderData> additionalColumns;
	LinkedHashSet<String> selection = new LinkedHashSet<String>();
	private List<String> inSelection;
	private ColumnSelectionChangeListener listener;

	@Inject
	public ColumnsPopupPresenter(SearchServiceAsync searchService, ColumnsPopupView columnPopupView){
		this.service = searchService;
		this.view = columnPopupView;	
		this.view.setPresenter(this);
	}

	@Override
	public void apply() {
		// Hide the dialog
		view.hide();
		// Fire off a change if there was one.
		if(hasSelectionChanged()){
			List<String> newSelection = new ArrayList<String>();
			// Add the columns in the same order as defined
			for(HeaderData header: defaultColumns){
				String key = header.getId();
				if(isSelected(key)){
					newSelection.add(key);
				}
			}
			// Add the additional
			for(HeaderData header: additionalColumns){
				String key = header.getId();
				if(isSelected(key)){
					newSelection.add(key);
				}
			}
			// Fire off the change
			this.listener.columnSelectionChanged(newSelection);
		}		
	}
	
	private boolean hasSelectionChanged(){
		// Compare the current selection to the input selection
		if(selection.size() != inSelection.size()) return true;
		for(String inKey: inSelection){
			// Is there an input key that is not selected
			if(!isSelected(inKey)) return true;
		}
		// They are the same
		return false;
	}
	
	@Override
	public void cancel() {
		this.selection.clear();
		view.hide();
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
	public void showPopup(String type, List<String> selected, ColumnSelectionChangeListener listener) {
		if(listener == null) throw new IllegalArgumentException("ColumnSelectionChangeListener cannot be null");
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		// Start with a clean selection
		this.selection.clear();
		this.type = type;
		this.inSelection = selected;
		this.listener = listener;
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

	public void setColumnsFromServer(ColumnsForType result) {
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
			this.inSelection = new ArrayList<String>();
			// All of the defaults are selected
			for(HeaderData header: defaultColumns){
				this.selection.add(header.getId());
				// Treat this as if they passed us these columns
				inSelection.add(header.getId());
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

	/**
	 * Exposed for testing
	 * @return
	 */
	public List<HeaderData> getDefaultColumns() {
		return defaultColumns;
	}

	/**
	 * Exposed for testing.
	 * @return
	 */
	public List<HeaderData> getAdditionalColumns() {
		return additionalColumns;
	}

}
