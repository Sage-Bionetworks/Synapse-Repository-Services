package org.sagebionetworks.web.client.widget.licenseddownloader;

import java.util.List;

import org.sagebionetworks.web.shared.FileDownload;

import com.google.gwt.user.client.ui.IsWidget;

public interface LicensedDownloaderView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * Make the view show the License acceptance view first
	 * @param licenseRequired
	 */
	public void setLicenceAcceptanceRequired(boolean licenseRequired);
	
	/**
	 * Set the license text to display
	 * @param licenseHtml
	 */
	public void setLicenseHtml(String licenseHtml);	
	
	/**
	 * Set the citation text to display
	 * @param citationHtml
	 */
	public void setCitationHtml(String citationHtml);
	
	/**
	 * Show the License Box window
	 */
	public void showWindow();
	
	
	/**
	 * Hide the License Box window
	 */
	public void hideWindow();
	
	/**
	 * Sets the content of the download pane
	 * @param displayText
	 * @param fileUrls
	 * @param checksums
	 */
	void setDownloadUrls(List<FileDownload> downloads);
	
	
	/**
	 * Presenter Interface 
	 */
	public interface Presenter {
		
		/**
		 * Call when the user accepts the presented License Agreement
		 */
		public void setLicenseAccepted();
		
	}
	
}
