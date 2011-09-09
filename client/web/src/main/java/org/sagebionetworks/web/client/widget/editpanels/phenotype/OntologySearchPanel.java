package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class OntologySearchPanel implements OntologySearchPanelView.Presenter, SynapseWidgetPresenter {
	private OntologySearchPanelView view;

    private PlaceChanger placeChanger;
	
	@Inject
	public OntologySearchPanel(OntologySearchPanelView view) {
        this.view = view;
        view.setPresenter(this);		
	}
	
	public void setResources() {
		this.view.createWidget();
	}	
		
    @Override
	public Widget asWidget() {
   		view.setPresenter(this);
        return view.asWidget();
    }

	@Override
	public void setPlaceChanger(PlaceChanger placeChanger) {
		this.placeChanger = placeChanger;
	}

	@Override
	public void executeSearch(String searchTerms) {
		// TODO call Ontology search service
		view.setSearchResults();		
	}

}
