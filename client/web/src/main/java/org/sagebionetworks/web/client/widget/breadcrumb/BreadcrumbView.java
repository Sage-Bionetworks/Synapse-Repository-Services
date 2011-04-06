package org.sagebionetworks.web.client.widget.breadcrumb;

import java.util.List;

import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.IsWidget;

public interface BreadcrumbView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	public void setLinksList(List<Hyperlink> breadcrumbs);
	
	public void setLinksList(List<Hyperlink> breadcrumbs, String current);
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
	}
}
