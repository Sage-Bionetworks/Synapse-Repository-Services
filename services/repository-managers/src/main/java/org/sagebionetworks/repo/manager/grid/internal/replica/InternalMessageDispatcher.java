package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.grid.db.MessageChain;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Component;

@Component
public class InternalMessageDispatcher {

	private final GridReplicaManager gridReplicaManager;
	private final GridIndexManager gridIndexManager;

	public InternalMessageDispatcher(GridReplicaManager gridReplicaManager, GridIndexManager gridIndexManager) {
		this.gridReplicaManager = gridReplicaManager;
		this.gridIndexManager = gridIndexManager;
	}

	public void dispatchMessage(JsonRxMessageBundle bundle) {
		ValidateArgument.required(bundle, "bundle");
		ValidateArgument.required(bundle.getConnection(), "bundle.connection");
		ValidateArgument.required(bundle.getMessage(), "bundle.message");
		ValidateArgument.required(bundle.getProgressCallback(), "bundle.callback");

		if (!handleMessage(bundle)) {
			throw new IllegalArgumentException(String.format("Cannot handle: '%s'", bundle.getMessage().toJson()));
		}
	}

	private boolean handleMessage(JsonRxMessageBundle bundle) {
		switch (bundle.getMessage().getType()) {
		case Notification:
			return handleNotification(bundle);
		case ResponseData:
			return handleResponseData(bundle);
		case ResponseComplete:
			gridReplicaManager.onResponseComplete(bundle.getProgressCallback(), bundle.getConnection(),
					bundle.getMessage().getId().get());
			return true;
		default:
			return false;
		}
	}

	private boolean handleNotification(JsonRxMessageBundle bundle) {
		String method = bundle.getMessage().getMethod().get();
		switch (method) {
		case "connected":
		case "replica-connected":
			gridReplicaManager.onConnected(bundle.getProgressCallback(), bundle.getConnection());
			return true;
		case "new-patch":
			gridReplicaManager.onNewPatch(bundle.getProgressCallback(), bundle.getConnection());
			return true;
		default:
			return false;
		}
	}

	private boolean handleResponseData(JsonRxMessageBundle bundle) {
		if (bundle.getMessage().getId().isEmpty()) {
			throw new IllegalArgumentException("ResponseData must have an ID.");
		}

		Optional<MessageChain> chain = gridIndexManager.getMessageChain(bundle.getConnection().getSessionId(),
				bundle.getConnection().getReplicaId(), bundle.getMessage().getId().get());
		if (chain.isEmpty()) {
			throw new IllegalArgumentException(
					String.format("No message chain found for session: %s, replica: %d, id: %d",
							bundle.getConnection().getSessionId(), bundle.getConnection().getReplicaId(),
							bundle.getMessage().getId().get()));
		}

		if (GridReplicaManager.SYNCHRONIZE_CLOCK.equals(chain.get().getMethod())) {
			JSONObject messageBody = (JSONObject) bundle.getMessage().getBody().get();
			if (!messageBody.has("type")) {
				throw new IllegalArgumentException("ResponseData body must have a 'type' field.");
			}
			String type = messageBody.getString("type");
			switch (type) {
			case "snapshot": {
				String urlString = messageBody.getString("body");
				URL url;
				try {
					url = new URL(urlString);
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException("Invalid snapshot URL: " + urlString, e);
				}
				gridReplicaManager.onApplySnapshot(bundle.getProgressCallback(), bundle.getConnection(),
						bundle.getMessage().getId().get(), url);
				return true;
			} case "patches": {
				JSONArray patchesAsJson = messageBody.getJSONArray("body");
				List<Patch> patchList = new ArrayList<>(patchesAsJson.length());
				for (int i = 0; i < patchesAsJson.length(); i++) {
					JSONArray patchJson = patchesAsJson.getJSONArray(i);
					patchList.add(PatchCompactSerializable.deserialize(patchJson));
				}
				gridReplicaManager.onApplyPatches(bundle.getProgressCallback(), bundle.getConnection(),
						bundle.getMessage().getId().get(), patchList);

				return true;
			} default:
				throw new IllegalArgumentException("Unknown ResponseData body type: " + type);
			}
		}
		return false;
	}
}
