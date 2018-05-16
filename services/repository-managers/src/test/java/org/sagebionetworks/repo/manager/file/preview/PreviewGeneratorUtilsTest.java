package org.sagebionetworks.repo.manager.file.preview;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.TempFileProvider;
import org.sagebionetworks.repo.util.ResourceTracker;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class PreviewGeneratorUtilsTest {
	
	@Test
	public void testFindExtension() {
		assertEquals("noextension", PreviewGeneratorUtils.findExtension(""));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension("."));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension(".s"));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension(".sdfsfsd"));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension("s."));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension("sasdas."));

		assertEquals("a", PreviewGeneratorUtils.findExtension("x.a"));
		assertEquals("aa", PreviewGeneratorUtils.findExtension("x.aa"));
		assertEquals("aa", PreviewGeneratorUtils.findExtension("x.bb.aa"));
		assertEquals("aa", PreviewGeneratorUtils.findExtension(".bb.aa"));
	}
}
