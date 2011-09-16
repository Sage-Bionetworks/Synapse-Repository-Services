package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import org.sagebionetworks.web.client.widget.SynapseWidgetView;

import com.google.gwt.user.client.ui.IsWidget;

public interface ColumnMappingEditorView extends IsWidget, SynapseWidgetView {

	public void createWidget();
	
	public void disable();
	
	public void enable();

    public void setHeight(int width);
    
    public void setWidth(int width);

    /**
     * Set the presenter.
     * @param presenter
     */
    public void setPresenter(Presenter presenter);

	
	/**
     * Presenter interface
     */
    public interface Presenter {
    	
    	
    }
	
}
