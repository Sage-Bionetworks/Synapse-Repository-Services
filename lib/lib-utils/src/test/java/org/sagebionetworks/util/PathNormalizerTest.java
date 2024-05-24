package org.sagebionetworks.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.sagebionetworks.util.PathNormalizer.normalizeMethodSignature;

public class PathNormalizerTest {

	/*
	 * Synapse API Tests
	 */
	@Test(expected = IllegalArgumentException.class)
	public void urlWithUnknownPathStart(){
		normalizeMethodSignature("/fake/path/syn12345");
	}
	
	@Test
	public void urlWithoutParameters() {
		assertEquals("/certifiedusertestresponse", normalizeMethodSignature("/repo/v1/certifiedUserTestResponse"));
	}
	
	@Test
	public void urlWithSynId() {
		assertEquals("/entity/#/bundle", normalizeMethodSignature("/repo/v1/entity/syn1571204/bundle"));
	}
	
	@Test
	public void urlWithSynIdNumberOnly() {
		assertEquals("/entity/#/bundle", normalizeMethodSignature("/repo/v1/entity/1571204/bundle"));
	}
	
	@Test
	public void urlWithEvaluationSubmissionId() {
		assertEquals("/evaluation/submission/#/status", normalizeMethodSignature("/repo/v1/evaluation/submission/2813223/status"));
	}

	@Test
	public void urlWithEntityIdAndVersionNumber() {
		assertEquals("/entity/#/version/#/filepreview", normalizeMethodSignature("/repo/v1/entity/syn2785825/version/1/filepreview"));
	}

	@Test
	public void urlWithWiki2AndWikiVersionNumber() {
		assertEquals("/evaluation/#/wiki2/#/#", normalizeMethodSignature("/repo/v1/evaluation/2785825/wiki2/2813234/2"));
	}

	@Test
	public void urlWith4IdFields() {
		assertEquals("/entity/#/table/column/#/row/#/version/#/filepreview", normalizeMethodSignature("/repo/v1/entity/syn3456789/table/column/1/row/12/version/2/filepreview"));
	}

	@Test
	public void urlStartWithFileV1() {
		assertEquals("/createchunkedfileuploadchunkurl", normalizeMethodSignature("/file/v1/createChunkedFileUploadChunkURL"));
	}

	@Test
	public void urlStartWithAuthV1() {
		assertEquals("/session", normalizeMethodSignature("/auth/v1/session"));
	}

	@Test
	public void md5UrlTest() {
		assertEquals("/entity/md5/#", normalizeMethodSignature("/repo/v1/entity/md5/0b3ea097271bd405839fca053688c5aa"));
	}

	@Test
	public void evaluationNameUrlTest() {
		assertEquals("/evaluation/name/#", normalizeMethodSignature("/repo/v1/evaluation/name/challenge0evaluation"));
	}

	@Test
	public void entityAliasUrlTest() {
		assertEquals("/entity/alias/#", normalizeMethodSignature("/repo/v1/entity/alias/auditData"));
	}
	
	@Test
	public void warNameBeforeV1Prefix(){
		assertEquals("/entity", normalizeMethodSignature("/services-repository-develop-snapshot/repo/v1/entity")); 
	}
	
	@Test
	public void warNameCapitalizedBeforeV1Prefix(){
		assertEquals("/entity", normalizeMethodSignature("/services-repository-develop-SNAPSHOT/repo/v1/entity")); 
	}
	@Test
	public void warNameWithNumbers(){
		assertEquals("/entity", normalizeMethodSignature("/services-repository-2016-09-06-2313-ec7939b/repo/v1/entity"));
	}
	
	@Test
	public void multipleV1s(){
		assertEquals("/v1/repo/v1/entity", normalizeMethodSignature("/services-repository-develop-SNAPSHOT/repo/v1/v1/repo/v1/entity")); 
	}
	
	public void moreThanOnePrefixBeforeServiceAndV1(){
		assertEquals("/there/is/random/v1/included/syn12345", normalizeMethodSignature("/prefix/Should-Be/Delet-ed/repo/v1/there/is/random/v1/included/syn12345"));
	}

}
