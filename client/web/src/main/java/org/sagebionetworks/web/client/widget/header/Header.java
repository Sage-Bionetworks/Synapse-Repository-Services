package org.sagebionetworks.web.client.widget.header;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Header implements HeaderView.Presenter {

	private HeaderView view;
	
	@Inject
	public Header(HeaderView view) {
		this.view = view;
		view.setPresenter(this);
	}

	public Widget asWidget() {
		return view.asWidget();
	}	
	
}
