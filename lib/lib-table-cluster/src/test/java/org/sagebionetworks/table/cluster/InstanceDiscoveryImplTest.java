package org.sagebionetworks.table.cluster;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

public class InstanceDiscoveryImplTest {
	
	AmazonRDSClient mockRDSClient;

	@Before
	public void before(){
		mockRDSClient = Mockito.mock(AmazonRDSClient.class);
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
		result.setDBInstances(instances);
		DBInstance instance = new DBInstance();
		instance.setDBInstanceIdentifier(identifer);
		instances.add(instance);
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
