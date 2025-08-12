package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.ConstantProvider;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObjectBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObjectBuilder;

@ExtendWith(MockitoExtension.class)
public class ChangePatchBuilderTest {

	@Mock
	private PatchPublisher mockPatchPublisher;
	@Mock
	private ConstantProvider mockConstantProvider;
	@Captor
	private ArgumentCaptor<JSONArray> jsonArrayCaptor;

	private Long maxBytesPerPatch;
	private String sessionId;
	private Long replicaId;
	private String connectionId;
	private GridConnectionInfo con;
	private LogicalTimestamp currentClock;

	private ChangePatchBuilder builder;

	@BeforeEach
	public void before() {
		connectionId = "con123";
		replicaId = 3L;
		sessionId = "session34";
		maxBytesPerPatch = 300L;
		con = new GridConnectionInfo().setConnectionId(connectionId).setSessionId(sessionId).setReplicaId(replicaId);
		currentClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(99L);
		builder = Mockito.spy(
				new ChangePatchBuilder(mockPatchPublisher, mockConstantProvider, con, currentClock, maxBytesPerPatch));
	}

	@Test
	public void testAddOperationBuilder() throws IOException {

		when(mockConstantProvider.findExistingConstant(sessionId, replicaId, "[\"foo\"]")).thenReturn(Optional.empty());

		// call under test
		LogicalTimestamp conId = builder
				.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "foo")));

		assertEquals(LogicalTimestamp.newIncrement(currentClock, 1), conId);
		// call under test
		builder.close();
		verify(mockPatchPublisher).publishPatch(eq(con), jsonArrayCaptor.capture());
		assertEquals("[[[3,100]],[0,\"foo\"]]", jsonArrayCaptor.getValue().toString());
	}

	@Test
	public void testAddOperationBuilderWithConstantInIndex() throws IOException {
		LogicalTimestamp existing = new LogicalTimestamp().setReplicaId(33L).setSequenceNumber(401L);
		when(mockConstantProvider.findExistingConstant(sessionId, replicaId, "[\"foo\"]"))
				.thenReturn(Optional.of(existing));

		// call under test
		LogicalTimestamp conId = builder
				.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "foo")));

		assertEquals(existing, conId);
		// call under test
		builder.close();
		verify(mockPatchPublisher, never()).publishPatch(any(), any());
	}

	@Test
	public void testAddOperationBuilderWithConstantsTwice() throws IOException {
		LogicalTimestamp fooId = new LogicalTimestamp().setReplicaId(33L).setSequenceNumber(401L);
		when(mockConstantProvider.findExistingConstant(sessionId, replicaId, "[\"foo\"]"))
				.thenReturn(Optional.of(fooId));

		// call under test
		LogicalTimestamp fooResult = builder
				.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "foo")));
		LogicalTimestamp fooAgain = builder
				.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "foo")));

		assertEquals(fooId, fooResult);
		assertEquals(fooId, fooAgain);
		// call under test
		builder.close();
		verify(mockConstantProvider, times(1)).findExistingConstant(any(), any(), any());
		verify(mockPatchPublisher, never()).publishPatch(any(), any());
	}

	@Test
	public void testAddOperationBuilderWithMultipleTypes() throws IOException {
		LogicalTimestamp fooId = new LogicalTimestamp().setReplicaId(33L).setSequenceNumber(401L);
		when(mockConstantProvider.findExistingConstant(sessionId, replicaId, "[\"foo\"]"))
				.thenReturn(Optional.of(fooId));
		when(mockConstantProvider.findExistingConstant(sessionId, replicaId, "[\"bar\"]")).thenReturn(Optional.empty());

		// call under test
		LogicalTimestamp fooResult = builder
				.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "foo")));
		assertEquals(fooId, fooResult);
		LogicalTimestamp barId = builder
				.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "bar")));
		assertEquals(LogicalTimestamp.newIncrement(currentClock, 1), barId);

		LogicalTimestamp newObId = builder.addOperationBuilder(new NewObjectBuilder());
		LinkedHashMap<String, LogicalTimestamp> obMap = new LinkedHashMap<>();
		obMap.put("fooKey", fooResult);
		obMap.put("barKey", barId);
		LogicalTimestamp inserObId = builder
				.addOperationBuilder(new InsertObjectBuilder().setObjectId(newObId).setMap(obMap));

		// adding the same constants again should be a no-op
		builder.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "foo")));
		builder.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "bar")));

		// call under test
		builder.close();
		verify(mockConstantProvider, times(2)).findExistingConstant(any(), any(), any());
		verify(mockPatchPublisher).publishPatch(eq(con), jsonArrayCaptor.capture());
		assertEquals("[[[3,100]],[0,\"bar\"],[2],[10,101,[[\"fooKey\",[33,401]],[\"barKey\",100]]]]",
				jsonArrayCaptor.getValue().toString());
	}

	@Test
	public void testAddOperationBuilderWithOverSizeLimit() throws IOException {
		when(mockConstantProvider.findExistingConstant(any(), any(), any())).thenReturn(Optional.empty());

		// call under test
		builder.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "a".repeat(150))));
		builder.addOperationBuilder(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, "b".repeat(200))));

		// call under test
		builder.close();
		verify(mockConstantProvider, times(2)).findExistingConstant(any(), any(), any());
		verify(mockPatchPublisher, times(2)).publishPatch(any(), any());

		verify(mockPatchPublisher, times(2)).publishPatch(eq(con), jsonArrayCaptor.capture());
		assertEquals(
				"[[[3,100]],[0,\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
						+ "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"]]",
				jsonArrayCaptor.getAllValues().get(0).toString());
		assertEquals(
				"[[[3,101]],[0,\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
						+ "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
						+ "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"]]",
				jsonArrayCaptor.getAllValues().get(1).toString());
	}
}
