package org.sagebionetworks.web.client.widget.modal;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ModalWindowViewImpl extends LayoutContainer implements ModalWindowView {

	private Presenter presenter;
	private final Window window = new Window();
	private LayoutContainer contentContainer;
	private String contentHtml;
	private String heading;
	private boolean windowCreated = false;
	private boolean isModal = true;
	private int height = 200;
	private int width = 600;
	private String stateId;
	
	private boolean useCallbackButton = false;
	Button callbackButton;
	private String callbackButtonTitle;
	private AsyncCallback<Void> onClickCallback;
	
	
	@Inject
	public ModalWindowViewImpl() {
		contentHtml = "";
		heading = "";
		stateId = String.valueOf(Random.nextInt());
	}
	
	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);		
	}
	
	@Override
	public Widget asWidget() {
		return this;
	}
	

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;		
	}

	@Override
	public void setDimensions(int height, int width) {
		if(height > 0 && width > 0) {
			this.height = height;
			this.width = width;
		}
	}		

	@Override
	public void setHtml(String html) {		
		contentHtml = html;
	}

	@Override
	public void setCallbackButton(String buttonTitle, final AsyncCallback<Void> onClickCallback) {
		useCallbackButton = true;
		callbackButtonTitle = buttonTitle;
		this.onClickCallback = onClickCallback;
		callbackButton = new Button(callbackButtonTitle, new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				onClickCallback.onSuccess(null);				
			}
		});			
		callbackButton.setStateId(stateId + "-callbackButton");

	}	

	@Override
	public void setHeading(String heading) {
		this.heading = heading;
	}
	
	@Override
	public void setBlockBackground(boolean isModal) {
		this.isModal = isModal;
	}
		
	@Override
	public void showWindow() {		
			// show download window
			if(!windowCreated) {
				createWindow();
			}
			window.show();
	}

	@Override
	public void hideWindow() {
		if(window != null && window.isVisible()) {
			window.hide();			
		}		
	}

	@Override
	public void clear() {
		contentContainer.removeAll();
		window.remove(callbackButton);
		callbackButtonTitle = null;
		onClickCallback = null;
	}

	
	/* 
	 * Protected Methods
	 */

	protected void createWindow() {
		window.setStateId(stateId);
		window.setSize(width, height);
		window.setPlain(true);
		window.setModal(isModal);
		window.setBlinkModal(true);
		window.setHeading(heading);
		window.setLayout(new FitLayout());
		window.setResizable(false);		
		
		RowData standardPadding = new RowData();
		standardPadding.setMargins(new Margins(15, 0, 0, 15));		
		
		ContentPanel panel = new ContentPanel();
		panel.setStateId(stateId + "-panel"); 
		panel.setLayoutData(new RowLayout(Orientation.VERTICAL));		
		panel.setBorders(false);
		panel.setBodyBorder(false);
		panel.setHeaderVisible(false);
		panel.addStyleName("pad-text");
		
		contentContainer = new LayoutContainer();
		contentContainer.setStateId(stateId + "-contentContainer");
		int contentHeight = height - 70 > 0 ? height - 70 : 0;
		contentContainer.setHeight(contentHeight);
		contentContainer.addStyleName("pad-text");
		contentContainer.setStyleAttribute("backgroundColor", "white");
		contentContainer.setBorders(false);
		contentContainer.setScrollMode(Style.Scroll.AUTOY);
		contentContainer.addText(contentHtml);
		contentContainer.addStyleName("pad-text");  		
		panel.add(contentContainer, standardPadding);
		
		if(useCallbackButton) {
			window.addButton(callbackButton);
		}
		
		Button closeButton = new Button("Close", new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						window.hide();
					}
		});
		closeButton.setStateId(stateId + "-closeButton");

		window.addButton(closeButton);
		window.add(panel);
		windowCreated = true;
	}
	
}

