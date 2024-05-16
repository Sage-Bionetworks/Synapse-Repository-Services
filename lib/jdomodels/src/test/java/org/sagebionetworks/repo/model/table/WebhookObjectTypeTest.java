package org.sagebionetworks.repo.model.table;

import java.util.Arrays;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;

public class WebhookObjectTypeTest {
    @Test
    public void testWebhookObjectTypeIsSubsetOfObjectType() {
        // Attempt to get each corresponding object type, will throw an exception one doesn't exist
        Arrays.stream(WebhookObjectType.values()).forEach(type -> ObjectType.valueOf(type.name()));
    }
}
