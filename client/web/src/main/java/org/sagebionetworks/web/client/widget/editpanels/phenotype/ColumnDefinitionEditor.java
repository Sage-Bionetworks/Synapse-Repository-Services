package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.ontology.StaticOntologies;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ColumnDefinitionEditor implements ColumnDefinitionEditorView.Presenter, SynapseWidgetPresenter {
	private ColumnDefinitionEditorView view;
	private StaticOntologies staticOntologies;

	private List<String> columns;
    private String currentIdentityColumn;
    private Map<String,String> columnToOntology;
    private Collection<Ontology> ontologies;
	
	@Inject
	public ColumnDefinitionEditor(ColumnDefinitionEditorView view, StaticOntologies staticOntologies) {
        this.view = view;
		this.staticOntologies = staticOntologies;
        view.setPresenter(this);		
	}
	
	public void setResources(List<String> columns, String identityColumn, Map<String, String> columnToOntology, Collection<Ontology> ontologies) {
		this.columns = columns;
		this.currentIdentityColumn = identityColumn;
		this.columnToOntology = columnToOntology;
		this.ontologies = ontologies;
		this.view.createWidget(columns, identityColumn, columnToOntology, ontologies);
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

	
	@Override
	public void removeColumn(String columnName) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setColumnOntology(String columnName, String value) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setIdentityColumn(String value) {
		// TODO Auto-generated method stub
		
	}
}
