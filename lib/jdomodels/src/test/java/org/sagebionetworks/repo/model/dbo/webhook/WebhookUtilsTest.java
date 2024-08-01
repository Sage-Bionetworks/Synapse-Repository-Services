package org.sagebionetworks.repo.model.dbo.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;

public class WebhookUtilsTest {
	
	@Test
	public void testSynapseObjectTypeEnum() {
		// Verifies that the SynapseObjectType enum is a subset of the ObjectType
		Arrays.stream(SynapseObjectType.values()).forEach( t -> ObjectType.valueOf(t.name()));
	}

	@Test
	public void testEventsToJsonRoundTrip() {
		Set<SynapseEventType> events = new TreeSet<>(List.of(SynapseEventType.UPDATE, SynapseEventType.DELETE, SynapseEventType.CREATE));
		
		String result = WebhookUtils.eventsToJson(events);
		
		assertEquals(events, WebhookUtils.eventsFromJson(result));
	}

}
