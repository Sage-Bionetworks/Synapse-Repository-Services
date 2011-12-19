package org.sagebionetworks.web.client.widget.licenseddownloader;

import java.util.List;

import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.shared.FileDownload;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LicensedDownloaderViewImpl extends LayoutContainer implements LicensedDownloaderView {

	private Presenter presenter;
	private boolean licenseAcceptanceRequired;	
	private boolean showCitation;
	private String citationText;	
	private String licenseTextHtml;
	private String downloadHtml;
	private Window eulaWindow;
	private Window downloadWindow;
	final CheckBox acceptLicenseCheckBox = new CheckBox();
	private Button acceptLicenseButton;
	private LayoutContainer licenseTextContainer;
	private LayoutContainer downloadContentContainer;
	private IconsImageBundle icons;
	private SageImageBundle sageImageBundle;


	/*
	 * Constructors
	 */
	@Inject
	public LicensedDownloaderViewImpl(IconsImageBundle icons, SageImageBundle sageImageBundle) {
		this.icons = icons;
		this.sageImageBundle = sageImageBundle;
		
		clear();
	}

	
	/**
	 * This method isn't used as the view is shown via showWindow() 
	 */
	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);		
	}

	/*
	 * View Impls
	 */
	@Override
	public void showWindow() {		
		if(licenseAcceptanceRequired) {
			// show License window
			createEulaWindow();
			// clear out selections if window has already been shown
			if(acceptLicenseCheckBox != null) {
				acceptLicenseCheckBox.setValue(false);
				acceptLicenseButton.disable();
			}
			eulaWindow.show();		
		} else {
			// show download window
			createDownloadWindow();
			downloadWindow.show();
		}		
	}

	@Override
	public void hideWindow() {
		if(eulaWindow != null && eulaWindow.isVisible()) {
			eulaWindow.hide();			
		}
		if(downloadWindow != null && downloadWindow.isVisible()) {
			downloadWindow.hide();
		}
	}
	
	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}
 
	@Override
	public Widget asWidget() {
		final LicensedDownloaderView view = this;
		Button downloadButton = new Button(DisplayConstants.BUTTON_DOWNLOAD, AbstractImagePrototype.create(icons.NavigateDown16()));		
		downloadButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				view.showWindow();
			}
		});		
		return downloadButton;
	}

	@Override
	public void setLicenceAcceptanceRequired(boolean licenseRequired) {
		this.licenseAcceptanceRequired = licenseRequired;		
	}

	@Override
	public void setLicenseHtml(String licenseHtml) {
		licenseTextHtml = licenseHtml;
		
		// replace the view content if this is after initialization
		if(licenseTextContainer != null) {
			refillLicenseTextContainer();
		}
	}
	
	@Override
	public void setCitationHtml(String citationHtml) {
		showCitation = true;
		citationText = citationHtml;		
	}
	
	@Deprecated
	@Override
	public void setDownloadUrls(List<FileDownload> downloads) {
		if(downloads != null && downloads.size() > 0) {
			// build a list of links in HTML
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<downloads.size(); i++) {
				FileDownload dl = downloads.get(i);
				sb.append("<a href=\"" + dl.getUrl() + "\" target=\"new\">" + dl.getDisplay() + "</a> " + AbstractImagePrototype.create(icons.external16()).getHTML());
				if(dl.getChecksum() != null) {
					sb.append("&nbsp;<small>md5 checksum: " + dl.getChecksum() + "</small>");
				}
				sb.append("<br/>");				
			}
			downloadHtml = sb.toString();

			// replace the view content if this is after initialization
			if(downloadContentContainer != null) {
				downloadHtml = sb.toString();
				fillDownloadContentContainer();
			}			
		} else {
			setNoDownloads();
		}
	}
	
	@Override
	public void setDownloadLocations(List<LocationData> locations) {
		if(locations != null && locations.size() > 0) {
			// build a list of links in HTML
			StringBuilder sb = new StringBuilder();
			String displayString = "Download";  // TODO : add display to LocationData
			for(int i=0; i<locations.size(); i++) {
				LocationData dl = locations.get(i);
				sb.append("<a href=\"" + dl.getPath() + "\" target=\"new\">" + displayString + "</a> " + AbstractImagePrototype.create(icons.external16()).getHTML());
				if(dl.getMd5() != null) {
					sb.append("&nbsp;<small>md5 checksum: " + dl.getMd5() + "</small>");
				}
				sb.append("<br/>");				
			}
			downloadHtml = sb.toString();

			// replace the view content if this is after initialization
			if(downloadContentContainer != null) {
				downloadHtml = sb.toString();
				fillDownloadContentContainer();
			}			
		} else {
			setNoDownloads();
		}
	}
	
	@Override
	public void clear() {
		// defaults
		citationText = "";
		licenseTextHtml = "";
		downloadHtml = "";		
	}

	@Override
	public void showDownloadFailure() {
		// TODO Auto-generated method stub		
	}


	@Override
	public void showLoading() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}
	
	
	@Override
	public void showDownloadsLoading() {
		downloadHtml = DisplayUtils.getIconHtml(sageImageBundle.loading16()) + " Loading...";
		fillDownloadContentContainer();
	}
	
	@Override
	public void setNoDownloads() {
		downloadHtml = DisplayConstants.TEXT_NO_DOWNLOADS;
		fillDownloadContentContainer();		
	}

	@Override
	public void setUnauthorizedDownloads() {
		downloadHtml = DisplayConstants.TEXT_UNAUTH_DOWNLOADS;
		fillDownloadContentContainer();		
	}


	/*
	 * Protected Methods
	 */	
	protected void createEulaWindow() {
		int windowHeight = showCitation ? 460 : 360;
		eulaWindow = new Window();
		eulaWindow.setSize(500, windowHeight);
		eulaWindow.setPlain(true);
		eulaWindow.setModal(true);
		eulaWindow.setBlinkModal(true);
		eulaWindow.setHeading("License Acceptance Required");
		eulaWindow.setLayout(new FitLayout());
		eulaWindow.setResizable(false);		
		
		RowData standardPadding = new RowData();
		standardPadding.setMargins(new Margins(15, 15, 0, 15));
		RowData h1Padding = new RowData();
		h1Padding.setMargins(new Margins(10, 15, 0, 15));
		RowData h2Padding = new RowData();
		h2Padding.setMargins(new Margins(10, 15, 0, 25));
		
		ContentPanel panel = new ContentPanel();		
		panel.setLayoutData(new RowLayout(Orientation.VERTICAL));		
		panel.setBorders(false);
		panel.setBodyBorder(false);
		panel.setHeaderVisible(false);		
		panel.setBodyStyle("backgroundColor: #e8e8e8");

		Label topTxtLabel = new Label("End-User License Agreement<br/>");
		topTxtLabel.setStyleAttribute("font-weight", "bold");
		panel.add(topTxtLabel, standardPadding);
		
		Label top2TxtLabel = new Label("&nbsp;&nbsp;Please read the following License Agreement.");
		panel.add(top2TxtLabel, standardPadding);
		
		licenseTextContainer = new LayoutContainer();
		licenseTextContainer.setHeight(200);
		licenseTextContainer.addStyleName("pad-text");
		licenseTextContainer.setStyleAttribute("backgroundColor", "white");
		licenseTextContainer.setBorders(true);
		licenseTextContainer.setScrollMode(Style.Scroll.AUTOY);
		refillLicenseTextContainer();
		panel.add(licenseTextContainer, standardPadding);


		if(showCitation) {
			Label useCitationLabel = new Label("Please use the following citation");
			panel.add(useCitationLabel);
			Text citationTxtLabel = new Text(citationText);
			panel.add(citationTxtLabel);
		}
		
		acceptLicenseCheckBox.setBoxLabel("I accept the terms in the license agreement");
		acceptLicenseCheckBox.addListener(Events.OnClick, new Listener<BaseEvent>() {
			@Override
			public void handleEvent(BaseEvent be) {
				if(acceptLicenseCheckBox.getValue()) {
					acceptLicenseButton.enable();
				} else {
					acceptLicenseButton.disable();
				}
			}
		});
		
		panel.add(acceptLicenseCheckBox, standardPadding);
		
		acceptLicenseButton = new Button("Accept", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				eulaWindow.hide();
				presenter.setLicenseAccepted();
			}
		});
		acceptLicenseButton.disable();
		Button cancelLicenseButton = new Button("Cancel", new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						eulaWindow.hide();
					}
				});

		panel.layout(true);
		eulaWindow.addButton(acceptLicenseButton);
		eulaWindow.addButton(cancelLicenseButton);
		eulaWindow.add(panel);
		eulaWindow.layout(true);
	}

	protected void createDownloadWindow() {
		downloadWindow = new Window();
		downloadWindow.setSize(600, 200);
		downloadWindow.setPlain(true);
		downloadWindow.setModal(false);
		downloadWindow.setBlinkModal(true);
		downloadWindow.setHeading("Download");
		downloadWindow.setLayout(new FitLayout());
		downloadWindow.setResizable(false);		
		
		RowData standardPadding = new RowData();
		standardPadding.setMargins(new Margins(15, 15, 0, 15));
		
		ContentPanel panel = new ContentPanel();		
		panel.setLayoutData(new RowLayout(Orientation.VERTICAL));		
		panel.setBorders(false);
		panel.setBodyBorder(false);
		panel.setHeaderVisible(false);
		
		downloadContentContainer = new LayoutContainer();
		downloadContentContainer.setHeight(100);
		downloadContentContainer.addStyleName("pad-text");
		downloadContentContainer.setStyleAttribute("backgroundColor", "white");
		downloadContentContainer.setBorders(false);
		downloadContentContainer.setScrollMode(Style.Scroll.AUTOY);
		if(downloadHtml == null || downloadHtml.equals("")) {
			setNoDownloads();
		} else {
			fillDownloadContentContainer();
		}
		panel.add(downloadContentContainer, standardPadding);
		
		Button cancelLicenseButton = new Button("Close", new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						downloadWindow.hide();
					}
				});

		downloadWindow.addButton(cancelLicenseButton);
		downloadWindow.add(panel);		
		downloadWindow.layout(true);
	}

	
	private void fillDownloadContentContainer() {
		if(downloadContentContainer != null) {
			downloadContentContainer.removeAll();				
			downloadContentContainer.add(new Html(downloadHtml));
			downloadContentContainer.layout(true);
		}
	}

	private void refillLicenseTextContainer() {
		licenseTextContainer.removeAll();
		licenseTextContainer.add(new Html(licenseTextHtml));
		licenseTextContainer.layout(true);
	}




	
	

}
