package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.ChangeDataTypeRequest;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.repo.model.FacetPostProcessingParameters;
import org.sagebionetworks.repo.model.Project;

@ExtendWith(ITTestExtension.class)
public class ITEntityDataTypeTest {

	private final SynapseAdminClient adminSynapse;
	private final SynapseClient synapse;

	private List<Entity> entitiesToDelete;
	private Project project;

	public ITEntityDataTypeTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeEach
	public void before() throws SynapseException {
		entitiesToDelete = new LinkedList<>();
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);
	}

	@AfterEach
	public void after() throws Exception {
		for (Entity entity : entitiesToDelete) {
			try {
				synapse.deleteEntity(entity);
			} catch (SynapseException e) {
				// best-effort cleanup
			}
		}
	}

	@Test
	public void testChangeEntityDataTypeWithoutRequestBody() throws SynapseException {
		// The legacy query-parameter form carries no request body. AGGREGATE_DATA/OPEN_DATA
		// require the caller to be a member of the ACT, so the admin performs the change.
		// call under test
		DataTypeResponse response = adminSynapse.changeEntitysDataType(project.getId(), DataType.OPEN_DATA);
		assertNotNull(response);
		assertEquals(project.getId(), response.getObjectId());
		assertEquals(DataType.OPEN_DATA, response.getDataType());
		// the query-parameter form can never carry an aggregate configuration
		assertNull(response.getAggregateDataConfiguration());
	}

	@Test
	public void testChangeEntityDataTypeWithRequestBody() throws SynapseException {
		AggregateDataConfiguration configuration = new AggregateDataConfiguration().setSuppressionThreshold(10L)
				.setFacetPostProcessingConfig(new FacetPostProcessingConfig()
						.setAlgorithm(FacetPostProcessingAlgorithm.ROUNDING)
						.setParameters(new FacetPostProcessingParameters()));
		ChangeDataTypeRequest request = new ChangeDataTypeRequest().setDataType(DataType.AGGREGATE_DATA)
				.setAggregateDataConfiguration(configuration);
		// call under test
		DataTypeResponse response = adminSynapse.changeEntitysDataType(project.getId(), request);
		assertNotNull(response);
		assertEquals(project.getId(), response.getObjectId());
		assertEquals(DataType.AGGREGATE_DATA, response.getDataType());
		// the full bound configuration must round-trip through the request body
		assertEquals(configuration, response.getAggregateDataConfiguration());
	}

	@Test
	public void testChangeEntityDataTypeToAggregateWithoutRequestBody() {
		// AGGREGATE_DATA cannot be expressed through the query-parameter form because it
		// requires a bound configuration that only the request body can carry.
		// call under test
		assertThrows(SynapseBadRequestException.class, () -> {
			adminSynapse.changeEntitysDataType(project.getId(), DataType.AGGREGATE_DATA);
		});
	}
}
