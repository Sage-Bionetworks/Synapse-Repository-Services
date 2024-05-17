package org.sagebionetworks.repo.manager.monitoring;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.util.VirtualMachineIdProvider;

public class TempDiskProviderImplTest {

	private TempDiskProviderImpl provider;

	@BeforeEach
	public void before() {
		provider = new TempDiskProviderImpl();
	}

	@Test
	public void testGetTempDirectoryName() {
		// call under test
		assertEquals(System.getProperty("java.io.tmpdir"), provider.getTempDirectoryName());
	}

	@Test
	public void testGetDiskSpaceUsedPercent() {
		// call under test
		double percent = provider.getDiskSpaceUsedPercent();
		assertTrue(percent >= 0.0);
		assertTrue(percent <= 1.0);
	}

	@Test
	public void testGetMachineId() {
		// call under test
		assertEquals(VirtualMachineIdProvider.getVMID(), provider.getMachineId());
	}

	@Test
	public void testListTempFiles() throws IOException {
		File temp = File.createTempFile("TempDiskProviderImplTest", ".txt");
		try {
			FileUtils.writeStringToFile(temp, "12345", StandardCharsets.UTF_8);
			// call under test
			List<FileInfo> result = provider.listTempFiles();
			assertTrue(result.contains(new FileInfo(5, temp.getName())));
		} finally {
			temp.delete();
		}
}}
