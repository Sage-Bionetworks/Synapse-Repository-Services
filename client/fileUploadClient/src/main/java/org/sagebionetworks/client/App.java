package org.sagebionetworks.client;

import java.awt.Color;
import java.awt.Font;

import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.collections.Map;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;
import org.apache.pivot.wtk.HorizontalAlignment;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.VerticalAlignment;
import org.apache.pivot.wtk.Window;
import org.sagebionetworks.StackConfiguration;

public class App implements Application {
	private Window window = null;
	private static String sessionToken;
	private static String parentId;
	
	public static void main(String[] args) {			
		if(args != null && args.length >= 2) {
			sessionToken = args[0];
			parentId = args[1];
		}
		DesktopApplicationContext.main(App.class, args);				
	}

	@Override
    public void startup(Display display, Map<String, String> properties) throws Exception {
		if(sessionToken == null || parentId == null ) {
			showUsage(display);
			return;
		}
		
		FileUploader fileUploader = WidgetFactory.createFileUploader();
		window = fileUploader.asWidget();		
        window.open(display);

        fileUploader.configure(createSynapseClient(), parentId);       
    }

	@Override
	public boolean shutdown(boolean optional) {
		if (window != null) {
			window.close();
		}

		return false;
	}

	@Override
	public void suspend() {
	}

	@Override
	public void resume() {
	}

	/*
	 * Private Methods
	 */
	private static Synapse createSynapseClient() {
		// Create a new syanpse			
		Synapse synapseClient = new Synapse();
		synapseClient.setSessionToken(sessionToken);
		synapseClient.setRepositoryEndpoint(StackConfiguration.getRepositoryServiceEndpoint());
		synapseClient.setAuthEndpoint(StackConfiguration.getAuthenticationServicePublicEndpoint());
		synapseClient.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());
		return synapseClient;
	}

	private static void showUsage(Display display) {
			Window window = new Window();
			
	        Label label = new Label();
	        label.setText("Application not configured properly.");
	        label.getStyles().put("font", new Font("Arial", Font.BOLD, 24));
	        label.getStyles().put("color", Color.RED);
	        label.getStyles().put("horizontalAlignment",
	            HorizontalAlignment.CENTER);
	        label.getStyles().put("verticalAlignment",
	            VerticalAlignment.CENTER);
	 
	        window.setContent(label);
	        window.setTitle("Error");
	        window.setMaximized(true);
	 
	        window.open(display);	
	}

}