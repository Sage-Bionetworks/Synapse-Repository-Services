package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;

@ExtendWith(ITTestExtension.class)
public class ITGridControllerTest {

	private static long MAX_TME_MS = 30 * 1000;

	private final SynapseClient synapse;

	public ITGridControllerTest(SynapseClient synapse) {
		this.synapse = synapse;
	}

	@Test
	public void testPingGrid() throws AssertionError, SynapseException, URISyntaxException, InterruptedException {
		// call under test
		CreateGridResponse resposne = (CreateGridResponse) AsyncJobHelper
				.assertAysncJobResult(synapse, AsynchJobType.CreateGrid, new CreateGridRequest(), body -> {
					assertTrue(body instanceof CreateGridResponse);
					CreateGridResponse response = (CreateGridResponse) body;
					assertNotNull(response.getGridSession());
					assertNotNull(response.getGridSession().getSessionId());
				}, MAX_TME_MS, AsyncJobHelper.INFINITE_RETRIES).getResponse();

		GridSession session = resposne.getGridSession();

		// call under test
		GridSession clone = synapse.getGridSession(session.getSessionId());
		assertEquals(session, clone);

		// call under test
		CreateReplicaResponse replicaResponse = synapse
				.createGridReplica(new CreateReplicaRequest().setGridSessionId(session.getSessionId()));
		assertNotNull(replicaResponse);
		assertNotNull(replicaResponse.getReplica());
		GridReplica replica = replicaResponse.getReplica();

		// call under test
		GridReplica replicaClone = synapse.getGridReplica(replica.getGridSessionId(), replica.getReplicaId());
		assertEquals(replica, replicaClone);

		// call under test
		CreateGridPresignedUrlResponse urlResponse = synapse.createGridPresignedUrl(new CreateGridPresignedUrlRequest()
				.setGridSessionId(replica.getGridSessionId()).setReplicaId(replica.getReplicaId()));
		assertNotNull(urlResponse);
		assertNotNull(urlResponse.getPresignedUrl());

		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket ws = AsyncJobHelper.createConnection(urlResponse.getPresignedUrl(), incomingMessages);

		ws.sendText(new JSONArray("[8,\"ping\"]").toString(), true).join();
		assertTrue(AsyncJobHelper.waitForMessage(8, "pong", incomingMessages));
		ws.sendClose(4999, "closing").join();
	}

}
