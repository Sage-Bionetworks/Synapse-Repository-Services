package org.sagebionetworks.grid.db.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.operation.Delete;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    @Mock
    private GridIndexDao mockDao;

    @InjectMocks
    private DeleteHandler handler;

    private String sessionId;
    private Long replicaId;

    @BeforeEach
    public void before() {
        sessionId = "sessionOne";
        replicaId = 123L;
    }

    @Test
    public void testGetOperationType() {
        assertEquals(OperationType.del, handler.getOperationType());
    }

    @Test
    public void testHandleBatch() {
    	LogicalTimestamp nodeOne = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
    	List<Timespan> nodeOneIds = List.of(
    		new Timespan(new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(2L), 10L)
    	);
    	
    	LogicalTimestamp nodeTwo = new LogicalTimestamp().setReplicaId(456L).setSequenceNumber(1L);
    	List<Timespan> nodeTwoIds = List.of(
    		new Timespan(new LogicalTimestamp().setReplicaId(456L).setSequenceNumber(3L), 1L),
    		new Timespan(new LogicalTimestamp().setReplicaId(456L).setSequenceNumber(5L), 2L)
    	);
    	
        List<Delete> batch = List.of(
        	new Delete(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L), nodeOne, nodeOneIds),
        	new Delete(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(2L), nodeTwo, nodeTwoIds)
        );

        // Call under test
        Set<LogicalTimestamp> result = handler.handleBatch(sessionId, replicaId, batch);

        assertEquals(Set.of(nodeOne, nodeTwo), result);
        
        verify(mockDao).deleteRgaNodes(sessionId, replicaId, nodeOne, nodeOneIds);
        verify(mockDao).deleteRgaNodes(sessionId, replicaId, nodeTwo, nodeTwoIds);
        verifyNoMoreInteractions(mockDao);
    }
    
    @Test
    public void testHandleBatchWithEmptyBatch() {

        // Call under test
        Set<LogicalTimestamp> result = handler.handleBatch(sessionId, replicaId, Collections.emptyList());

        assertEquals(Collections.emptySet(), result);
        
        verifyZeroInteractions(mockDao);
    }
}