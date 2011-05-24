package org.sagebionetworks.web.client.widget.header;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Header implements HeaderView.Presenter {

	public static enum MenuItems {
		DATASETS, TOOLS, NETWORKS, PEOPLE, PROJECTS
	}
	
	private HeaderView view;
	
	@Inject
	public Header(HeaderView view) {
		this.view = view;
		view.setPresenter(this);
	}
	
	public void setMenuItemActive(MenuItems menuItem) {
		view.setMenuItemActive(menuItem);
	}

	public void removeMenuItemActive(MenuItems menuItem) {
		view.removeMenuItemActive(menuItem);
	}

	public Widget asWidget() {
		return view.asWidget();
	}
	
	public void refresh() {
		view.refresh();
	}
	
}
