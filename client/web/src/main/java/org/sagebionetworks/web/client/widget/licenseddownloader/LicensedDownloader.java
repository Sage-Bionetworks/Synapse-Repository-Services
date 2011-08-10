package org.sagebionetworks.web.client.widget.licenseddownloader;

import java.util.List;

import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * Licensed Downloader Presenter
 * @author dburdick
 *
 */
public class LicensedDownloader implements LicensedDownloaderView.Presenter {
	
	private LicensedDownloaderView view;
	private boolean requireLicenseAcceptance;
	private AsyncCallback<Void> licenseAcceptedCallback;	
	
	@Inject
	public LicensedDownloader(LicensedDownloaderView view) {
		this.view = view;
		view.setPresenter(this);
	}
	
	/*
	 * Public methods
	 */		
	public void showWindow() {
		this.view.showWindow();
	}
	
	public void hideWindow() {
		this.view.hideWindow();
	}
	
	public void setLicenseAgreement(LicenseAgreement agreement) {
		if(agreement.getCitationHtml() != null) { 
			view.setCitationHtml(agreement.getCitationHtml());
		}
		view.setLicenseHtml(agreement.getLicenseHtml());
	}
	
	public void setDownloadUrls(List<FileDownload> downloads) {		
		this.view.setDownloadUrls(downloads);
	}
	
	public void showLoading() {
		this.view.showLoading();
	}
	
	public Widget asWidget() {
		view.setPresenter(this);
		return view.asWidget();
	}	
	
	public void clear() {
		view.clear();
		this.licenseAcceptedCallback = null;		
	}
	
	public void setRequireLicenseAcceptance(boolean requireLicenseAcceptance) {
		this.requireLicenseAcceptance = requireLicenseAcceptance;
		this.view.setLicenceAcceptanceRequired(requireLicenseAcceptance);
	}
	
	
	public void setLicenseAcceptedCallback(AsyncCallback<Void> callback) {
		this.licenseAcceptedCallback = callback;
	}
	
	@Override
	public void setLicenseAccepted() {		
		// send out to using class to let know of acceptance
		licenseAcceptedCallback.onSuccess(null);
		// allow the view to skip the license agreement now and show the download view
		setRequireLicenseAcceptance(false);
		showWindow();		
	}

}
