package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.ontology.Ontology;

import com.google.gwt.user.client.ui.IsWidget;

public interface PhenotypeEditorView extends IsWidget {

	public void generatePhenotypeEditor(List<String> columns,
			String identityColumn, List<Map<String, String>> phenoData,
			Collection<Ontology> ontologies,
			ColumnDefinitionEditor columnDefinitionEditor,
			ColumnMappingEditor columnMappingEditor,
			PhenotypeMatrix phenotypeMatrix);
	
	
    /**
     * Set the presenter.
     * @param presenter
     */
    public void setPresenter(Presenter presenter);
		
	/**
	 * Shows a loading view
	 */
	public void showLoading();
	
	/**
	 * Clears out old elements
	 */
	public void clear();
	
	/**
	 * Shows user info
	 * @param title
	 * @param message
	 */
	public void showInfo(String title, String message);
	
	/**
	 * Show error message
	 * @param string
	 */
	public void showErrorMessage(String message);
	
    /**
     * Presenter interface
     */
    public interface Presenter {

		void removeColumn(String columnName);
		
		void setColumnOntology(String columnName, String value);

		void setIdentityColumn(String value);
    	
    }


}