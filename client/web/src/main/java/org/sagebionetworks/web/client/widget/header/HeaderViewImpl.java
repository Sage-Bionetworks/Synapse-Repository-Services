package org.sagebionetworks.web.client.widget.header;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.header.Header.MenuItem;

import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class HeaderViewImpl extends Composite implements HeaderView {

	public interface Binder extends UiBinder<Widget, HeaderViewImpl> {
	}
	
	@UiField
	LIElement navbarDatasets;
	@UiField
	LIElement navbarTools;
	@UiField
	LIElement navbarNetworks;
	@UiField
	LIElement navbarPeople;
	@UiField
	LIElement navbarProjects;
//	@UiField
//	PushButton searchButton;
//	@UiField
//	SimplePanel searchPanel;
	@UiField
	Anchor searchAnchor;
	
	private Presenter presenter;
	private Map<MenuItem, Element> itemToElement;
	
	@Inject
	public HeaderViewImpl(Binder binder, SageImageBundle sageImageBundle) {
		this.initWidget(binder.createAndBindUi(this));
		itemToElement = new HashMap<Header.MenuItem, Element>();		
		itemToElement.put(MenuItem.DATASETS, navbarDatasets);
		itemToElement.put(MenuItem.TOOLS, navbarTools);
		itemToElement.put(MenuItem.NETWORKS, navbarNetworks);
		itemToElement.put(MenuItem.PEOPLE, navbarPeople);
		itemToElement.put(MenuItem.PROJECTS, navbarProjects);		

		// search button
		searchAnchor.setHTML(AbstractImagePrototype.create(sageImageBundle.searchButtonHeaderIcon()).getHTML());
		//searchAnchor.setStyleName("search_button");
	}
	
	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void setMenuItemActive(MenuItem menuItem) {
		if(itemToElement == null) loadMap();
		Element el = itemToElement.get(menuItem);
		if(el != null) {
			el.addClassName("active");
		}
	}

	@Override
	public void removeMenuItemActive(MenuItem menuItem) {
		if(itemToElement == null) loadMap();
		Element el = itemToElement.get(menuItem);
		if(el != null) {
			el.removeClassName("active");
		}
	}

	/*
	 * Private Methods
	 */
	// load elements after page has rendered
	private void loadMap() {
	}
}

