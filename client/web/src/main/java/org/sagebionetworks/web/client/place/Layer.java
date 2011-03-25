package org.sagebionetworks.web.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class Layer extends Place {
	
	private String rawToken;
	private String layerId;
	private String datasetId;
	
	public Layer(String layerId, String datasetId) {
		this.layerId = layerId;
		this.datasetId = datasetId;
	}

	public String toToken() {
		return layerId + ";Dataset:" + datasetId;
	}
		
	public String getLayerId() {
		return layerId;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public static class Tokenizer implements PlaceTokenizer<Layer> {
        @Override
        public String getToken(Layer place) {
            return place.toToken();
        }

        @Override
        public Layer getPlace(String token) {
        	String layerId = token.replaceAll(";.*", "");
        	String datasetId = token.replaceAll(".*;Dataset:", "");
            return new Layer(layerId, datasetId); 
        }
    }

}
