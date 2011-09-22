package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.PersistSuccessEvent;
import org.sagebionetworks.web.client.events.PersistSuccessHandler;
import org.sagebionetworks.web.client.widget.adminmenu.AdminMenu;
import org.sagebionetworks.web.client.widget.breadcrumb.Breadcrumb;
import org.sagebionetworks.web.client.widget.editpanels.AnnotationEditor;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
import org.sagebionetworks.web.client.widget.modal.ModalWindow;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton.AccessLevel;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.DivElement;
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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

@SuppressWarnings("unused")
public class DatasetViewImpl extends Composite implements DatasetView {	

	public interface Binder extends UiBinder<Widget, DatasetViewImpl> {
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
	DivElement rClientCodeDiv;
	@UiField 
	SpanElement breadcrumbTitleSpan;
	@UiField
	SimplePanel annotationsPanel;
	@UiField
	SimplePanel tablePanel;
	@UiField
	SimplePanel downloadPanel;
	@UiField
	SimplePanel followDatasetPanel;
	@UiField
	SimplePanel seeTermsPanel;
	@UiField
	SimplePanel accessPanel;
	@UiField
	SpanElement accessSpan;
	@UiField
	SimplePanel adminPanel;
	@UiField
	SimplePanel addLayerPanel;
	// DEMO Panels
	@UiField
	SimplePanel demoComments;
	@UiField
	SimplePanel demoAnalysis;

	private Presenter presenter;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private IconsImageBundle iconsImageBundle;
	private QueryServiceTableResourceProvider queryServiceTableResourceProvider;
	private QueryServiceTable queryServiceTable;
	private final LicensedDownloader datasetLicensedDownloader;
	private Breadcrumb breadcrumb;
	private boolean disableDownloads;
	private ModalWindow followDatasetModal;
	private ModalWindow seeTermsModal;
	private AccessMenuButton accessMenuButton;
	private NodeEditor nodeEditor;
	private AnnotationEditor annotationEditor;
	private AdminMenu adminMenu;
	private boolean isAdministrator = false; 
	private boolean canEdit = false;
	private Header headerWidget;
	
	
	@Inject
	public DatasetViewImpl(
			Binder uiBinder,
			Header headerWidget,
			Footer footerWidget,
			IconsImageBundle iconsImageBundle,
			final PreviewDisclosurePanel previewDisclosurePanel,
			QueryServiceTableResourceProvider queryServiceTableResourceProvider,
			LicensedDownloader licensedDownloader, Breadcrumb breadcrumb,
			final ModalWindow followDatasetModal,
			final ModalWindow seeTermsModal,
			AccessMenuButton accessMenuButton,
			NodeEditor nodeEditor,
			AnnotationEditor annotationEditor,
			AdminMenu adminMenu) {		

		disableDownloads = false;
		initWidget(uiBinder.createAndBindUi(this));
		this.iconsImageBundle = iconsImageBundle;
		this.previewDisclosurePanel = previewDisclosurePanel;
		this.datasetLicensedDownloader = licensedDownloader;
		this.followDatasetModal = followDatasetModal;
		this.accessMenuButton = accessMenuButton;
		this.nodeEditor = nodeEditor;
		this.annotationEditor = annotationEditor;
		this.adminMenu = adminMenu;
		this.queryServiceTableResourceProvider = queryServiceTableResourceProvider;
		this.seeTermsModal = seeTermsModal;
		this.headerWidget = headerWidget;
		
		// header setup
		header.clear();
		footer.clear();
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItems.DATASETS);

