package org.sagebionetworks.web.client.view;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DatasetsHomeViewImpl extends Composite implements DatasetsHomeView {

	public interface DatasetsHomeViewImplUiBinder extends 	UiBinder<Widget, DatasetsHomeViewImpl> {}

	@UiField
	SimplePanel simplePanel;
	private Presenter presenter;
	private DynamicTableView dynamicTableView;

	@Inject
	public DatasetsHomeViewImpl(DatasetsHomeViewImplUiBinder binder, DynamicTableView dynamic) {
		this.dynamicTableView = dynamic;
		initWidget(binder.createAndBindUi(this));
		// Add the view to the panel
		simplePanel.add(this.dynamicTableView);
	}



	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

}
