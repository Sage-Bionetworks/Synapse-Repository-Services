package org.sagebionetworks.repo.manager.agent.supervisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.jsonschema.JsonSchemaSpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.jsonschema.JsonSchemaSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.tablequery.TableQuerySpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.tablequery.TableQuerySpecialistFactory;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
public class SupervisorToolsTest {

	@Mock
	private TableQuerySpecialistFactory tableQuerySpecialistFactory;
	@Mock
	private JsonSchemaSpecialistFactory jsonSchemaSpecialistFactory;
	@Mock
	private FileSummarySpecialistFactory fileSummarySpecialistFactory;

	@Mock
	private TableQuerySpecialist tableQuerySpecialist;
	@Mock
	private JsonSchemaSpecialist jsonSchemaSpecialist;
	@Mock
	private FileSummarySpecialist fileSummarySpecialist;

	private SupervisorTools tools;
	private UserInfo userInfo;
	private ToolContext toolContext;

	@BeforeEach
	public void setup() {
		tools = new SupervisorTools(tableQuerySpecialistFactory, jsonSchemaSpecialistFactory, fileSummarySpecialistFactory);
		userInfo = new UserInfo(false, 101L);
		toolContext = new ToolContext(Map.of("userInfo", userInfo, "sessionId", "session-123"));
	}

	@Test
	public void testAskTableQuerySpecialist() {
		when(tableQuerySpecialistFactory.create()).thenReturn(tableQuerySpecialist);
		when(tableQuerySpecialist.chat("describe syn1", userInfo, "session-123")).thenReturn("table described");

		// call under test
		String result = tools.askTableQuerySpecialist("describe syn1", toolContext);

		assertEquals("table described", result);
		// A fresh specialist is created and given the propagated user + session.
		verify(tableQuerySpecialistFactory).create();
		verify(tableQuerySpecialist).chat("describe syn1", userInfo, "session-123");
	}

	@Test
	public void testAskJsonSchemaSpecialist() {
		when(jsonSchemaSpecialistFactory.create()).thenReturn(jsonSchemaSpecialist);
		when(jsonSchemaSpecialist.chat("describe my.org-S", userInfo, "session-123")).thenReturn("schema described");

		// call under test
		String result = tools.askJsonSchemaSpecialist("describe my.org-S", toolContext);

		assertEquals("schema described", result);
		verify(jsonSchemaSpecialistFactory).create();
		verify(jsonSchemaSpecialist).chat("describe my.org-S", userInfo, "session-123");
	}

	@Test
	public void testAskFileSummarySpecialist() {
		when(fileSummarySpecialistFactory.create()).thenReturn(fileSummarySpecialist);
		when(fileSummarySpecialist.chat("summarize out.csv", userInfo, "session-123")).thenReturn("file summarized");

		// call under test
		String result = tools.askFileSummarySpecialist("summarize out.csv", toolContext);

		assertEquals("file summarized", result);
		verify(fileSummarySpecialistFactory).create();
		verify(fileSummarySpecialist).chat("summarize out.csv", userInfo, "session-123");
	}

	@Test
	public void testAskTableQuerySpecialistWithoutSessionId() {
		ToolContext noSession = new ToolContext(Map.of("userInfo", userInfo));
		when(tableQuerySpecialistFactory.create()).thenReturn(tableQuerySpecialist);
		when(tableQuerySpecialist.chat("describe syn1", userInfo, null)).thenReturn("ok");

		// call under test — a null sessionId is forwarded as-is
		String result = tools.askTableQuerySpecialist("describe syn1", noSession);

		assertEquals("ok", result);
		verify(tableQuerySpecialist).chat("describe syn1", userInfo, null);
	}
}
