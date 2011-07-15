package org.sagebionetworks.web.client.view;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;

import com.google.gwt.place.shared.Place;
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
	
	/**
	 * Shows a info box with the given message
	 * @param title
	 * @param message
	 */
	public void showInfo(String title, String message);
	
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
								  boolean canEdit);
	
//	public void setDatasetRow(DatasetRow row);
	
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

		/**
		 * Refreshes the object on the page
		 */
		public void refresh();
		
		public void licenseAccepted();
		
		public void goTo(Place place);
		
		public PlaceChanger getPlaceChanger();

		/**
		 * Determine if a download screen can be displayed to the user
		 * @return true if the user should be shown the download screen 
		 */
		public boolean downloadAttempted();

		public void delete();
}

	


}
