package org.sagebionetworks.web.client.widget.footer;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class FooterViewImpl extends Composite implements FooterView {

	public interface Binder extends UiBinder<Widget, FooterViewImpl> {
	}

	private Presenter presenter;
	
	@Inject
	public FooterViewImpl(Binder binder) {
		this.initWidget(binder.createAndBindUi(this));
	}
	
	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

}
