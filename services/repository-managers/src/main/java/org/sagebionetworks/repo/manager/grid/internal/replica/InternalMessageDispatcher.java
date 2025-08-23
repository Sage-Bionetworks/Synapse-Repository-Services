package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.Optional;
import org.json.JSONArray;
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
		ValidateArgument.required(bundle.getMessage(), "bundle.messgae");
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
			Patch patch = PatchCompactSerializable.deserialize((JSONArray) bundle.getMessage().getBody().get());
			gridReplicaManager.onApplyPatch(bundle.getProgressCallback(), bundle.getConnection(),
					bundle.getMessage().getId().get(), patch);
			return true;
		}
		return false;
	}
}
