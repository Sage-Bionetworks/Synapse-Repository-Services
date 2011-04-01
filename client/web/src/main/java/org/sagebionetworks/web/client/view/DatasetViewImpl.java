package org.sagebionetworks.web.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.presenter.DatasetRow;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
import org.sagebionetworks.web.client.widget.table.QueryServiceTable;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.WhereCondition;

import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DatasetViewImpl extends Composite implements DatasetView {

	private final int DESCRIPTION_SUMMARY_LENGTH = 50; // characters for summary

	public interface Binder extends UiBinder<Widget, DatasetViewImpl> {
	}

	@UiField
	FlowPanel overviewPanel;
	@UiField
	SpanElement titleSpan;
	@UiField
	FlexTable middleFlexTable;
	@UiField
	FlexTable rightFlexTable;
	@UiField
	SimplePanel tablePanel;
	@UiField
	SimplePanel downloadPanel;
	@UiField
	SpanElement downloadSpan;

	private Presenter presenter;
	private PreviewDisclosurePanel previewDisclosurePanel;
	private QueryServiceTable queryServiceTable;
	private final LicensedDownloader datasetLicensedDownloader;
	private boolean disableDownloads;

	@Inject
	public DatasetViewImpl(Binder uiBinder, IconsImageBundle icons, final PreviewDisclosurePanel previewDisclosurePanel, QueryServiceTableResourceProvider queryServiceTableResourceProvider, LicensedDownloader licensedDownloader) {		
		disableDownloads = false;
		initWidget(uiBinder.createAndBindUi(this));
		this.previewDisclosurePanel = previewDisclosurePanel;
		this.datasetLicensedDownloader = licensedDownloader;
		setupDatasetLicensedDownloaderCallbacks();
		
		middleFlexTable.setCellSpacing(5);
		rightFlexTable.setCellSpacing(5);

		// layers table
		queryServiceTable = new QueryServiceTable(queryServiceTableResourceProvider, ObjectType.layer, false, 320, 275);
		tablePanel.add(queryServiceTable.asWidget());
		
		// download dataset button
		Button downloadDatasetButton = new Button("Download Dataset", new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				datasetLicensedDownloader.showWindow();			
			}
		}); 		
//		downloadPanel.add(downloadDatasetButton);
		
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
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showErrorMessage(String message) {
		Window.alert(message);
	}

	@Override
	public void setDatasetDetails(String id, String name, String overviewText,
			String[] diseases, String[] species, int studySize,
			String[] tissueTypes, String referencePublicationDisplay,
			String referencePublicationUrl, int nOtherPublications,
			String viewOtherPublicationsUrl, Date postedDate,
			Date curationDate, String[] contributors, int nFollowers,
			String viewFollowersUrl, String downloadAvailability,
			String releaseNotesUrl) {

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
		
		int summaryLength = overviewText.length() >= DESCRIPTION_SUMMARY_LENGTH ? DESCRIPTION_SUMMARY_LENGTH : overviewText.length();
		previewDisclosurePanel.init("Expand", overviewText.substring(0, summaryLength), overviewText);
		overviewPanel.add(previewDisclosurePanel);
		
		// add metadata to tables
		int rowIndex = 0;
		addRowToTable(rowIndex++, "Disease(s):", join(diseases, ", "), middleFlexTable);
		addRowToTable(rowIndex++, "Species:", join(species, ", "), middleFlexTable);
		addRowToTable(rowIndex++, "Tissue Type(s):", join(tissueTypes, ", "), middleFlexTable);
		addRowToTable(rowIndex++, "Reference Publication:", "<a href=\""+ referencePublicationUrl + "\" target=\"_new\">" + referencePublicationDisplay + "</a>", middleFlexTable);
		addRowToTable(rowIndex++, "Other Publications:", nOtherPublications + " <a href=\""+ viewOtherPublicationsUrl + "\" target=\"_new\">view</a>", middleFlexTable);

		rowIndex = 0;
		if(postedDate != null)
			addRowToTable(rowIndex++, "Posted:", DisplayConstants.DATE_FORMAT.format(postedDate), rightFlexTable);
		if(curationDate != null)
			addRowToTable(rowIndex++, "Curated:", DisplayConstants.DATE_FORMAT.format(curationDate), rightFlexTable);				
		addRowToTable(rowIndex++, "Contributor(s)/Institution:", join(contributors, "<br/>"), rightFlexTable);
		addRowToTable(rowIndex++, "Followers:", nFollowers + " <a href=\""+ viewFollowersUrl + "\" target=\"_new\">view</a>", rightFlexTable);
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
