package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiCreator;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceType;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceTypeGeneral;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.model.doi.v2.DoiTitle;

@ExtendWith(ITTestExtension.class)
public class IT065SynapseJavaClientDoiV2Test {

	private static final long RETRY_TIME = 1000L;

	private static Entity entity;
	
	private static StackConfiguration config;
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;

	@BeforeAll
	public static void beforeClass(SynapseAdminClient configuredAdmin, SynapseClient configuredClient, StackConfiguration configuredConfig) throws Exception {
		adminSynapse = configuredAdmin;
		synapse = configuredClient;
		config = configuredConfig;
		assumeTrue(config.getDoiDataciteEnabled());
		entity = new Project();
		entity.setName("IT065SynapseJavaClientDoiV2Test" + UUID.randomUUID());
		entity = synapse.createEntity(entity);
		assertNotNull(entity);
	}

	@AfterAll
	public static void afterClass() throws Exception {
		if (config.getDoiDataciteEnabled()) {
			if (entity != null) {
				synapse.deleteEntity(entity);
			}
			adminSynapse.clearDoi();
		}
	}

	@Test
	public void testCreateGetUpdate() throws SynapseException, InterruptedException {
		Doi doiToMint = setUpRequestDoi();

		Doi doiRetrieved = createOrUpdateDoiRetrieveAndValidate(doiToMint);

		DoiCreator newCreator = new DoiCreator();
		newCreator.setCreatorName("A different creator");

		// We modify and send back the retrieved object since it has the updated Etag and all other information.
		doiRetrieved.setCreators(Collections.singletonList(newCreator));
		createOrUpdateDoiRetrieveAndValidate(doiRetrieved);
	}

	@Test
	public void testCreateGetNullVersion() throws SynapseException, InterruptedException {
		Doi doiToMint = setUpRequestDoi();
		doiToMint.setObjectVersion(null);
		createOrUpdateDoiRetrieveAndValidate(doiToMint);
	}

	@Test
	public void testGetNotFoundException() throws SynapseException {
		
		assertThrows(SynapseNotFoundException.class, () -> {			
			synapse.getDoiAssociation("syn8395713", ObjectType.ENTITY, null);
		});
	}

	@Test
	public void testGetPortalUrl() throws SynapseException {
		assertNotNull(synapse.getPortalUrl("syn1236464", ObjectType.ENTITY, 5L));
	}

	private static Doi setUpRequestDoi() {
		Doi doi = new Doi();
		doi.setObjectId(entity.getId());
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(1L);

		doi.setPublicationYear(2018L);
		DoiTitle title = new DoiTitle();
		title.setTitle("A DOI for a Synapse integration test.");
		doi.setTitles(Collections.singletonList(title));
		DoiCreator creator = new DoiCreator();
		creator.setCreatorName("Someone running an integration test");
		doi.setCreators(Collections.singletonList(creator));
		DoiResourceType resourceType = new DoiResourceType();
		resourceType.setResourceTypeGeneral(DoiResourceTypeGeneral.Dataset);
		doi.setResourceType(resourceType);
		return doi;
	}

	private static Doi createOrUpdateDoiRetrieveAndValidate(Doi doiToMint) throws SynapseException, InterruptedException {
		String jobToken = synapse.createOrUpdateDoiAsyncStart(doiToMint);
		DoiResponse response =  null;
		boolean succesfullyMinted = false;
		while (!succesfullyMinted) {
			Thread.sleep(RETRY_TIME);
			try {
				response = synapse.createOrUpdateDoiAsyncGet(jobToken);
				if (response.getDoi() != null) {
					succesfullyMinted = true;
				}
			} catch (SynapseResultNotReadyException e) {
				assertNotEquals(e.getJobStatus().getJobState(), AsynchJobState.FAILED);
			}
		}
		Doi doiRetrieved = response.getDoi();

		assertNotNull(doiRetrieved);
		assertEquals(doiRetrieved.getObjectId(), doiToMint.getObjectId());
		assertEquals(doiRetrieved.getObjectType(), doiToMint.getObjectType());
		assertEquals(doiRetrieved.getObjectVersion(), doiToMint.getObjectVersion());
		assertEquals(doiRetrieved.getTitles(), doiToMint.getTitles());
		assertEquals(doiRetrieved.getCreators(), doiToMint.getCreators());
		assertEquals(doiRetrieved.getResourceType(), doiToMint.getResourceType());
		assertEquals(doiRetrieved.getPublicationYear(), doiToMint.getPublicationYear());
		String expectedUser = synapse.getMyProfile().getOwnerId();
		assertEquals(doiRetrieved.getAssociatedBy(), expectedUser);
		assertEquals(doiRetrieved.getUpdatedBy(), expectedUser);
		assertNotNull(doiRetrieved.getEtag());
		assertEquals(entity.getId(), doiRetrieved.getObjectId());
		assertEquals(ObjectType.ENTITY, doiRetrieved.getObjectType());
		assertNotNull(doiRetrieved.getUpdatedOn());

		return doiRetrieved;
	}
}
