package org.sagebionetworks.web.client.widget.breadcrumb;

import java.util.List;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class BreadcrumbViewImpl extends Composite implements BreadcrumbView {
	public interface BreadcrumbViewImplUiBinder extends UiBinder<Widget, BreadcrumbViewImpl> {	}

	@UiField
	HorizontalPanel panel;
	private Presenter presenter;

	@Inject
	public BreadcrumbViewImpl() {		
	}
		
	@Override
	public Widget asWidget() {
		return this;
	}
	

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void setLinksList(List<Hyperlink> breadcrumbs) {
		setLinksList(breadcrumbs, null);
	}

	@Override
	public void setLinksList(List<Hyperlink> breadcrumbs, String current) {
		for(int i=0; i<breadcrumbs.size(); i++) {
			Hyperlink location = breadcrumbs.get(i);
			if(i>0) {
				panel.add(new HTML(" > "));
			}
			panel.add(location);						
		}
		if(current != null) {
			panel.add(new HTML(" > "+ current));
		}
	}

}
