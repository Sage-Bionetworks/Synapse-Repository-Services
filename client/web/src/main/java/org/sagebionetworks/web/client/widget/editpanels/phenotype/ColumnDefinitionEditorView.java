package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.ontology.Ontology;
import org.sagebionetworks.web.client.widget.SynapseWidgetView;
import com.google.gwt.user.client.ui.IsWidget;

public interface ColumnDefinitionEditorView extends IsWidget, SynapseWidgetView {

	public void createWidget(List<String> columns, String identityColumn, Map<String,String> columnToOntology, Collection<Ontology> ontologies);
	
	public void refresh(List<String> columns, String identityColumn, Map<String,String> columnToOntology);

	
    /**
     * Set the presenter.
     * @param presenter
     */
    public void setPresenter(Presenter presenter);

    public void setHeight(int width);
    
    public void setWidth(int width);
    
    
	/**
     * Presenter interface
     */
    public interface Presenter {
    	
		void removeColumn(String columnName);
		
		void setColumnOntology(String columnName, String value);

		void setIdentityColumn(String value);
    	
    }
	
}
