package org.sagebionetworks.web.client.widget.modal;

import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ModalWindowViewImpl extends LayoutContainer implements ModalWindowView {

	private Presenter presenter;
	
	@Inject
	public ModalWindowViewImpl() {		
	}
	
	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);		
	}
	
	@Override
	public Widget asWidget() {
		return this;
	}
	

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;		
	}

}
