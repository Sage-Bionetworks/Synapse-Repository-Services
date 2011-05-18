package com.google.gwt.cell.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.CustomButton;
import com.google.gwt.user.client.ui.FlexTable;
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
	
	private FlexTable headerTable;
	private HTML previewHtml;
	private HTML contentHtml;	
	private boolean isOpen = false;		
	private String caption;
	private String preview;
	private String content;

	
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
	public PreviewDisclosurePanel(final CustomWidgetImageBundle bundle, final FlexTable headerTable, final HTML previewHtml, final HTML contentHtml) {
		this.bundle = bundle;
		this.headerTable = headerTable;
		this.previewHtml = previewHtml;
		this.contentHtml = contentHtml;
		// flow the elements vertically
		VerticalPanel panel = new VerticalPanel();
		panel.add(headerTable); 
		panel.add(previewHtml);
		panel.add(contentHtml);

		// All composites must call initWidget() in their constructors.
		initWidget(panel);
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
		
		CustomButton arrowButton = null;		
		if(bundle.iconArrowRight16() != null && bundle.iconArrowDown16() != null) {
			Image upImage = new Image(bundle.iconArrowRight16());
			Image downImage = new Image(bundle.iconArrowDown16());
			arrowButton = new ToggleButton(upImage, downImage, new ClickHandler() {			
				@Override
				public void onClick(ClickEvent event) {
					isOpen = isOpen ? false : true;
					setContentVisibility();
				}
			});
			arrowButton.setWidth((upImage.getWidth() + ARROW_PADDING) + "px");

		} else {
			// If no real images are passed in the bundle use strings 
			arrowButton = new ToggleButton("Expand", "Collapse", new ClickHandler() {			
				@Override
				public void onClick(ClickEvent event) {
					isOpen = isOpen ? false : true;
					setContentVisibility();
				}
			});
			arrowButton.setWidth("70px"); // reasonable width for expand/collapse text			
		}
		
		arrowButton.setStyleName("previewDisclosureFace"); // empty style to remove standard button look
					
		headerTable.setWidget(0, ARROW_COL, arrowButton);
		headerTable.setText(0, CAPTION_COL, caption);		
		
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
		this.caption = caption;
		headerTable.setText(0, CAPTION_COL, this.caption);
	}

	/**
	 * Gets the caption associated with the arrow.
	 * 
	 * @return the arrow's caption
	 */
	public String getCaption() {
		return caption;
	}

	/**
	 * Gets the preview HTML.
	 * 
	 * @return the preview HTML
	 */
	public String getPreview() {
		return preview;
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
		this.preview = preview;
		if(!"".equals(this.preview)) this.preview += "..."; // add elipses if preview has characters
		this.previewHtml.setHTML(this.preview);
	}

	/**
	 * Gets the content HTML.
	 * 
	 * @return the content HTML
	 */
	public String getContent() {
		return content;
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
		this.content = content;
		this.contentHtml.setHTML(this.content);
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
