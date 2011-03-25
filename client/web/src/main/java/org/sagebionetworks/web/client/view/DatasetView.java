package org.sagebionetworks.web.client.view;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.web.client.presenter.DatasetRow;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;



/**
 * Defines the communication between the view and presenter for a view of a single datasets.
 * 
 * @author jmhill
 *
 */
public interface DatasetView extends IsWidget {
	
	/**
	 * This how the view communicates with the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * The view pops-up an error dialog.
	 * @param message
	 */
	public void showErrorMessage(String message);
	
	
//	public void setDatasetDetails(String name,
//								  String overviewText,
//								  String[] diseases,
//								  String[] species,
//								  int studySize,
//								  String[] tissueTypes,
//								  String referencePublicationDisplay,
//								  String referencePublicationUrl,
//								  int nOtherPublications,
//								  String viewOtherPublicationsUrl,
//								  Date postedDate,
//								  Date curationDate,
//								  String[] contributors,
//								  int nFollowers,
//								  String viewFollowersUrl,
//								  );
	
	public void setDatasetRow(DatasetRow row);
	
	/**
	 * require the view to show the license agreement
	 * @param requireLicense
	 */
	public void requireLicenseAcceptance(boolean requireLicense);
	
	/**
	 * the license agreement to be shown
	 * @param agreement
	 */
	public void setLicenseAgreement(LicenseAgreement agreement);
	
	/**
	 * Set the list of files available via the whole dataset download
	 * @param downloads
	 */
	public void setDatasetDownloads(List<FileDownload> downloads);
	
	/**
	 * Disables the downloading of files
	 * @param disable
	 */
	public void disableLicensedDownloads(boolean disable);
		
	/**
	 * Defines the communication with the presenter.
	 *
	 */
	public interface Presenter {

		public void licenseAccepted();
		
		public void logDownload();
	}

	


}
