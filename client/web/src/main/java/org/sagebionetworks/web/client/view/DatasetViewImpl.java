package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.xpath.operations.Mod;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.presenter.DatasetRow;
import org.sagebionetworks.web.client.widget.breadcrumb.Breadcrumb;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.header.Header.MenuItem;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
import org.sagebionetworks.web.client.widget.modal.ModalWindow;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.WhereCondition;

import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

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
	SpanElement breadcrumbTitleSpan;
	@UiField
	FlexTable middleFlexTable;
	@UiField
	FlexTable rightFlexTable;
	@UiField
	SimplePanel tablePanel;
	@UiField
	SimplePanel downloadPanel;
	@UiField
	SimplePanel followDatasetPanel;
	@UiField
	SimplePanel seeTermsPanel;

	private Presenter presenter;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private QueryServiceTable queryServiceTable;
	private final LicensedDownloader datasetLicensedDownloader;
	private Breadcrumb breadcrumb;
	private boolean disableDownloads;
	private ModalWindow followDatasetModal;
	private ModalWindow seeTermsModal;

	@Inject
	public DatasetViewImpl(Binder uiBinder, Header headerWidget, Footer footerWidget, IconsImageBundle icons, final PreviewDisclosurePanel previewDisclosurePanel, QueryServiceTableResourceProvider queryServiceTableResourceProvider, LicensedDownloader licensedDownloader, Breadcrumb breadcrumb, final ModalWindow followDatasetModal, final ModalWindow seeTermsModal) {		
		disableDownloads = false;
		initWidget(uiBinder.createAndBindUi(this));
		this.previewDisclosurePanel = previewDisclosurePanel;
		this.datasetLicensedDownloader = licensedDownloader;
		this.followDatasetModal = followDatasetModal; 
		setupDatasetLicensedDownloaderCallbacks();

		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		headerWidget.setMenuItemActive(MenuItem.DATASETS);

		// Button: Follow dataset 
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
		// download link		
		Anchor followDatasetAnchor = new Anchor();
		followDatasetAnchor.setHTML(AbstractImagePrototype.create(icons.arrowCurve16()).getHTML() + " Follow this Dataset");
		followDatasetAnchor.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				followDatasetModal.showWindow();
			}
		});		
		followDatasetPanel.add(followDatasetAnchor);
		

		// Button: See terms of use
		this.seeTermsModal = seeTermsModal;
		seeTermsModal.setHeading("Terms of Use");
		seeTermsModal.setDimensions(400, 500);
		seeTermsModal.setHtml(DisplayConstants.DEFAULT_TERMS_OF_USE); // TODO : get this from a service
		// download link		
		Anchor seeTermsAnchor = new Anchor();
		seeTermsAnchor.setHTML(AbstractImagePrototype.create(icons.documentText16()).getHTML() + " See Terms of Use");
		seeTermsAnchor.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				seeTermsModal.showWindow();
			}
		});		
		seeTermsPanel.add(seeTermsAnchor);
		
		
		
		middleFlexTable.setCellSpacing(5);
		rightFlexTable.setCellSpacing(5);

		// layers table
		queryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.layer, false, 320, 237);
		tablePanel.add(queryServiceTable.asWidget());
				
		// download link		
		Anchor downloadLink = new Anchor();
		downloadLink.setHTML(AbstractImagePrototype.create(icons.download16()).getHTML() + " Download Dataset");
		downloadLink.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				datasetLicensedDownloader.showWindow();
			}
		});
		downloadPanel.add(downloadLink);
		
		// breadcrumb
