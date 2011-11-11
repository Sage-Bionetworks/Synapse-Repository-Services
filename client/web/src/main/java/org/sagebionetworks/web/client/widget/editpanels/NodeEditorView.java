package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.ontology.Enumeration;
import org.sagebionetworks.web.client.ontology.EnumerationTerm;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.IsWidget;

public interface NodeEditorView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * Tells the view what fields to build in the form
	 * @param formFields
	 * @param isEditor show editor view
	 * @param typeDisplay display string for the type
	 * @param map 
	 */
	public void generateCreateForm(List<FormField> formFields, String typeDisplay, String topText, List<String> requiredFields, Map<String, Enumeration> keyToOntologyTerms);

	/**
	 * Tells the view what fields to build in the form
	 * @param formFields
	 * @param isEditor show editor view
	 * @param typeDisplay display string for the type
	 * @param map 
	 */
	public void generateEditForm(List<FormField> formFields, String typeDisplay, String topText, List<String> ignoreFields, Map<String, Enumeration> keyToOntologyTerms, JSONObject editorValues);

	/**
	 * Shows a loading view
	 */
	public void showLoading();
	
	/**
	 * Shows an error in the view
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	/**
	 * Shows the user that their changes/creation was saved
	 */
	public void showPersistSuccess();
	
	/**
	 * Shows the user that their changes/creation failed.
	 */
	public void showPersistFail(String message);
	
	/**
	 * Clears out any existing forms in the panel
	 */
	public void clear();
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {

		/**
		 * Asks the presenter to persist or create the object  
		 * @param formFields
		 */
		public void persist(List<FormField> formFields);		
		
		public void closeButtonSelected();		
	}
}
