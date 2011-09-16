package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PhenotypeMatrix implements PhenotypeMatrixView.Presenter, SynapseWidgetPresenter {
	private PhenotypeMatrixView view;
	private PlaceChanger placeChanger;
	
	@Inject
	public PhenotypeMatrix(PhenotypeMatrixView view) {
        this.view = view;
        view.setPresenter(this);		
	}
	
	public void setResources() {
		this.view.createWidget();
	}	
	
	public void disable() {
		this.view.disable();
	}
	
	public void enable() {
		this.view.enable();
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
	
	public void setHeight(int height) {
		view.setHeight(height);
	}
	
	public void setWidth(int width) {
		view.setWidth(width);
	}


}
