package org.sagebionetworks.web.client.widget.portlet;

import java.util.List;

import org.sagebionetworks.web.client.widget.entity.DOMUtil;

import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.custom.Portlet;
import com.google.gwt.user.client.Element;

public class SynapsePortlet extends Portlet {

	public SynapsePortlet(String title, boolean isTop, boolean isTitle) {
		String headTag = "";
		String headTagClose = "";
		if(isTitle) {
			headTag = isTop ? "<h4 class=\"top\">" : "<h4>";
			headTagClose = "</h4>";			
		} else {
			headTag = isTop ? "<h3 class=\"top\">" : "<h3>";
			headTagClose = "</h3>";
		}
		Html header = new Html(headTag + title + "</h3>");
		header.setAutoHeight(true);
	    add(header);
	    
		setStyleName("");
		setHeaderVisible(false);
		setBorders(false);
		setBodyStyleName(""); // TODO : create a style for SynapsePortlet?
		setBodyStyle("background-color: #ffffff");
		
	}
	
	public SynapsePortlet(String title) {
		this(title, false, false);
	}
	
	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);
		// remove all GXT ContentPanel border styles
		String[] classNames = new String[] { "x-panel-ml", "x-panel-mr",
				"x-panel-bl", "x-panel-br", "x-panel-bc", "x-panel-mc",
				"x-panel-body", "x-panel-tl", "x-panel-tr",
				"x-panel-body-noheader" };			
		removeStyles(classNames, this.getElement());			
	}

	private void removeStyles(String[] classNames, Element rootElement) {
		for(String className : classNames) {
		@SuppressWarnings("unchecked")
		List<Element> bwraps = DOMUtil.getElementsByClassNameFrom(rootElement, className);
			for(Element el : bwraps) {
				el.removeClassName(className);
			}
		}
	}
}
