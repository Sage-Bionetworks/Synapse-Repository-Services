package org.sagebionetworks.repo.manager.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.handler.EntityMetadataHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.service.EntityBundleService;


import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EntityMetadataHandlerTest {
    private static final String ACTION_GROUP = "org_sage_zero";
    private static final String FUNCTION = "org_sage_zero_get_entity_metadata";
    private static final String ID = "syn123";
    private static final long USER_ID = 123;
    private static  final Parameter SYN_ID = new Parameter("synId", "string", ID);

    @Mock
    private EntityBundleService entityBundleService;
    @InjectMocks
    private EntityMetadataHandler entityMetadataHandler;

    private ReturnControlEvent returnControlEvent;
    private EntityBundle  entityBundle = new EntityBundle();

    @Test
    public void testGetEntityMetadataHandlerWithoutSynId() {
        returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, Collections.emptyList());
        String resultMessage = assertThrows(IllegalArgumentException.class, () -> {
            entityMetadataHandler.handleEvent(returnControlEvent);
        }).getMessage();

        assertEquals("Parameter 'synId' of type string is required", resultMessage);
    }

    @Test
    public void testGetEntityMetadataHandler() throws Exception {
        returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, List.of(SYN_ID));

        EntityBundleRequest entityBundleRequest = new EntityBundleRequest().setIncludeAccessControlList(true).setIncludeEntity(true)
                .setIncludeAnnotations(true).setIncludeEntityPath(true).setIncludeHasChildren(true)
                .setIncludePermissions(true).setIncludeTableBundle(true);

        when(entityBundleService.getEntityBundle(USER_ID, ID, entityBundleRequest))
                .thenReturn(entityBundle);
        entityMetadataHandler.handleEvent(returnControlEvent);
        verify(entityBundleService).getEntityBundle(returnControlEvent.getRunAsUserId(),
                returnControlEvent.getParameters().get(0).getValue(), entityBundleRequest);
    }
}