//		breadcrumb.appendLocation(new Hyperlink("Home", ""));
//		breadcrumb.appendLocation(new Hyperlink("All Datasets", "DatasetsHome:0"));
//		breadcrumbPanel.add(breadcrumb.asWidget());
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showErrorMessage(String message) {
		MessageBox.info("Message", message, null);
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
								  Integer pubmedId) {

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
		
		
		// Set the where clause
		List<WhereCondition> whereList = new ArrayList<WhereCondition>();
		whereList.add(new WhereCondition("dataset.id", WhereOperator.EQUALS, id));
		this.queryServiceTable.setWhereCondition(whereList);
		// Clear everything
		clearAllFields();
		titleSpan.setInnerText(name);
		breadcrumbTitleSpan.setInnerText(name);
//		breadcrumb.setCurrentLocation(name);
//		breadcrumbPanel.add(breadcrumb.asWidget());
		
		int summaryLength = overviewText.length() >= DisplayConstants.DESCRIPTION_SUMMARY_LENGTH ? DisplayConstants.DESCRIPTION_SUMMARY_LENGTH : overviewText.length();
		previewDisclosurePanel.init("Expand", overviewText.substring(0, summaryLength), overviewText);
		overviewPanel.add(previewDisclosurePanel);		
		
		// add metadata to tables
		int rowIndex = 0;
		addRowToTable(rowIndex++, "Disease(s):", join(diseases, ", "), middleFlexTable);
		addRowToTable(rowIndex++, "Species:", join(species, ", "), middleFlexTable);		
		addRowToTable(rowIndex++, "Tissue Type(s):", join(tissueTypes, ", "), middleFlexTable);
		addRowToTable(rowIndex++, "Tissue/Tumor:", tissueTumor, middleFlexTable);
		if(referencePublicationUrl != null)
			addRowToTable(rowIndex++, "Reference Publication:", "<a href=\""+ referencePublicationUrl + "\" target=\"_new\">" + referencePublicationDisplay + "</a>", middleFlexTable);
		else 
			addRowToTable(rowIndex++, "Reference Publication:", referencePublicationDisplay, middleFlexTable);
		addRowToTable(rowIndex++, "Other Publications:", nOtherPublications + " <a href=\""+ viewOtherPublicationsUrl + "\" target=\"_new\">view</a>", middleFlexTable);
		addRowToTable(rowIndex++, "Status:", status, middleFlexTable);
		addRowToTable(rowIndex++, "Version:", version, middleFlexTable);

		rowIndex = 0;
		if(postedDate != null)
			addRowToTable(rowIndex++, "Posted:", DisplayConstants.DATE_FORMAT.format(postedDate), rightFlexTable);
		addRowToTable(rowIndex++, "Creator:", creator, rightFlexTable);
		if(curationDate != null)
			addRowToTable(rowIndex++, "Curated On:", DisplayConstants.DATE_FORMAT.format(curationDate), rightFlexTable);
		if(lastModifiedDate != null)
			addRowToTable(rowIndex++, "Last Modified On:", DisplayConstants.DATE_FORMAT.format(lastModifiedDate), rightFlexTable);						
		addRowToTable(rowIndex++, "Contributor(s)/Institution:", join(contributors, "<br/>"), rightFlexTable);
		addRowToTable(rowIndex++, "Followers:", nFollowers + " <a href=\""+ viewFollowersUrl + "\" target=\"_new\">view</a>", rightFlexTable);
		addRowToTable(rowIndex++, "Number of Samples:", Integer.toString(nSamples), rightFlexTable);
		addRowToTable(rowIndex++, "Number of Downloads:", Integer.toString(nDownloads), rightFlexTable);
		addRowToTable(rowIndex++, "Download Availability:", downloadAvailability, rightFlexTable);
		addRowToTable(rowIndex++, "Release Notes:", "<a href=\""+ releaseNotesUrl + "\" target=\"_new\">view</a>", rightFlexTable);			
		
	}
	
	
	@Override
	public void setDatasetRow(DatasetRow row) {

	}

	/**
	 * Add a row to the provided FlexTable.
	 * 
	 * @param key
	 * @param value
	 * @param table
	 */
	private static void addRowToTable(int row, String key, String value,
			FlexTable table) {
		table.setHTML(row, 0, key);
		table.getCellFormatter().addStyleName(row, 0, "boldRight");
		table.setHTML(row, 1, value);
	}

	private void clearAllFields() {
		titleSpan.setInnerText("");
		middleFlexTable.clear();
		middleFlexTable.removeAllRows();
		rightFlexTable.clear();
		rightFlexTable.removeAllRows();
	}

	@Override
	public void setLicenseAgreement(LicenseAgreement agreement) {		
		datasetLicensedDownloader.setLicenseAgreement(agreement);		
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

	private String join(String[] list, String delimiter) {
		String returnStr = "";
		for(String str : list) {
			if(!"".equals(returnStr)) returnStr += delimiter;
			returnStr += str;
		}
		return returnStr;
	}
}
