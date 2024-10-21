package org.sagebionetworks.repo.manager.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.handler.GetEntityChildrenHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.service.EntityService;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class GetEntityChildrenHandlerTest {
    private static final String ACTION_GROUP = "org_sage_zero";
    private static final String FUNCTION = "org_sage_zero_get_entity_children";
    private static final String ID = "syn123";
    private static final String TOKEN = "50as50";
    private static final String ENTITY = "dataset";
    private static final long USER_ID = 123;


    @Mock
    private EntityService entityService;
    @InjectMocks
    private GetEntityChildrenHandler entityChildrenHandler;

    private ReturnControlEvent returnControlEvent;
    private EntityChildrenResponse entityChildrenResponse = new EntityChildrenResponse();
    private Parameter SYN_ID;
    private Parameter NEXT_PAGE_TOKEN;
    private Parameter ENTITY_TYPE;

    @BeforeEach
    public void before() {
        SYN_ID = new Parameter("synId", "string", ID);
        NEXT_PAGE_TOKEN = new Parameter("nextPageToken", "string", TOKEN);
        ENTITY_TYPE = new Parameter("entityType", "string", ENTITY);
    }

    @Test
    public void testGetEntityChildrenWithoutSynId() {
        returnControlEvent = new ReturnControlEvent(123L, ACTION_GROUP, FUNCTION, List.of(NEXT_PAGE_TOKEN));
        String resultMessage = assertThrows(IllegalArgumentException.class, () -> {
            entityChildrenHandler.handleEvent(returnControlEvent);
        }).getMessage();

        assertEquals("Parameter 'synId' of type string is required", resultMessage);
    }

    @Test
    public void testGetEntityChildrenWithoutNextPageToken() throws Exception {
        returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, List.of(SYN_ID));

        EntityChildrenRequest expectedRequest = new EntityChildrenRequest().setParentId(ID).setIncludeSumFileSizes(true)
                .setIncludeTotalChildCount(true).setSortBy(SortBy.MODIFIED_ON).setSortDirection(Direction.DESC)
                .setIncludeTypes(Arrays.stream(EntityType.values()).collect(Collectors.toList()))
                .setNextPageToken(null);

        when(entityService.getChildren(anyLong(), any())).thenReturn(entityChildrenResponse);
        entityChildrenHandler.handleEvent(returnControlEvent);
        verify(entityService).getChildren(USER_ID, expectedRequest);

    }


    @Test
    public void testGetEntityChildrenWithNextPageToken() throws Exception {
        returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, List.of(SYN_ID, NEXT_PAGE_TOKEN));

        EntityChildrenRequest expectedRequest = new EntityChildrenRequest().setParentId(ID).setIncludeSumFileSizes(true)
                .setIncludeTotalChildCount(true).setSortBy(SortBy.MODIFIED_ON).setSortDirection(Direction.DESC)
                .setIncludeTypes(Arrays.stream(EntityType.values()).collect(Collectors.toList()))
                .setNextPageToken(TOKEN);

        when(entityService.getChildren(anyLong(), any())).thenReturn(entityChildrenResponse);
        entityChildrenHandler.handleEvent(returnControlEvent);
        verify(entityService).getChildren(USER_ID, expectedRequest);

    }

    @Test
    public void testGetEntityChildrenWithEntityType() throws Exception {
        returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, List.of(SYN_ID, ENTITY_TYPE));

        EntityChildrenRequest expectedRequest = new EntityChildrenRequest().setParentId(ID).setIncludeSumFileSizes(true)
                .setIncludeTotalChildCount(true).setSortBy(SortBy.MODIFIED_ON).setSortDirection(Direction.DESC)
                .setIncludeTypes(List.of(EntityType.dataset))
                .setNextPageToken(null);

        when(entityService.getChildren(anyLong(), any())).thenReturn(entityChildrenResponse);
        entityChildrenHandler.handleEvent(returnControlEvent);
        verify(entityService).getChildren(USER_ID, expectedRequest);

    }

    @Test
    public void testGetEntityChildrenWithAllParameters() throws Exception {
        returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, List.of(SYN_ID, NEXT_PAGE_TOKEN, ENTITY_TYPE));

        EntityChildrenRequest expectedRequest = new EntityChildrenRequest().setParentId(ID).setIncludeSumFileSizes(true)
                .setIncludeTotalChildCount(true).setSortBy(SortBy.MODIFIED_ON).setSortDirection(Direction.DESC)
                .setIncludeTypes(List.of(EntityType.dataset))
                .setNextPageToken(TOKEN);

        when(entityService.getChildren(anyLong(), any())).thenReturn(entityChildrenResponse);
        entityChildrenHandler.handleEvent(returnControlEvent);
        verify(entityService).getChildren(USER_ID, expectedRequest);

    }
}
