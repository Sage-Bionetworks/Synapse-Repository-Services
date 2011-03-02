package com.google.gwt.cell.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.CustomButton;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

/**
 * A DisclosurePanel that shows a preview of the content when closed.
 * 
 * @author dburdick
 */
public class PreviewDisclosurePanel extends Composite {

	private static int ARROW_PADDING = 5;
	private static int ARROW_COL = 0;
	private static int CAPTION_COL = 1;
	
	private FlexTable headerTable = new FlexTable();	
	private HTML previewHtml = new HTML();
	private HTML contentHtml = new HTML();	
	private boolean isOpen = false;		
		
	/**
	 * Injected via Gin
	 */
	private CustomWidgetImageBundle bundle;
	
	/**
	 * Constructs a PreviewDisclosurePanel 
	 * 
	 * @param bundle
	 * 		Custom Widget CliendBundle, injected via Gin 
	 */
	@Inject
	public PreviewDisclosurePanel(final CustomWidgetImageBundle bundle) {
		this.bundle = bundle;
	}
		
	/**
	 * Initializes an PreviewDisclosurePanel with the given caption.
	 * 
	 * @param caption
	 *            the caption to be displayed with the arrow
	 * @param preview
	 *            A string of HTML to be shown as the preview (arrow up)
	 * @param content
	 *            A string of HTML to be shown as the content (arrow down)
	 */
	public void init(String caption, String preview, String content) {
		// hide/show initial state
		setContentVisibility();		
		setPreview(preview);
		setContent(content);
		if(caption == null) {
			caption = "";
		}
		
		ImageResource iconArrowRight = bundle.iconArrowRight16(); 
		ImageResource iconArrowDown = bundle.iconArrowDown16(); 
		Image upImage = iconArrowRight == null ? new Image() : new Image(iconArrowRight);
		Image downImage = iconArrowDown == null ? new Image() : new Image(iconArrowDown);
		
		CustomButton arrowButton = new ToggleButton(upImage, downImage, new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				isOpen = isOpen ? false : true;
				setContentVisibility();
			}
		});
		arrowButton.setStyleName("previewDisclosureFace"); // empty style to remove standard button look
		arrowButton.setWidth((upImage.getWidth() + ARROW_PADDING) + "px");
					
		headerTable.setWidget(0, ARROW_COL, arrowButton);
		headerTable.setText(0, CAPTION_COL, caption);		
		
		// flow the elements vertically
		VerticalPanel panel = new VerticalPanel();
		panel.add(headerTable); 
		panel.add(previewHtml);
		panel.add(contentHtml);

		// All composites must call initWidget() in their constructors.
		initWidget(panel);		
	}
	
	/**
	 * Sets the caption associated with the arrow.
	 * 
	 * @param caption
	 *            the arrow's caption
	 */
	public void setCaption(String caption) {
		if(caption == null) 
			caption = "";			
		headerTable.setText(0, CAPTION_COL, caption);
	}

	/**
	 * Gets the caption associated with the arrow.
	 * 
	 * @return the arrow's caption
	 */
	public String getCaption() {
		return headerTable.getText(0, CAPTION_COL);
	}

	/**
	 * Gets the preview HTML.
	 * 
	 * @return the preview HTML
	 */
	public String getPreview() {
		return previewHtml.getHTML();
	}

	/**
	 * Sets the preview HTML that is shown when the arrow is closed.
	 * 
	 * @param preview
	 *            A string of HTML to be shown as the preview
	 */
	public void setPreview(String preview) {
		if(preview == null)
			preview = "";
		this.previewHtml.setHTML(preview);
	}

	/**
	 * Gets the content HTML.
	 * 
	 * @return the content HTML
	 */
	public String getContent() {
		return contentHtml.getHTML();
	}

	/**
	 * Sets the content HTML that is sown when the arrow is open.
	 * 
	 * @param content
	 *            A string of HTML to be shown as the expanded content
	 */
	public void setContent(String content) {
		if(content == null)
			content = "";
		this.contentHtml.setHTML(content);
	}

	/*
	 * Private Methods
	 */
	private void setContentVisibility() {
		if (isOpen) {
			previewHtml.setVisible(false);
			contentHtml.setVisible(true);
		} else {
			previewHtml.setVisible(true);
			contentHtml.setVisible(false);
		}
	}
}
