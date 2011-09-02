package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ColumnMappingEditor implements ColumnMappingEditorView.Presenter, SynapseWidgetPresenter {
	private ColumnMappingEditorView view;

	private List<String> columns;
    private String currentIdentityColumn;
    private Map<String,String> columnToOntology;
    private Collection<Ontology> ontologies;
	
	@Inject
	public ColumnMappingEditor(ColumnMappingEditorView view) {
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
		// TODO Auto-generated method stub
		
	}

}