		// alignment setup
//		middleFlexTable.setCellSpacing(5);
//		rightFlexTable.setCellSpacing(5);
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
		this.headerWidget.refresh();
		this.headerWidget.setPlaceChanger(presenter.getPlaceChanger());
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
	public void setDatasetDetails(String id, 
								  String name, 
								  String overviewText,
								  String[] diseases, 
								  String[] species, 
								  int studySize,
								  String tissueTumor,
								  String[] tissueTypes, 
								  String referencePublicationDisplay,
								  String referencePublicationUrl, 
								  int nOtherPublications,
								  String viewOtherPublicationsUrl, 
								  Date postedDate,
								  Date curationDate, 
								  Date lastModifiedDate,
								  String creator, 
								  String[] contributors, 
								  int nFollowers,
								  String viewFollowersUrl, 
								  String downloadAvailability,
								  String releaseNotesUrl, 
								  String status, 
								  String version,
								  int nSamples, 
								  int nDownloads, 
								  String citation, 
								  Integer pubmedId, 
								  boolean isAdministrator, 
								  boolean canEdit) {

		// assure reasonable values
		if(id == null) id = "";
		if(name == null) name = "";
		if(overviewText == null) overviewText = "";
		if(diseases == null) diseases = new String[0];
		if(species == null) species = new String[0];
		if(tissueTypes == null) tissueTypes = new String[0];
		if(referencePublicationDisplay == null) referencePublicationDisplay = "";
		if(referencePublicationUrl == null) referencePublicationUrl = "";
		if(viewOtherPublicationsUrl == null) viewOtherPublicationsUrl = "";
		if(contributors == null) contributors = new String[0];
		if(viewFollowersUrl == null) viewFollowersUrl = "";
		if(downloadAvailability == null) downloadAvailability = "";
		if(releaseNotesUrl == null) releaseNotesUrl = "";
	
		// Clear everything
		clear();
		
		// check authorization
		this.isAdministrator = isAdministrator;
		this.canEdit = canEdit;
		createAccessPanel(id);
		createAdminPanel(id);		
		
		datasetLicensedDownloader.clear();
		setupDatasetLicensedDownloaderCallbacks();
 
		Anchor followDatasetAnchor = setupFollowDatasetModal();		
		followDatasetPanel.clear();
		followDatasetPanel.add(followDatasetAnchor);	

		setupLayerTable(id);
		tablePanel.clear();
		tablePanel.add(queryServiceTable.asWidget());
				
		// download link		
		Anchor downloadLink = setupDatasetDownloadLink();		
		downloadPanel.clear();
		downloadPanel.add(downloadLink);
								
		// fill in fields
		titleSpan.setInnerText(name);
		synapseIdSpan.setInnerText(DisplayConstants.SYNAPSE_ID_PREFIX + id);
		rClientCodeDiv.setInnerHTML(DisplayUtils.getRClientEntityLoad(id));
		rClientCodeDiv.setClassName(DisplayUtils.STYLE_CODE_CONTENT);
		breadcrumbTitleSpan.setInnerText(name);
		
		int summaryLength = overviewText.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH : overviewText.length();
		previewDisclosurePanel.init("Expand", overviewText.substring(0, summaryLength), overviewText);
		overviewPanel.clear();
		overviewPanel.add(previewDisclosurePanel);		
		
		annotationsPanel.clear();
		annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
		annotationEditor.setResource(NodeType.DATASET, id);
		annotationsPanel.add(annotationEditor.asWidget());
		// add metadata to tables
//		int rowIndex = 0;
//		DisplayUtils.addRowToTable(rowIndex++, "Disease(s):", join(diseases, ", "), middleFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Species:", join(species, ", "), middleFlexTable);		
//		DisplayUtils.addRowToTable(rowIndex++, "Tissue Type(s):", join(tissueTypes, ", "), middleFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Tissue/Tumor:", tissueTumor, middleFlexTable);
//		if(referencePublicationUrl != null)
//			DisplayUtils.addRowToTable(rowIndex++, "Reference Publication:", "<a href=\""+ referencePublicationUrl + "\" target=\"_new\">" + referencePublicationDisplay + "</a>", middleFlexTable);
//		else 
//			DisplayUtils.addRowToTable(rowIndex++, "Reference Publication:", referencePublicationDisplay, middleFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Other Publications:", nOtherPublications + " <a href=\""+ viewOtherPublicationsUrl + "\" target=\"_new\">view</a>", middleFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Status:", status, middleFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Version:", version, middleFlexTable);
//
//		rowIndex = 0;
//		if(postedDate != null)
//			DisplayUtils.addRowToTable(rowIndex++, "Posted:", DisplayConstants.DATE_FORMAT.format(postedDate), rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Creator:", "<a href=\"people_charles.html\">"+ creator + "</a>", rightFlexTable);
//		if(curationDate != null)
//			DisplayUtils.addRowToTable(rowIndex++, "Curated On:", DisplayConstants.DATE_FORMAT.format(curationDate), rightFlexTable);
//		if(lastModifiedDate != null)
//			DisplayUtils.addRowToTable(rowIndex++, "Last Modified On:", DisplayConstants.DATE_FORMAT.format(lastModifiedDate), rightFlexTable);						
//		DisplayUtils.addRowToTable(rowIndex++, "Contributor(s)/Institution:", join(contributors, "<br/>"), rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Followers:", nFollowers + " <a href=\""+ viewFollowersUrl + "\" target=\"_new\">view</a>", rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Number of Samples:", Integer.toString(nSamples), rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Number of Downloads:", Integer.toString(nDownloads), rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Download Availability:", downloadAvailability, rightFlexTable);
//		DisplayUtils.addRowToTable(rowIndex++, "Release Notes:", "<a href=\""+ releaseNotesUrl + "\" target=\"_new\">view</a>", rightFlexTable);	
		
		/*
		 * DEMO STRINGS
		 */
		if(DisplayConstants.showDemoHtml) {
			demoComments.clear();
			demoComments.add(new HTML(DisplayConstants.DEMO_COMMENTS));
			
			demoAnalysis.clear();
			demoAnalysis.add(new HTML(DisplayConstants.DEMO_ANALYSIS));
		}
	}

	
	@Override
	public void setLicenseAgreement(LicenseAgreement agreement) {		
		datasetLicensedDownloader.setLicenseAgreement(agreement);
		Anchor seeTermsAnchor = setupTermsModal(agreement);
		seeTermsPanel.clear();		
		seeTermsPanel.add(seeTermsAnchor);
	}

