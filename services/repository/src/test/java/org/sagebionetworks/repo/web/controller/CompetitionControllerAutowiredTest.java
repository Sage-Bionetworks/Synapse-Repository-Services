package org.sagebionetworks.repo.web.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CompetitionControllerAutowiredTest {
	
	@Autowired
//	CompetitionService
	
	@Before
	public void setUp() {
		
	}
	
	@Test
	public void test1() {
		
	}

}
