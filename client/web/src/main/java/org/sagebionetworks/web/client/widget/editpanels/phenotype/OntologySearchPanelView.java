package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import org.sagebionetworks.web.client.widget.SynapseWidgetView;

import com.google.gwt.user.client.ui.IsWidget;

public interface OntologySearchPanelView extends IsWidget, SynapseWidgetView {

	public void createWidget();
	
	public void disable();
	
	public void enable();
	
    /**
     * Set the presenter.
     * @param presenter
     */
    public void setPresenter(Presenter presenter);

	public void setSearchResults();

    
	/**
     * Presenter interface
     */
    public interface Presenter {

		void executeSearch(String searchTerms);    	
    	
    }
	
}