	@Override
	public void requireLicenseAcceptance(boolean requireLicense) {
		datasetLicensedDownloader.setRequireLicenseAcceptance(requireLicense);		
	}

	@Override
	public void disableLicensedDownloads(boolean disable) {
		this.disableDownloads = true;
	}

	@Override
	public void setDatasetDownloads(List<FileDownload> downloads) {		
		datasetLicensedDownloader.setDownloadUrls(downloads);
	}

	/*
	 * Private Methods
	 */
	private void clear() {
		titleSpan.setInnerText("");
		rClientCodeDiv.setInnerHTML("");
//		middleFlexTable.clear();
//		middleFlexTable.removeAllRows();
//		rightFlexTable.clear();
//		rightFlexTable.removeAllRows();
	}

	private void createAdminPanel(String id) {		
		if(canEdit) {
//			annotationEditor.setPlaceChanger(presenter.getPlaceChanger());
//			annotationEditor.setResource(NodeType.DATASET, id);
			
			Button button = new Button("Admin Menu");
			button.setIcon(AbstractImagePrototype.create(iconsImageBundle.adminTools16()));
			//adminButton.setIconAlign(IconAlign.LEFT);
			button.setMenu(createAdminMenu(id));
			button.setHeight(25);
			adminPanel.clear();
			adminPanel.add(button);
			
			
		}
		
		if(canEdit) {
			// add dataset button on page			
			addLayerPanel.clear();			
			addLayerPanel.add(createAddLayerLink(id));			
		}		
	}

