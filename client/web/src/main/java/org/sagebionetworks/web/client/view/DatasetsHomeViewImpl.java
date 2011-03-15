package org.sagebionetworks.web.client.view;

import java.util.List;

import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.filter.QueryFilter.SelectionListner;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.WhereCondition;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DatasetsHomeViewImpl extends Composite implements DatasetsHomeView {

	public interface DatasetsHomeViewImplUiBinder extends 	UiBinder<Widget, DatasetsHomeViewImpl> {}

	@UiField
	SimplePanel tablePanel;
	//ContentPanel tablePanel;
	@UiField
	Button addColumnsButton;
	@UiField
	SimplePanel filterPanel;
	@UiField
	TextBox searchTextBox;
	@UiField (provided=true)
	PushButton searchButton;
	
	private Presenter presenter;
	private QueryServiceTable queryServiceTable;
	
	@Inject
	public DatasetsHomeViewImpl(DatasetsHomeViewImplUiBinder binder, QueryFilter filter, SageImageBundle imageBundle, QueryServiceTableResourceProvider queryServiceTableResourceProvider) {		
		queryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.dataset, true, 880, 440);
		ImageResource searchIR = imageBundle.searchButtonIcon();
		searchButton = new PushButton(new Image(searchIR));
		initWidget(binder.createAndBindUi(this));
		searchButton.setStyleName("imageButton");

		// The pager will listen to the dynamic table
		queryServiceTable.initialize(ObjectType.dataset, true);

		// Add the table
		tablePanel.add(queryServiceTable.asWidget());
		// Add the filter
		filterPanel.add(filter);

		addColumnsButton.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				presenter.onEditColumns();
			}});
		
		// We need to listen to filter changes
		filter.addSelectionListner(new SelectionListner() {
			
			@Override
			public void selectionChanged(List<WhereCondition> newConditions) {
				if(newConditions.size() < 1){
					queryServiceTable.setWhereCondition(null);
				}else{
					queryServiceTable.setWhereCondition(newConditions.get(0));
				}
			}
		});
	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}


	@Override
	public void setVisibleColumns(List<String> visible) {
		this.queryServiceTable.setDispalyColumns(visible);
	}

}
