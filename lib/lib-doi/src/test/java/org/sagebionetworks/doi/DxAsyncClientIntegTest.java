package org.sagebionetworks.doi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.doi.Doi;

public class DxAsyncClientIntegTest {

	@Before
	public void before() {

	}

	@Test
	public void testResolvingDoi() {

		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		final String doi = "doi:10.7303/syn1720822.1";
		ezidDoi.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		ezidDoi.setMetadata(metadata);

		long delay = 100L;
		long decay = 50L;
		DxAsyncClient dxClient = new DxAsyncClient(delay, decay);
		dxClient.resolve(ezidDoi, new DxAsyncCallback() {
			@Override
			public void onSuccess(EzidDoi ezidDoi) {
			}
			@Override
			public void onError(EzidDoi ezidDoi, Exception e) {
				fail();
			}
		});
	}

	@Test
	public void testNonResolvingDoi() {

		final EzidDoi ezidDoi = new EzidDoi();
		final Doi dto = new Doi();
		ezidDoi.setDto(dto);
		final String doi = "doi:10.7303/duygh989837979";
		ezidDoi.setDoi(doi);
		final EzidMetadata metadata = new EzidMetadata();
		ezidDoi.setMetadata(metadata);

		long delay = 100L;
		long decay = 30L;
		DxAsyncClient dxClient = new DxAsyncClient(delay, decay);
		final long start = System.currentTimeMillis();
		dxClient.resolve(ezidDoi, new DxAsyncCallback() {
			@Override
			public void onSuccess(EzidDoi ezidDoi) {
				fail();
			}
			@Override
			public void onError(EzidDoi ezidDoi, Exception e) {
				long stop = System.currentTimeMillis();
				assertTrue((stop - start) > (100 + 70 + 40 + 10));
			}
		});
	}
}
