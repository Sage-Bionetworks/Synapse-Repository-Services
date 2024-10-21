package org.sagebionetworks.repo.manager.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.agent.handler.GetDescriptionHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.service.WikiService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetDescriptionHandlerTest {
    private static final String ACTION_GROUP = "org_sage_zero";
    private static final String FUNCTION = "org_sage_zero_get_description";
    private static final String ID = "syn123";
    private static final long USER_ID = 123;
    private static  final Parameter SYN_ID = new Parameter("synId", "string", ID);

    @Mock
    private WikiService wikiService;
    @InjectMocks
    private GetDescriptionHandler getDescriptionHandler;
    private ReturnControlEvent returnControlEvent;

    @Test
    public void testGetEntityDescriptionHandlerWithoutSynId() {
        returnControlEvent = new ReturnControlEvent(123L, ACTION_GROUP, FUNCTION, Collections.emptyList());
        String resultMessage = assertThrows(IllegalArgumentException.class, () -> {
            getDescriptionHandler.handleEvent(returnControlEvent);
        }).getMessage();

        assertEquals("Parameter 'synId' of type string is required", resultMessage);
    }


    @Test
    public void testGetEntityDescriptionHandler() throws Exception {
        returnControlEvent = new ReturnControlEvent(USER_ID, ACTION_GROUP, FUNCTION, List.of(SYN_ID));
        WikiHeader wikiHeader = new WikiHeader().setId("headerOne").setTitle("test header").setParentId("syn345");
        WikiPage page = new WikiPage().setId("TestPage").setTitle("random page").setMarkdown("markdown");
        WikiPageKey key = new WikiPageKey().setOwnerObjectId(ID)
                .setOwnerObjectType(ObjectType.ENTITY).setWikiPageId("headerOne");
        PaginatedResults<WikiHeader> wikiHeaders = PaginatedResults.createWithLimitAndOffset(List.of(wikiHeader), 5l,0l);

        when(wikiService.getWikiHeaderTree(USER_ID, ID, ObjectType.ENTITY, 5l, 0l))
                .thenReturn(wikiHeaders);
        when(wikiService.getWikiPage(USER_ID, key, null))
                .thenReturn(page);

        String result = getDescriptionHandler.handleEvent(returnControlEvent);
        assertEquals("{\"description\":\"random page\\nmarkdown\\n\\n\"}", result);
        verify(wikiService).getWikiHeaderTree(USER_ID, ID, ObjectType.ENTITY, 5l,0l);
        verify(wikiService).getWikiPage(USER_ID,key,null);
    }
}
