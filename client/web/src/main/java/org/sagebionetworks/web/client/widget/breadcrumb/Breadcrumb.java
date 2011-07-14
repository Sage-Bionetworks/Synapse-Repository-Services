package org.sagebionetworks.web.client.widget.breadcrumb;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Breadcrumb implements BreadcrumbView.Presenter {

	private List<Hyperlink> order;
	private String current;
	private BreadcrumbView view;
	
	@Inject
	public Breadcrumb(BreadcrumbView view) {
		this.view = view;
		view.setPresenter(this);
		
		order = new LinkedList<Hyperlink>();
	}
	
	public void appendLocation(Hyperlink location) {
		order.add(location);
	}
	
	public void setCurrentLocation(String current) {
		this.current = current;
	}

	public Widget asWidget() {
		view.setPresenter(this);
		if(current != null) {
			view.setLinksList(order, current);
		} else {
			view.setLinksList(order);
		}	
		return view.asWidget();
	}	
}
