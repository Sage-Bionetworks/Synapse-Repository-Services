package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Layer extends Place {
	
	private String layerId;
	private String datasetId;
	private Boolean download;
	
	public Layer(String layerId, String datasetId, Boolean download) {
		this.layerId = layerId;
		this.datasetId = datasetId;
		this.download = download;
	}

	public String toToken() {
		// mandatory tokens
		String token = layerId + ";Dataset:" + datasetId;
		
		// add optional tokens if they are defined
		if(download != null) token += ";Download:" + download.toString();
		
		return token;
	}
		
	public String getLayerId() {
		return layerId;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public Boolean getDownload() {
		return download;
	}
	
	public static class Tokenizer implements PlaceTokenizer<Layer> {
        @Override
        public String getToken(Layer place) {
            return place.toToken();
        }

        @Override
        public Layer getPlace(String token) {        	
        	String layerId = token.replaceAll(";.*", "");
        	String datasetId = null;
        	Boolean download = null;
        	
        	// parse other tokens
	        	String[] subtokens = token.split(";");
	        	// skip first token as it is the layerid
	        	for(int i=1; i<subtokens.length; i++) {
	        		if(subtokens[i] != null) {
		        		String[] keyValue = subtokens[i].split(":");
		        		// only parse valid subtokens
		        		if(keyValue != null && keyValue.length==2) {	        			
		        			if("Dataset".equals(keyValue[0]))
		        				datasetId = keyValue[1];
		        			if("Download".equals(keyValue[0]) && "true".equals(keyValue[1]))
	        					download = true;	        			
		        		}
	        		}
	        	}
        	        	
            return new Layer(layerId, datasetId, download); 
        }
    }

}
