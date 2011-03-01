package org.sagebionetworks.web.client.view;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DatasetsHomeViewImpl extends Composite implements DatasetsHomeView {

	public interface DatasetsHomeViewImplUiBinder extends 	UiBinder<Widget, DatasetsHomeViewImpl> {}

	@UiField 
	SimplePager pager;
	@UiField
	SimplePanel tablePanel;
	
	
	private Presenter presenter;
	private DynamicTableView dynamicTableView;

	@Inject
	public DatasetsHomeViewImpl(DatasetsHomeViewImplUiBinder binder, DynamicTableView dynamic) {
		this.dynamicTableView = dynamic;
		initWidget(binder.createAndBindUi(this));
		// Add the view to the panel
		tablePanel.add(this.dynamicTableView);
		// The pager will listen to the dynamic table
		pager.setDisplay(dynamic);
	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

}
