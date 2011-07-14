package org.sagebionetworks.web.client.widget.footer;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Footer implements FooterView.Presenter {

	private FooterView view;
	
	@Inject
	public Footer(FooterView view) {
		this.view = view;
		view.setPresenter(this);
	}

	public Widget asWidget() {
		view.setPresenter(this);
		return view.asWidget();
	}	
	
}
