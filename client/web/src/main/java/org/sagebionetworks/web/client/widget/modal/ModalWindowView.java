package org.sagebionetworks.web.client.widget.modal;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;

public interface ModalWindowView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
		
	public void setDimensions(int height, int width);
	
	public void setHtml(String html);
	
	public void setCallbackButton(String buttonTitle, AsyncCallback<Void> onClickCallback);
	
	public void setHeading(String heading);
	
	public void setBlockBackground(boolean isModal);
	
	public void showWindow();
	
	public void hideWindow();
	
	public void clear();
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
	}
}

