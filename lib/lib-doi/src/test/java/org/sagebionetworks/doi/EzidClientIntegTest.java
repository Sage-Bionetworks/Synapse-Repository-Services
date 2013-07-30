package org.sagebionetworks.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.doi.Doi;

public class EzidClientIntegTest {

	@Test
	public void testCreateGet() throws Exception {
		final EzidDoi doiCreate = new EzidDoi();
		final Doi dto = new Doi();
		doiCreate.setDto(dto);
		String id = Integer.toHexString(random.nextInt());
		final String doi = EzidConstants.DOI_PREFIX + id;
		assertTrue(doi.startsWith("doi:10.5072/FK2."));
		doiCreate.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		final String target = EzidConstants.TARGET_URL_PREFIX;
		assertTrue(target.startsWith("http"));
		assertTrue(target.endsWith("/#!Synapse:"));
		assertFalse(target.endsWith("//#!Synapse:"));
		metadata.setTarget(target);
		final String creator = "Test, Something-Interesting";
		metadata.setCreator(creator);
		final String title = "This is a test! (And I mean it.)";
		metadata.setTitle(title);
		final String publisher = EzidConstants.PUBLISHER;
		metadata.setPublisher(publisher);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);
		doiCreate.setMetadata(metadata);
		DoiClient client = new EzidClient();
		if (client.isStatusOk()) {
			client.create(doiCreate);
			EzidDoi doiGet = client.get(doiCreate);
			assertNotNull(doiGet);
			assertEquals(doi, doiGet.getDoi());
			assertEquals(target, doiGet.getMetadata().getTarget());
			assertEquals(creator, doiGet.getMetadata().getCreator());
			assertEquals(title, doiGet.getMetadata().getTitle());
			assertEquals(publisher, doiGet.getMetadata().getPublisher());
			assertEquals(year, doiGet.getMetadata().getPublicationYear());
			assertNotNull(doiGet.getMetadata().getOriginalMetadata());
		}
	}

	@Test
	public void testCreateUpdate() throws Exception {
		final EzidDoi doiCreate = new EzidDoi();
		final Doi dto = new Doi();
		doiCreate.setDto(dto);
		String id = Integer.toHexString(random.nextInt());
		final String doi = EzidConstants.DOI_PREFIX + id;
		doiCreate.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		final String target = EzidConstants.TARGET_URL_PREFIX;
		metadata.setTarget(target);
		final String creator = "Test, Something";
		metadata.setCreator(creator);
		final String title = "This is a test";
		metadata.setTitle(title);
		final String publisher = EzidConstants.PUBLISHER;
		metadata.setPublisher(publisher);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);
		doiCreate.setMetadata(metadata);
		DoiClient client = new EzidClient();
		if (client.isStatusOk()) {
			client.create(doiCreate);
			// Now change the target and update
			final String newTarget = target + "#!Home";
			metadata.setTarget(newTarget);
			client.update(doiCreate);
			EzidDoi doiGet = client.get(doiCreate);
			assertNotNull(doiGet);
			assertEquals(doi, doiGet.getDoi());
			assertEquals(newTarget, doiGet.getMetadata().getTarget());
			assertEquals(creator, doiGet.getMetadata().getCreator());
			assertEquals(title, doiGet.getMetadata().getTitle());
			assertEquals(publisher, doiGet.getMetadata().getPublisher());
			assertEquals(year, doiGet.getMetadata().getPublicationYear());
			assertNotNull(doiGet.getMetadata().getOriginalMetadata());
		}
	}

	@Test(expected=RuntimeException.class)
	public void testCreateInvalidDoi() throws Exception {
		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		String id = Integer.toHexString(random.nextInt());
		// Invalid domain
		final String doi = "doi:10.99999/test.invalid." + id;
		ezidDoi.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		final String target = EzidConstants.TARGET_URL_PREFIX;
		metadata.setTarget(target);
		final String creator = "Test, Something";
		metadata.setCreator(creator);
		final String title = "This is a test";
		metadata.setTitle(title);
		final String publisher = EzidConstants.PUBLISHER;
		metadata.setPublisher(publisher);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);
		ezidDoi.setMetadata(metadata);
		DoiClient client = new EzidClient();
		if (client.isStatusOk()) {
			client.create(ezidDoi);
		}
	}

	@Test
	public void testCreateDoiAlreadyExists() throws Exception {
		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		final String doi = EzidConstants.DOI_PREFIX + "3829383478";
		ezidDoi.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		final String target = EzidConstants.TARGET_URL_PREFIX;
		metadata.setTarget(target);
		final String creator = "Test, Something";
		metadata.setCreator(creator);
		final String title = "This is a test";
		metadata.setTitle(title);
		final String publisher = EzidConstants.PUBLISHER;
		metadata.setPublisher(publisher);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);
		ezidDoi.setMetadata(metadata);
		DoiClient client = new EzidClient();
		if (client.isStatusOk()) {
			client.create(ezidDoi);
		}
	}

	// If the doi does not exist, EZID does not return
	// HttpStatus.SC_NOT_FOUND as of now. Instead it returns
	// HttpStatus.SC_BAD_REQUEST "no such identifier".
	@Test(expected=RuntimeException.class)
	public void testGetDoiNotFoundException() {
		EzidDoi ezidDoi = new EzidDoi();
		ezidDoi.setDoi(EzidConstants.DOI_PREFIX + 819079303);
		ezidDoi.setDto(new Doi());
		ezidDoi.setMetadata(new EzidMetadata());
		DoiClient client = new EzidClient();
		if (client.isStatusOk()) {
			client.get(ezidDoi);
		}
	}

	private final Random random = new Random();
}
