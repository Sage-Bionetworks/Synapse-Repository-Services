package org.sagebionetworks.web.client.widget.sharing;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AccessMenuButton implements AccessMenuButtonView.Presenter {

	public static enum AccessLevel { PUBLIC, SHARED, PRIVATE }
	
	private AccessMenuButtonView view;
	
	@Inject
	public AccessMenuButton(AccessMenuButtonView view) {
		this.view = view;
		view.setPresenter(this);
	}	
	
	public void setAccessLevel(AccessLevel level) {
		view.setAccessLevel(level);
	}

	public Widget asWidget() {
		return view.asWidget();
	}	
	
	
	
}
