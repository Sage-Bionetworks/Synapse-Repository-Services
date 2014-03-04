package org.sagebionetworks.table.cluster;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

public class InstanceDiscoveryImplTest {
	
	AmazonRDSClient mockRDSClient;

	InstanceDiscoveryImpl discovery;
	
	@Before
	public void before(){
		mockRDSClient = Mockito.mock(AmazonRDSClient.class);
		discovery = new InstanceDiscoveryImpl();
		ReflectionTestUtils.setField(discovery, "awsRDSClient", mockRDSClient);
	}
	
	@Test
	public void testDiscoverAllInstances(){
		// Setup for three contiguous instances.
		List<String> instanceIds = new LinkedList<String>();
		for(int i=0; i< 3; i++){
			String id = InstanceUtils.createDatabaseInstanceIdentifier(i);
			instanceIds.add(id);
			mockFoundInstance(id);
		}
		// set the last not found
		mockNotFoundInstance(InstanceUtils.createDatabaseInstanceIdentifier(3));
		List<DBInstance> results = discovery.discoverAllInstances();
		assertNotNull(results);
		assertEquals(3, results.size());
		assertEquals(instanceIds.get(0), results.get(0).getDBInstanceIdentifier());
		assertEquals(instanceIds.get(1), results.get(1).getDBInstanceIdentifier());
		assertEquals(instanceIds.get(2), results.get(2).getDBInstanceIdentifier());
		
		// Now knock out the middle instances and confirm it still finds other other two
		mockNotFoundInstance(InstanceUtils.createDatabaseInstanceIdentifier(1));
		results = discovery.discoverAllInstances();
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(instanceIds.get(0), results.get(0).getDBInstanceIdentifier());
		assertEquals(instanceIds.get(2), results.get(1).getDBInstanceIdentifier());
	}
	
	/**
	 * Helper to mock the case where an instance is found.
	 * @param identifer
	 * @return
	 */
	private DBInstance mockFoundInstance(String identifer){
		DescribeDBInstancesRequest request = new DescribeDBInstancesRequest().withDBInstanceIdentifier(identifer);
		DescribeDBInstancesResult result = new DescribeDBInstancesResult();
		List<DBInstance> instances = new LinkedList<DBInstance>();
		DBInstance instance = new DBInstance();
		instance.setDBInstanceIdentifier(identifer);
		instances.add(instance);
		result.setDBInstances(instances);
		instance.setDBInstanceStatus(InstanceDiscovery.RDS_STATUS_AVAILABLE);
		when(mockRDSClient.describeDBInstances(request)).thenReturn(result);
		return instance;
	}
	
	/**
	 * Helper to mock the case where an instnaces is not found.
	 * 
	 * @param identifer
	 */
	private void mockNotFoundInstance(String identifer){
		DescribeDBInstancesRequest request = new DescribeDBInstancesRequest().withDBInstanceIdentifier(identifer);
		when(mockRDSClient.describeDBInstances(request)).thenThrow(new DBInstanceNotFoundException(identifer));
	}
}
