package org.sagebionetworks.web.client.widget.editpanels;

import java.util.List;

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
	 */
	public void generateCreateForm(List<FormField> formFields, String typeDisplay, String topText, List<String> ignoreFields);

	/**
	 * Tells the view what fields to build in the form
	 * @param formFields
	 * @param isEditor show editor view
	 * @param typeDisplay display string for the type
	 */
	public void generateEditForm(List<FormField> formFields, String typeDisplay, String topText, List<String> ignoreFields, JSONObject editorValues);

	/**
	 * Shows a loading mask over the panel
	 */
	public void showLoading();
	
	/**
	 * Shows an error in the view
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
	}
}
