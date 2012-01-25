package org.sagebionetworks.tool.migration;

import static org.junit.Assert.*;

import org.junit.Test;

public class MigrateLocationsTest {

	/**
	 * 
	 */
	@Test
	public void testExtractPath(){
		String bucket = "proddata.sagebase.org";
		String fullPath = "https://s3.amazonaws.com/proddata.sagebase.org/4495/0.0.0/mskcc_prostate_cancer.zip?Expires=1327102626&x-amz-security-token=AQoDYXdzELH%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEasAIzlYAjWsw7l7JFCIUG%2FnSM%2BAjb9WR0n6yL%2Ba02PnEfzgs7ec2WgL3oNYiwS%2Fm94l9htxyIXzy7y2B11%2BFsmldg%2BC4IkBhxYozSuMonitncHZB%2BIRtIJ3H6mOOaR4jvSmdz13l0aS5o9zjA%2Fudm4KL0XzQsrniQfMXeRP%2FI1WA8DNAuDznW4ibsMFvUwI3wbZcG3lxjkYnSF6yaT7syOeTrp%2BMzb34YMmiqJbBilMh3v3wQNAO3jroVX04rwQFUf9GkzDpXaaTxMkBmcN%2FmXVZTwXIcHFnRptygGyPozX3%2B%2BLhVwKkamroJk%2FN6qkWnshjMSgRHgGjFIQJBmyilEAy405banecDWqthKlturbzJJoTDzH%2BWJRQjQxsX%2BwpGIODFSdhgoQ6CBbJIpMndaKCLIKLS4vgE&AWSAccessKeyId=ASIAI3LHDIXFYV4SN5UQ&Signature=lWjfdr3gEWjrAze5i6nqFU9USFw%3D";
		String extracted = MigrateLocations.extractPath(fullPath, bucket);
		assertNotNull(extracted);
		assertEquals("4495/0.0.0/mskcc_prostate_cancer.zip", extracted);
	}
	
	@Test
	public void calcualteNewPath(){
		String bucket = "proddata.sagebase.org";
		String fullPath = "https://s3.amazonaws.com/proddata.sagebase.org/4495/0.0.0/mskcc_prostate_cancer.zip?Expires=1327102626&x-amz-security-token=AQoDYXdzELH%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEasAIzlYAjWsw7l7JFCIUG%2FnSM%2BAjb9WR0n6yL%2Ba02PnEfzgs7ec2WgL3oNYiwS%2Fm94l9htxyIXzy7y2B11%2BFsmldg%2BC4IkBhxYozSuMonitncHZB%2BIRtIJ3H6mOOaR4jvSmdz13l0aS5o9zjA%2Fudm4KL0XzQsrniQfMXeRP%2FI1WA8DNAuDznW4ibsMFvUwI3wbZcG3lxjkYnSF6yaT7syOeTrp%2BMzb34YMmiqJbBilMh3v3wQNAO3jroVX04rwQFUf9GkzDpXaaTxMkBmcN%2FmXVZTwXIcHFnRptygGyPozX3%2B%2BLhVwKkamroJk%2FN6qkWnshjMSgRHgGjFIQJBmyilEAy405banecDWqthKlturbzJJoTDzH%2BWJRQjQxsX%2BwpGIODFSdhgoQ6CBbJIpMndaKCLIKLS4vgE&AWSAccessKeyId=ASIAI3LHDIXFYV4SN5UQ&Signature=lWjfdr3gEWjrAze5i6nqFU9USFw%3D";
		String extracted = MigrateLocations.extractPath(fullPath, bucket);
		assertNotNull(extracted);
		assertEquals("4495/0.0.0/mskcc_prostate_cancer.zip", extracted);
		
		String newPath = MigrateLocations.calcualteNewPath("4495", "12345", extracted);
		assertEquals("12345/4495/mskcc_prostate_cancer.zip", newPath);
		

	}
	
	@Test
	public void calcualteNewPath2(){
		String bucket = "proddata.sagebase.org";
		String fullPath = "https://s3.amazonaws.com/proddata.sagebase.org/4495/0.0.0/mskcc_prostate_cancer.zip?Expires=1327102626&x-amz-security-token=AQoDYXdzELH%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEasAIzlYAjWsw7l7JFCIUG%2FnSM%2BAjb9WR0n6yL%2Ba02PnEfzgs7ec2WgL3oNYiwS%2Fm94l9htxyIXzy7y2B11%2BFsmldg%2BC4IkBhxYozSuMonitncHZB%2BIRtIJ3H6mOOaR4jvSmdz13l0aS5o9zjA%2Fudm4KL0XzQsrniQfMXeRP%2FI1WA8DNAuDznW4ibsMFvUwI3wbZcG3lxjkYnSF6yaT7syOeTrp%2BMzb34YMmiqJbBilMh3v3wQNAO3jroVX04rwQFUf9GkzDpXaaTxMkBmcN%2FmXVZTwXIcHFnRptygGyPozX3%2B%2BLhVwKkamroJk%2FN6qkWnshjMSgRHgGjFIQJBmyilEAy405banecDWqthKlturbzJJoTDzH%2BWJRQjQxsX%2BwpGIODFSdhgoQ6CBbJIpMndaKCLIKLS4vgE&AWSAccessKeyId=ASIAI3LHDIXFYV4SN5UQ&Signature=lWjfdr3gEWjrAze5i6nqFU9USFw%3D";
		String extracted = MigrateLocations.extractPath(fullPath, bucket);
		assertNotNull(extracted);
		assertEquals("4495/0.0.0/mskcc_prostate_cancer.zip", extracted);
		
		String newPath = MigrateLocations.calcualteNewPath("4621", "4620", "4621/0.0.0/curatedPhenotype.zip");
		assertEquals("4620/4621/curatedPhenotype.zip", newPath);
		

	}
}
