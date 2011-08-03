package org.sagebionetworks.web.client.widget.editpanels;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.web.client.ontology.Ontology;

import com.google.gwt.user.client.ui.IsWidget;

public interface AnnotationEditorView extends IsWidget {
 
    /**
     * Set the presenter.
     * @param presenter
     */
    public void setPresenter(Presenter presenter);

    /**
     * Create the annotation form
     * @param formFields
     * @param displayString
     * @param annotationIgnoreFields
     */
	public void generateAnnotationForm(List<FormField> formFields, String displayString, String topText, boolean editable);
	
	public void updateAnnotations(List<FormField> formFields);

	public void setOntologies(Collection<Ontology> ontologies);
		
	/**
	 * Shows a loading view
	 */
	public void showLoading();

	public void showPersistSuccess();

	public void showPersistFail();

	public void showAddAnnotationSuccess();
	
	public void showAddAnnotationFail(String string);

	public void showDeleteAnnotationSuccess();
	
	public void showDeleteAnnotationFail();
	
	/**
	 * Clears out old elements
	 */
	public void clear();
	
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
    	
    	public void editAnnotation(String key, String newValue);
    	
    	public void addAnnotation(String key, ColumnEditType type);
    	
    	public void addAnnotation(String key, ColumnEditType type, Ontology ontology);
    	
    	public void deleteAnnotation(String key);
    }


}