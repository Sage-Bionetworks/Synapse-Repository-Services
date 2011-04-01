package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class HomeViewImpl extends Composite implements HomeView {

	public interface HomeViewImplUiBinder extends UiBinder<Widget, HomeViewImpl> {}
	
	private Presenter presenter;
	
	@Inject
	public HomeViewImpl(HomeViewImplUiBinder binder, IconsImageBundle icons, QueryFilter filter, SageImageBundle imageBundle) {		
		initWidget(binder.createAndBindUi(this));

	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

}
