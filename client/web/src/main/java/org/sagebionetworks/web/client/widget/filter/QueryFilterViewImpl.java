package org.sagebionetworks.web.client.widget.filter;

import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;


public class QueryFilterViewImpl extends Composite implements QueryFilterView {

	public interface QueryFilterViewImplUiBinder extends UiBinder<Widget, QueryFilterViewImpl> {	}

	@UiField
	HorizontalPanel horizPanel;
	@UiField
	HTML searchHtml;
	private Presenter presenter;

	@Inject
	public QueryFilterViewImpl(QueryFilterViewImplUiBinder binder) {
		this.initWidget(binder.createAndBindUi(this));
		
		searchHtml.setHeight("16px");
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
		
	}

	@Override
	public void showError(String message) {
		Window.alert(message);
	}

	@Override
	public void setDisplayData(List<DropdownData> viewData) {		
		horizPanel.clear();
		
		// Fill it with data from the list
		for(int i=0; i<viewData.size(); i++){
			final DropdownData data = viewData.get(i);
			final ListBox box = new ListBox();
			// Add all of the items
			List<String> values = data.getValueList();
			if(values != null){
				for(String value: values){
					box.addItem(value);
				}
			}
			//box.setWidth("150px");
			// Set the current selection
			box.setSelectedIndex(0);
			// Listen for selection changes
			box.addChangeHandler(new ChangeHandler() {
				
				@Override
				public void onChange(ChangeEvent event) {
					;
					presenter.setSelectionChanged(data.getId(), box.getSelectedIndex());
				}
			});
			horizPanel.add(box);
		}
	}
	
}
