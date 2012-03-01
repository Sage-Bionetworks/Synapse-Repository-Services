package org.sagebionetworks.workflow.curation;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.workflow.Constants;
import org.sagebionetworks.workflow.Notification;
import org.sagebionetworks.workflow.UnrecoverableException;

import com.amazonaws.services.simpleworkflow.flow.ActivityFailureException;

/**
 * 
 * @author deflaux
 * 
 */
public class TcgaActivitiesImpl implements TcgaActivities {
	
	@Override
	public String createMetadata(String datasetId, String tcgaUrl,
			Boolean doneIfExists) throws ClientProtocolException, NoSuchAlgorithmException, UnrecoverableException, IOException, HttpClientHelperException, SynapseException, JSONException  {
		String layerId = null;
		try {
			layerId = TcgaCuration
					.createMetadata(
							datasetId, tcgaUrl, doneIfExists);
		} catch (SocketTimeoutException e) {
			throw new ActivityFailureException("Communication timeout, try this again");
		}
		return layerId;
	}
	
	@Override
	public String updateLocation(String layerId, String tcgaUrl) throws ClientProtocolException, NoSuchAlgorithmException, UnrecoverableException, IOException, HttpClientHelperException, SynapseException, JSONException  {
		boolean locationWasUpdated;
		try {
			locationWasUpdated = TcgaCuration
					.updateLocation(tcgaUrl, layerId);
		} catch (SocketTimeoutException e) {
			throw new ActivityFailureException("Communication timeout, try this again");
		}
		if(!locationWasUpdated) {
			return Constants.WORKFLOW_DONE;
		}
		return layerId;
	}

	@Override
	public String formulateNotificationMessage(String layerId) throws SynapseException, JSONException, UnrecoverableException {
		return TcgaCuration.formulateLayerNotificationMessage(layerId);
	}

	@Override
	public void notifyFollowers(String recipient, String subject, String message) {
		Notification.doSnsNotifyFollowers(TcgaWorkflowConfigHelper.getSNSClient(), recipient, subject, message);
		
	}
}
