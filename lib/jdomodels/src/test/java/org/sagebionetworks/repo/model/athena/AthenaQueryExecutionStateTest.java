package org.sagebionetworks.repo.model.athena;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.athena.model.QueryExecutionState;

@ExtendWith(MockitoExtension.class)
public class AthenaQueryExecutionStateTest {

	@Test
	public void testExecutionStateTranslation() {
		// We test all the possible values that are supported by AWS
		for (QueryExecutionState value : QueryExecutionState.values()) {
			AthenaQueryExecutionState.valueOf(value.toString());
		}
	}
	
	
}
