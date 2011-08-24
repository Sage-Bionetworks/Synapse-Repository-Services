package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.PersistSuccessEvent;
import org.sagebionetworks.web.client.events.PersistSuccessHandler;
import org.sagebionetworks.web.client.widget.adminmenu.AdminMenu;
import org.sagebionetworks.web.client.widget.editpanels.AnnotationEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
import org.sagebionetworks.web.client.widget.modal.ModalWindow;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton.AccessLevel;
import org.sagebionetworks.web.client.widget.statictable.StaticTable;
import org.sagebionetworks.web.client.widget.statictable.StaticTableColumn;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LayerViewImpl extends Composite implements LayerView {

	public interface Binder extends UiBinder<Widget, LayerViewImpl> {
	}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;	
	@UiField
	FlowPanel overviewPanel;
	@UiField
	SpanElement titleSpan;
	@UiField 
	SpanElement synapseIdSpan;
	@UiField 
	SimplePanel previewTablePanel;	
	@UiField
	SimplePanel downloadPanel;
	@UiField
	SimplePanel seeTermsPanel;
	@UiField
	SpanElement breadcrumbDatasetSpan;
	@UiField
	SpanElement breadcrumbTitleSpan;
	@UiField
	SimplePanel annotationsPanel;
	@UiField
	SpanElement previewTableMessage;
	@UiField
	SimplePanel previewDownload;
	@UiField
	SimplePanel accessPanel;
	@UiField
	SpanElement accessSpan;
	@UiField
	SimplePanel adminPanel;
	
	private Presenter presenter;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private StaticTable staticTable;
	private final LicensedDownloader licensedDownloader;
	private boolean disableDownloads = false;
	private IconsImageBundle iconsImageBundle;
	private ModalWindow seeTermsModal;
	private AccessMenuButton accessMenuButton;
	private NodeEditor nodeEditor;
	private AnnotationEditor annotationEditor;
	private AdminMenu adminMenu;
	private boolean isAdministrator = false; 
	private boolean canEdit = false;
	private Header headerWidget;

	@Inject
	public LayerViewImpl(Binder uiBinder, Header headerWidget,
			Footer footerWidget, 
			IconsImageBundle iconsImageBundle,			
			final PreviewDisclosurePanel previewDisclosurePanel,
			StaticTable staticTable, LicensedDownloader licensedDownloader,
			final ModalWindow seeTermsModal,
			AccessMenuButton accessMenuButton,
			NodeEditor nodeEditor,
			AnnotationEditor annotationEditor,
			AdminMenu adminMenu) {		
		
		initWidget(uiBinder.createAndBindUi(this));
		
		this.previewDisclosurePanel = previewDisclosurePanel;
		this.staticTable = staticTable;
		this.licensedDownloader = licensedDownloader;
		this.iconsImageBundle = iconsImageBundle;
		this.accessMenuButton = accessMenuButton;
		this.nodeEditor = nodeEditor;
		this.annotationEditor = annotationEditor;
		this.adminMenu = adminMenu;
		this.seeTermsModal = seeTermsModal;
		this.headerWidget = headerWidget;

		// Header setup
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());	
		headerWidget.setMenuItemActive(MenuItems.DATASETS);
		
		// alignment setup
//		rightFlexTable.setCellSpacing(5);		
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
		this.headerWidget.refresh();		
	}

	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Message", message, null);
	}

	@Override
	public void showInfo(String title, String message) {
		Info.display(title, message);
	}


	@Override
	public void showLayerPreviewUnavailable() {
		// check if user is admin? show create preview?
		//previewTableMessage.setInnerHTML("Preview of this layer is unavailable.");
	}
	
	@Override
	public void setLayerDetails(String id, 
								String layerName, 
								String processingFacility, 
								String qcByDisplay,
								String qcByUrl, 
								String qcAnalysisDisplay, 
								String qcAnalysisUrl,
								Date qcDate, 
								String overviewText, 
								int nDataRowsShown,
								int totalDataRows, 
								String privacyLevel, 
								String datasetLink, 
								String platform, 
								boolean isAdministrator, 
								boolean canEdit) {
		
		// make sure displayed values are clean
		if(layerName == null) layerName = "";
		if(processingFacility == null) processingFacility = "";
		if(qcByDisplay == null) qcByDisplay = "";
		if(qcByUrl == null) qcByUrl = "";
		if(qcAnalysisDisplay == null) qcAnalysisDisplay = "";
		if(qcAnalysisUrl == null) qcAnalysisUrl = "";
		if(overviewText == null) overviewText  = "";
		if(privacyLevel == null) privacyLevel = "";
		if(platform == null) platform = "";
		
		// check authorization
		this.isAdministrator = isAdministrator;
		this.canEdit = canEdit;
		createAccessPanel(id);
		createAdminPanel(id);
		
		setupLicensedDownloaderCallbacks();
						
		Anchor downloadLink = setupDownloadLink();
		downloadPanel.clear();
		downloadPanel.add(downloadLink);

		// fill in fields
		titleSpan.setInnerText(layerName);
		synapseIdSpan.setInnerText(DisplayConstants.SYNAPSE_ID_PREFIX + id);
			
		// set description
		if(overviewText == null) overviewText = ""; 
		if(DisplayConstants.showDemoHtml) {
			if(overviewText.equals("")) {
				overviewText = DisplayConstants.DEMO_OVERVIEW;
			}
		}
		int summaryLength = overviewText.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH : overviewText.length();
		previewDisclosurePanel.init("Expand", overviewText.substring(0, summaryLength), overviewText);
		overviewPanel.clear();
		overviewPanel.add(previewDisclosurePanel);
		
		annotationsPanel.clear();
		annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
		annotationEditor.setResource(NodeType.LAYER, id);
		annotationsPanel.add(annotationEditor.asWidget());
		
		// breadcrumbs
		breadcrumbDatasetSpan.setInnerHTML(datasetLink);
		breadcrumbTitleSpan.setInnerText(layerName);		
	}
	
	@Override
	public void showDownload() {	
		if(!disableDownloads)
			licensedDownloader.showWindow();
	}

	@Override
	public void setLicenseAgreement(LicenseAgreement agreement) {
		licensedDownloader.setLicenseAgreement(agreement);		
		Anchor seeTermsAnchor = setupTermsModal(agreement);
		seeTermsPanel.clear();		
		seeTermsPanel.add(seeTermsAnchor);
	}

	@Override
	public void requireLicenseAcceptance(boolean requireLicense) {
		licensedDownloader.setRequireLicenseAcceptance(requireLicense);		
	}

	@Override
	public void disableLicensedDownloads(boolean disable) {
		this.disableDownloads = true;
	}

	@Override
	public void setLicensedDownloads(List<FileDownload> downloads) {
		licensedDownloader.setDownloadUrls(downloads);			
		if(downloads.size() > 0) {
			ContentPanel cp = new ContentPanel();			
			cp.setHeading(DisplayConstants.TITLE_LAYER_PREVIEW);
			cp.setHeaderVisible(true);			
			cp.setAutoHeight(true);
			cp.setScrollMode(Scroll.AUTO);
			FileDownload download = downloads.get(0);
			if(DisplayUtils.MIME_TYPE_JPEG.equals(download.getContentType())
					|| DisplayUtils.MIME_TYPE_PNG.equals(download.getContentType())
					|| DisplayUtils.MIME_TYPE_GIF.equals(download.getContentType())) {					
				Image imgWidget = new Image(download.getUrl());
				cp.add(imgWidget);
				previewDownload.clear();
				previewDownload.add(cp);
				cp.layout(true);
			}
		}
	}
	
	
	@Override
	public void setLayerPreviewTable(List<Map<String,String>> rows,
			List<String> columnDisplayOrder,
			Map<String, String> columnDescriptions,
			Map<String, String> columnUnits) {
		// TODO : add data to static table		
		staticTable.setDimensions(1002, 175);
		staticTable.setTitle("Data Preview");		
		
		// create static table columns
		List<StaticTableColumn> stColumns = new ArrayList<StaticTableColumn>();
		for(String key : columnDisplayOrder) {
			StaticTableColumn stCol = new StaticTableColumn();
			stCol.setId(key);
			stCol.setName(key);
			
			// add units to column if available
			if (columnUnits.containsKey(key)) {
				stCol.setUnits(columnUnits.get(key));
			} 
			
			// add description if available
			if (columnDescriptions.containsKey(key)) {
				stCol.setTooltip(columnDescriptions.get(key));
			}
			
			stColumns.add(stCol);
		}
		
		staticTable.setDataAndColumnsInOrder(rows, stColumns);
		previewTablePanel.setWidget(staticTable.asWidget());		
	}	

	@Override
	public void setDownloadUnavailable() {
		licensedDownloader.setDownloadUrls(null);
		downloadPanel.clear();
		downloadPanel.add(new Html(DisplayUtils.getIconHtml(iconsImageBundle.download16()) + " Download Unavailable"));		
	}

	
	@Override
	public void clear() {
		titleSpan.setInnerText("");		
		staticTable.clear();
		downloadPanel.clear();
		previewDownload.clear();
	}

	@Override
	public void showDownloadsLoading() {
		licensedDownloader.showLoading();
	}

	
	/*
	 * Private Methods
	 */
	private Anchor setupDownloadLink() {
		// download link		
		Anchor downloadLink = new Anchor();
		downloadLink.setHTML(AbstractImagePrototype.create(iconsImageBundle.download16()).getHTML() + " Download Layer");
		downloadLink.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {				
				if(presenter.downloadAttempted()) {
					licensedDownloader.showWindow();
				}
			}
		});
		return downloadLink;
	}

	private Anchor setupTermsModal(LicenseAgreement licenseAgreement) {		
		// Button: See terms of use		
		seeTermsModal.setHeading("Terms of Use");
		seeTermsModal.setDimensions(400, 500);
		if(licenseAgreement != null) {
			seeTermsModal.setHtml(licenseAgreement.getLicenseHtml());
		}
		// download link		
		Anchor seeTermsAnchor = new Anchor();
		seeTermsAnchor.setHTML(AbstractImagePrototype.create(iconsImageBundle.documentText16()).getHTML() + " See Terms of Use");
		seeTermsAnchor.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				seeTermsModal.showWindow();
			}
		});

		return seeTermsAnchor;
	}

	private void setupLicensedDownloaderCallbacks() {
		// give the LicensedDownloader something to call when the view accepts the license
		licensedDownloader.setLicenseAcceptedCallback(new AsyncCallback<Void>() {
			// called when the user agrees to the license 
			@Override
			public void onSuccess(Void result) {
				// let presenter know so it can persist this
				presenter.licenseAccepted();
			}

			// not used
			@Override
			public void onFailure(Throwable caught) { 
				showInfo("Error", DisplayConstants.ERROR_FAILED_PERSIST_AGREEMENT_TEXT);				
			}

		});
	}
	
	private void createAdminPanel(String id) {		
		if(isAdministrator) {
			annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
			annotationEditor.setResource(NodeType.LAYER, id);
			
			Button button = new Button("Admin Menu");
			button.setIcon(AbstractImagePrototype.create(iconsImageBundle.adminTools16()));
			//adminButton.setIconAlign(IconAlign.LEFT);
			button.setMenu(createAdminMenu(id));
			button.setHeight(25);
			adminPanel.clear();
			adminPanel.add(button);
		}
	}

	private Menu createAdminMenu(final String datasetId) {
		Menu menu = new Menu();		
		MenuItem item = null; 
			
		// Edit menu options
		if(canEdit) {			
			item = new MenuItem("Edit Layer Details");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.applicationEdit16()));		
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					final Window window = new Window();  
					window.setSize(600, 280);
					window.setPlain(true);
					window.setModal(true);
					window.setBlinkModal(true);
					window.setHeading("Edit Layer");
					window.setLayout(new FitLayout());								
					nodeEditor.addCancelHandler(new CancelHandler() {					
						@Override
						public void onCancel(CancelEvent event) {
							window.hide();
						}
					});
					nodeEditor.addPersistSuccessHandler(new PersistSuccessHandler() {					
						@Override
						public void onPersistSuccess(PersistSuccessEvent event) {
							window.hide();
							presenter.refresh();
						}
					});
					nodeEditor.setPlaceChanger(presenter.getPlaceChanger());
					window.add(nodeEditor.asWidget(NodeType.LAYER, datasetId), new FitData(4));
					window.show();
				}
			});
			menu.add(item);			
		}
			
		// Administrator Menu Options
		if(isAdministrator) {
			item = new MenuItem("Delete Layer");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {
					MessageBox.confirm("Delete Layer", "Are you sure you want to delete this layer?", new Listener<MessageBoxEvent>() {					
						@Override
						public void handleEvent(MessageBoxEvent be) { 					
							Button btn = be.getButtonClicked();
							if(Dialog.YES.equals(btn.getItemId())) {
								presenter.delete();
							}
						}
					});
				}
			});
			menu.add(item);
		}

		
		return menu;
	}


	private void createAccessPanel(String id) {		
		// TODO : get access level from Authorization service
		AccessLevel accessLevel = AccessLevel.SHARED;		
		ImageResource icon = null;
		if(accessLevel == AccessLevel.PUBLIC) {
			icon = iconsImageBundle.lockUnlocked16();
		} else {
			icon = iconsImageBundle.lock16();
		}		

		if(isAdministrator) {	
			accessMenuButton.setPlaceChanger(presenter.getPlaceChanger());
			accessMenuButton.createAccessButton(accessLevel, NodeType.LAYER, id);
			accessPanel.clear();
			accessPanel.add(accessMenuButton.asWidget());
		} else {
			accessSpan.setInnerHTML("<span class=\"setting_label\">Access: </span><span class=\"setting_level\">"+ DisplayUtils.getIconHtml(icon) +" "+ accessLevel +"</span>");
		}
	}
	
}
