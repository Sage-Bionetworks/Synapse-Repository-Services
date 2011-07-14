package org.sagebionetworks.web.client.widget.modal;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ModalWindow implements ModalWindowView.Presenter {

	private ModalWindowView view;
	
	@Inject
	public ModalWindow(ModalWindowView view) {
		this.view = view;
		view.setPresenter(this);
	}

	public void setDimensions(int height, int width) {
		view.setDimensions(height, width);
	}
	
	public void setHtml(String html) {
		view.setHtml(html);
	}
	
	public void setCallbackButton(String buttonTitle, AsyncCallback<Void> onClickCallback) {
		view.setCallbackButton(buttonTitle, onClickCallback);
	}

	public void setHeading(String heading) {
		view.setHeading(heading);
	}
	
	public void setBlockBackground(boolean isModal) {
		view.setBlockBackground(isModal);
	}
	
	public void showWindow() {
		view.showWindow();
	}
	
	public void hideWindow() {
		view.hideWindow();
	}
		
	public void clear() { 
		view.clear();
	}
	
	public Widget asWidget() {
		view.setPresenter(this);
		return view.asWidget();
	}	
	
}
