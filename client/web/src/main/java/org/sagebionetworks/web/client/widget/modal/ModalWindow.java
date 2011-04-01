package org.sagebionetworks.web.client.widget.modal;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ModalWindow implements ModalWindowView.Presenter {

	private ModalWindowView view;
	
	@Inject
	public ModalWindow(ModalWindowView view) {
		this.view = view;
		view.setPresenter(this);
	}

	public Widget asWidget() {
		return view.asWidget();
	}	
	
	
	
}
