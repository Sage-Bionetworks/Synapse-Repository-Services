package org.sagebionetworks.web.client.view;

import java.util.List;

import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.shared.SearchParameters.FromType;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DatasetsHomeViewImpl extends Composite implements DatasetsHomeView {

	public interface DatasetsHomeViewImplUiBinder extends 	UiBinder<Widget, DatasetsHomeViewImpl> {}

	@UiField
	SimplePanel tablePanel;
	@UiField
	Button addColumnsButton;
	
	private Presenter presenter;
	private QueryServiceTable queryServiceTable;
	
	@Inject
	public DatasetsHomeViewImpl(DatasetsHomeViewImplUiBinder binder, QueryServiceTable table) {
		this.queryServiceTable = table;
		initWidget(binder.createAndBindUi(this));
		// The pager will listen to the dynamic table
		table.setUsePager(true);
		table.setType(FromType.dataset);
		// Add the table
		tablePanel.add(table.asWidget());

		addColumnsButton.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				presenter.onEditColumns();
			}});
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
