package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.ontology.Enumeration;
import org.sagebionetworks.web.client.ontology.StaticEnumerations;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ColumnDefinitionEditor implements ColumnDefinitionEditorView.Presenter, SynapseWidgetPresenter {
	private ColumnDefinitionEditorView view;
	private StaticEnumerations staticOntologies;

	private List<String> columns;
    private String currentIdentityColumn;
    private Map<String,String> columnToOntology;
    private Collection<Enumeration> ontologies;
	
	@Inject
	public ColumnDefinitionEditor(ColumnDefinitionEditorView view, StaticEnumerations staticOntologies) {
        this.view = view;
		this.staticOntologies = staticOntologies;
        view.setPresenter(this);		
	}
	
	public void setResources(List<String> columns, String identityColumn, Map<String, String> columnToOntology, Collection<Enumeration> ontologies) {
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
	
	public void setHeight(int height) {
		view.setHeight(height);
	}
	
	public void setWidth(int width) {
		view.setWidth(width);
	}
}
