package org.sagebionetworks.doi;

import java.util.Calendar;
import java.util.Random;

import org.junit.Test;
import org.sagebionetworks.repo.model.doi.Doi;

public class EzidClientIntegTest {

	@Test
	public void testCreate() throws Exception {
		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		String id = Integer.toHexString(random.nextInt());
		final String doi = EzidConstants.DOI_PREFIX + id;
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
		client.create(ezidDoi);
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
		client.create(ezidDoi);
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
		client.create(ezidDoi);
	}

	private final Random random = new Random();
}
