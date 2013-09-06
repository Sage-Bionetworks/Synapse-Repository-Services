package org.sagebionetworks.client.fileuploader;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

import org.apache.pivot.collections.Map;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;
import org.apache.pivot.wtk.HorizontalAlignment;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.VerticalAlignment;
import org.apache.pivot.wtk.Window;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.client.Synapse;

public class App implements Application {
	
	private static Logger logger = Logger.getLogger(App.class.getName());
	
	private Window window = null;
	private static String sessionToken;
	private static String entityId;
	
	public static void main(String[] args) {
		logger.info("starting app");
		if(args != null && args.length >= 2) {
			for(String arg : args) {
				if(arg.startsWith("--sessionToken=")) sessionToken = arg.replace("--sessionToken=", "");
				if(arg.startsWith("--entityId=")) entityId = arg.replace("--entityId=", "");
			}
			logger.info("sessiontoken: "+ sessionToken + ", parentId: "+ entityId);
		}
		DesktopApplicationContext.main(App.class, args);				
	}

	@Override
    public void startup(Display display, Map<String, String> properties) throws Exception {
		if(sessionToken == null || entityId == null ) {
			showImproperConfig(display);
			throw new IllegalArgumentException("Both sessionToken and entityId must be defined");
		}
		
		FileUploader fileUploader = WidgetFactory.createFileUploader();
		window = fileUploader.asWidget();		
        window.open(display);

        fileUploader.configure(createSynapseClient(), entityId);       
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

	private static void showImproperConfig(Display display) {
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