package org.sagebionetworks.web.unitclient.widget.licenseddownloader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloaderView;
import org.sagebionetworks.web.shared.LicenseAgreement;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class LicensedDownloaderTest {
		
	LicensedDownloader licensedDownloader;
	LicensedDownloaderView mockView;
	
	@Before
	public void setup(){		
		mockView = Mockito.mock(LicensedDownloaderView.class);		
		licensedDownloader = new LicensedDownloader(mockView);
		
		verify(mockView).setPresenter(licensedDownloader);
	}
	
	@Test
	public void testAsWidget(){
		licensedDownloader.asWidget();
	}
	
	@Test 
	public void testSetLicenseAgreement() {
		LicenseAgreement licenseAgreement = new LicenseAgreement();
		String licenseHtml = "license";		
		licenseAgreement.setLicenseHtml(licenseHtml);
		
		// test license only
		licensedDownloader.setLicenseAgreement(licenseAgreement);				
		verify(mockView).setLicenseHtml(licenseHtml);
		
		reset(mockView);
		
		// test license and citation
		String citationHtml = "citation";
		licenseAgreement.setCitationHtml(citationHtml);
		licensedDownloader.setLicenseAgreement(licenseAgreement);
		verify(mockView).setCitationHtml(citationHtml);
		verify(mockView).setLicenseHtml(licenseHtml);		
	}
	
	@Test
	public void testSetLicenseAccepted() {
		@SuppressWarnings("unchecked")
		AsyncCallback<Void> callback = mock(AsyncCallback.class);
		licensedDownloader.setLicenseAcceptedCallback(callback);
		
		licensedDownloader.setLicenseAccepted();		
		verify(callback).onSuccess(null);
		verify(mockView).setLicenceAcceptanceRequired(false);
	}
}
