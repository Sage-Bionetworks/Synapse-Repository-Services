package org.sagebionetworks.web.client.widget.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.shared.DisplayableValue;
import org.sagebionetworks.web.shared.FilterEnumeration;
import org.sagebionetworks.web.shared.WhereCondition;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * 
 * @author jmhill
 *
 */
public class QueryFilter implements QueryFilterView.Presenter, IsWidget{
	
	private QueryFilterView view;
	private SearchServiceAsync queryService;
	private Map<String, FilterEnumeration> filterMap;
	private LinkedHashMap<String, WhereCondition> whereMap;
	private List<SelectionListner> listeners;

	/**
	 * Injected via GIN
	 * @param view
	 */
	@Inject
	public QueryFilter(QueryFilterView view, SearchServiceAsync queryService){
		this.view = view;
		this.queryService = queryService;
		this.view.setPresenter(this);
		this.listeners = new ArrayList<SelectionListner>();
		// Get the data from the server
		refreshFromServer();
	}

	public void refreshFromServer() {
		queryService.getFilterEnumerations(new AsyncCallback<List<FilterEnumeration>>() {
			
			@Override
			public void onSuccess(List<FilterEnumeration> result) {
				setFilterEnumsFromServer(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showError("An error occurred. Please try reloading the page.");
			}
		});
		
	}

	protected void setFilterEnumsFromServer(List<FilterEnumeration> result) {
		// Store the resutls in a map
		this.filterMap = new TreeMap<String, FilterEnumeration>();
		// We maintain the order of selection.
		this.whereMap = new LinkedHashMap<String, WhereCondition>();
		if(result != null){
			// We also need to build up the data to send to the view
			List<DropdownData> viewData = new ArrayList<DropdownData>();
			for(FilterEnumeration filter: result){
				filterMap.put(filter.getColumnId(), filter);
				DropdownData data = new DropdownData();
				viewData.add(data);
				data.setId(filter.getColumnId());
				List<DisplayableValue> displayable = filter.getValues();;
				// The first on the list is the default
				data.addValue(filter.getDefaultValue());
				if(displayable != null){
					for(DisplayableValue display: displayable){
						// Display is only required to if the display 
						// does not match the value.
						String value = display.getDisplay();
						if(value == null) {
							value = display.getValue();
						}
						data.addValue(value);
					}
				}
			}
			// Set the data on the view.
			view.setDisplayData(viewData);
		}
	}

	@Override
	public Widget asWidget() {
		view.setPresenter(this);
		return view.asWidget();
	}

	@Override
	public void setSelectionChanged(String id, int selectedIndex) {
		// The first index is the default selection for index zero
		// remove it from the list
		if(selectedIndex == 0){
			whereMap.remove(id);
		}else{
			// Determine what the new value is.
			FilterEnumeration filterEnum = filterMap.get(id);
			if(filterEnum == null) throw new IllegalArgumentException("Unknown id: "+id);
			// Look up the value
			int realIndex = selectedIndex - 1;
			DisplayableValue displayValue = filterEnum.getValue(realIndex);
			// Build up the where
			WhereCondition condition = new WhereCondition(id,filterEnum.fetchOperator(), displayValue.getValue());
			whereMap.put(id, condition);
		}
		fireSelectionChanged();
	}
	/**
	 * 
	 * @param listener
	 */
	public void addSelectionListner(SelectionListner listener){
		this.listeners.add(listener);
	}
	
	private void fireSelectionChanged(){
		List<WhereCondition> newConditions = new ArrayList<WhereCondition>();
		Iterator<String> it = whereMap.keySet().iterator();
		while(it.hasNext()){
			newConditions.add(whereMap.get(it.next()));
		}
		// Let each listener know
		for(SelectionListner listener: listeners){
			listener.selectionChanged(newConditions);
		}
	}
	/**
	 * Listen for changes to the selection.
	 * 
	 */
	public static interface SelectionListner {
		/**
		 * Called when the selection changes.
		 * @param newConditions
		 */
		public void selectionChanged(List<WhereCondition> newConditions);
	}

}
