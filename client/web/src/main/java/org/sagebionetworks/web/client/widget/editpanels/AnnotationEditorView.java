package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;

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
	public void generateAnnotationForm(List<FormField> formFields, String displayString, String topText);

	/**
	 * Shows a loading view
	 */
	public void showLoading();

	public void showPersistSuccess();

	public void showPersistFail();

	/**
	 * Clears out old elements
	 */
	public void clear();
	
	/**
	 * Show error message
	 * @param string
	 */
	public void showErrorMessage(String message);
	
    /**
     * Presenter interface
     */
    public interface Presenter {
    	public void persist(List<FormField> formFields);
    }

}