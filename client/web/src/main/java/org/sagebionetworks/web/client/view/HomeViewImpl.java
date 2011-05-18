package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class HomeViewImpl extends Composite implements HomeView {

	public interface HomeViewImplUiBinder extends UiBinder<Widget, HomeViewImpl> {}
	
	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	
	private Presenter presenter;
	private Header headerWidget;
	private Footer footerWidget;
	
	@Inject
	public HomeViewImpl(HomeViewImplUiBinder binder, Header headerWidget, Footer footerWidget, IconsImageBundle icons, QueryFilter filter, SageImageBundle imageBundle) {		
		initWidget(binder.createAndBindUi(this));
		this.headerWidget = headerWidget;
		this.footerWidget = footerWidget;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());	
	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void refresh() {
		headerWidget.refresh();
	}

}