	private Anchor createAddLayerLink(final String datasetId) {
		Anchor addLayerLink = new Anchor();
		addLayerLink.setHTML(AbstractImagePrototype.create(iconsImageBundle.addSquare16()).getHTML() + " " + DisplayConstants.BUTTON_ADD_LAYER);
		addLayerLink.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				showAddLayerWindow(datasetId);
			}
		});
		return addLayerLink;
	}

	private Menu createAdminMenu(final String datasetId) {
		Menu menu = new Menu();		
		MenuItem item = null; 

		// Edit menu options
		if(canEdit) {			
			item = new MenuItem("Edit Dataset Details");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.applicationEdit16()));		
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					final Window window = new Window();  
					window.setSize(600, 345);
					window.setPlain(true);
					window.setModal(true);
					window.setBlinkModal(true);
					window.setHeading("Edit Dataset");
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
					window.add(nodeEditor.asWidget(NodeType.DATASET, datasetId), new FitData(4));				
					window.show();
				}
			});
			menu.add(item);
						 
			item = new MenuItem(DisplayConstants.BUTTON_ADD_A_LAYER_TO_DATASET);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.documentAdd16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {													
					showAddLayerWindow(datasetId);
				}

			});
			menu.add(item);
		}
		
		// Administrator Menu Options
		if(isAdministrator) {
			item = new MenuItem("Delete Dataset");
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.deleteButton16()));
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				public void componentSelected(MenuEvent menuEvent) {
					MessageBox.confirm("Delete Dataset", "Are you sure you want to delete this dataset?", new Listener<MessageBoxEvent>() {					
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
			accessMenuButton.createAccessButton(accessLevel, NodeType.DATASET, id);
			accessPanel.clear();
			accessPanel.add(accessMenuButton.asWidget());
		} else {
			accessSpan.setInnerHTML("<span class=\"setting_label\">Access: </span><span class=\"setting_level\">"+ DisplayUtils.getIconHtml(icon) +" "+ accessLevel +"</span>");
		}
	}
	
	
	private void setupDatasetLicensedDownloaderCallbacks() {
		// give the LicensedDownloader something to call when the view accepts the license
		datasetLicensedDownloader.setLicenseAcceptedCallback(new AsyncCallback<Void>() {
			// called when the user agrees to the license 
			@Override
			public void onSuccess(Void result) {
				// let presenter know so it can persist this
				presenter.licenseAccepted();
			}

			// not used
			@Override
			public void onFailure(Throwable caught) { }

		});
	}

	private void setupLayerTable(String id) {
		// layers table
		queryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.layer, false, 320, 237, presenter.getPlaceChanger());		
		// Set the where clause
		List<WhereCondition> whereList = new ArrayList<WhereCondition>();
		whereList.add(new WhereCondition("layer.parentId", WhereOperator.EQUALS, id));
		this.queryServiceTable.setWhereCondition(whereList);
	}

	private Anchor setupDatasetDownloadLink() {
		Anchor downloadLink = new Anchor();
		downloadLink.setHTML(DisplayUtils.getIconHtml(iconsImageBundle.NavigateDown16()) + " " + DisplayConstants.BUTTON_DOWNLOAD_DATASET);
		downloadLink.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				if(presenter.downloadAttempted()) {
					//datasetLicensedDownloader.showWindow();
					showErrorMessage("<strong>Alpha Note</strong>: Downloading of entire dataset is currently not operational. You can download layers individually though.");
				}
			}
		});
		return downloadLink;
	}

	private Anchor setupTermsModal(LicenseAgreement licenseAgreement) {
		seeTermsModal.setHeading(DisplayConstants.TITLE_TERMS_OF_USE);
		seeTermsModal.setDimensions(400, 500);
		seeTermsModal.setHtml(licenseAgreement.getLicenseHtml()); 
		// download link		
		Anchor seeTermsAnchor = new Anchor();
		seeTermsAnchor.setHTML(DisplayUtils.getIconHtml(iconsImageBundle.documentText16()) + " " + DisplayConstants.BUTTON_SEE_TERMS_OF_USE);
		seeTermsAnchor.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				seeTermsModal.showWindow();
			}
		});
		return seeTermsAnchor;
	}

	private Anchor setupFollowDatasetModal() {
		followDatasetModal.setHeading("Follow this Dataset");
		followDatasetModal.setDimensions(180, 500);		
		followDatasetModal.setHtml(DisplayConstants.FOLLOW_DATASET_HTML);
		followDatasetModal.setCallbackButton("Confirm", new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				// TODO : call a service layer to follow the dataset				
				followDatasetModal.hideWindow();
			}

			@Override
			public void onFailure(Throwable caught) {			}
		});
		// follow link		
		Anchor followDatasetAnchor = new Anchor();
		followDatasetAnchor.setHTML(AbstractImagePrototype.create(iconsImageBundle.arrowCurve16()).getHTML() + " " + DisplayConstants.BUTTON_FOLLOW_DATASET);
		followDatasetAnchor.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				followDatasetModal.showWindow();
				showErrorMessage("<strong>Alpha Note</strong>: Following datasets is currently not operational");
			}
		});
		return followDatasetAnchor;
	}
	
	private String join(String[] list, String delimiter) {
		String returnStr = "";
		for(String str : list) {
			if(!"".equals(returnStr)) returnStr += delimiter;
			returnStr += str;
		}
		return returnStr;
	}

	private void showAddLayerWindow(final String datasetId) {
		final Window window = new Window();  
		window.setSize(600, 275);
		window.setPlain(true);
		window.setModal(true);
		window.setBlinkModal(true);
		window.setHeading("Create Layer");
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
		window.add(nodeEditor.asWidget(NodeType.LAYER, null, datasetId), new FitData(4));
		window.show();
	}

}
