package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOGroupMembersDAOImplDeadlockTest {
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
    
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
    
    private static final Integer NUMBER_O_MONKEYS = 10;
    private static final Integer NUMBER_O_BARRELS = 5;
    private static final Integer PLAY_TIME = 10; // Seconds
    private static final Integer INTERMEDIATE_CHECK_TIME = 1; // Seconds 
    private List<String> monkeyIds;

    // For the monkeys (read only)
    private static final Integer ATTENTION_DEFICIT = 100; // Milliseconds
    private static final String MONKEY_NAME_SUFFIX = "OhWhatFun@Barrel.O.Monkeys";
    private static final String MONKEY_GROUP_PREFIX = "Barrel of Simians ";
    private List<String> barrelIds;
    private volatile boolean keepPlaying = true;

	@Before
	public void setUp() throws Exception {
        // Create all the monkeys
		monkeyIds = new ArrayList<String>(NUMBER_O_MONKEYS);
        for (int i = 0; i < NUMBER_O_MONKEYS; i++) {
            UserGroup monkey = new UserGroup();
            monkey.setName(i + MONKEY_NAME_SUFFIX);
            monkey.setIsIndividual(true);
            monkeyIds.add(userGroupDAO.create(monkey));
        }
        
        // Create all the barrels
        barrelIds = new ArrayList<String>(NUMBER_O_BARRELS);
        for (int i = 0; i < NUMBER_O_BARRELS; i++) {
            UserGroup barrel = new UserGroup();
            barrel.setName(MONKEY_GROUP_PREFIX + i);
            barrel.setIsIndividual(false);
            barrelIds.add(userGroupDAO.create(barrel));
        }
	}

	@After
	public void tearDown() throws Exception {
        for (int i = 0; i < NUMBER_O_MONKEYS; i++) {
        	userGroupDAO.deletePrincipal(i + MONKEY_NAME_SUFFIX);
        }
        for (int i = 0; i < NUMBER_O_BARRELS; i++) {
            userGroupDAO.deletePrincipal(MONKEY_GROUP_PREFIX + i);
        }
	}
    
    @Test
    public void testForDeadlock() throws Exception {
        // Play time!!!
        SocialMonkey monkeys[] = new SocialMonkey[NUMBER_O_MONKEYS];
        for (int i = 0; i < NUMBER_O_MONKEYS; i++) {
            monkeys[i] = new SocialMonkey(monkeyIds.get(i));
            monkeys[i].start();
        }
        
        // Check up on the playing monkeys a few times
        int totalSleepTime = 0; 
        while (totalSleepTime < PLAY_TIME) {
        	for (int i = 0; i < NUMBER_O_MONKEYS; i++) {
	        	if (monkeys[i].fatality != null) {
	                fail();
	            }
        	}
        	Thread.sleep(INTERMEDIATE_CHECK_TIME * 1000);
        	totalSleepTime += INTERMEDIATE_CHECK_TIME;
        }
        
        // Bed time...
        System.out.println("Ending deadlock test");
        keepPlaying = false;
        int totalRaces = 0;
        for (int i = 0; i < NUMBER_O_MONKEYS; i++) {
            while (monkeys[i].isPlaying) {
                Thread.sleep(ATTENTION_DEFICIT);
            }
            totalRaces += monkeys[i].racesDetected;
            if (monkeys[i].fatality != null) {
                throw new Exception(monkeys[i].fatality);
            }
        }
        
        // No deadlock detected :)
        System.out.println("Monkeys collided " + totalRaces + " times");
    }
    
    private enum MonkeyBehavior {
        JOIN,
        LEAVE, 
        ANNEX, 
        SECEDE
    }
    
    private class SocialMonkey extends Thread {
        
        public volatile boolean isPlaying = true;
        public volatile int racesDetected = 0;
        public volatile Exception fatality;
    
        private String monkeyId;
        private Random rand;
        private MonkeyBehavior action;
        private String barrel;
        private String member;
        
        SocialMonkey(String number) {
            this.monkeyId = number;
            rand = new Random();
        }
        
        @Override
        public void run() {
            try {
                while (keepPlaying) {
                    List<String> memberList = new ArrayList<String>();
                    
                    // Decide what to do
                    action = MonkeyBehavior.values()[rand.nextInt(MonkeyBehavior.values().length)];
                    switch (action) {
                        case JOIN:
                        	barrel = barrelIds.get(rand.nextInt(barrelIds.size()));
                        	member = monkeyId;
                            memberList.add(member);
                            groupMembersDAO.addMembers(barrel, memberList);
                            
                            // Check for the barrel
                            List<UserGroup> funBarrels = groupMembersDAO.getUsersGroups(member);
                            if (!checkListForEntry(funBarrels, barrel)) {
                                racesDetected++;
                            }
                            break;
                            
                        case LEAVE:
                            List<UserGroup> barrels = groupMembersDAO.getUsersGroups(monkeyId);
                            if (barrels.size() > 0) {
	                            barrel = barrels.get(rand.nextInt(barrels.size())).getId();
	                        	member = monkeyId;
	                            memberList.add(member);
	                            groupMembersDAO.removeMembers(barrel, memberList);
	                            
	                            // Check for the barrel
	                            List<UserGroup> boringBarrels = groupMembersDAO.getUsersGroups(member);
	                            if (checkListForEntry(boringBarrels, barrel)) {
	                                racesDetected++;
	                            }
                            }
                            break;
                            
                        case ANNEX:
                            barrel = barrelIds.get(rand.nextInt(barrelIds.size()));
                            member = barrelIds.get(rand.nextInt(barrelIds.size()));
                            memberList.add(member);
                            try {
                                groupMembersDAO.addMembers(barrel, memberList);
                            
                                // Check for the barrel
                                List<UserGroup> members = groupMembersDAO.getUsersGroups(member);
                                if (!checkListForEntry(members, barrel)) {
                                    racesDetected++;
                                }
                            } catch (IllegalArgumentException e) {
                                // Circularity exception ignored :P
                            }
                            break;
                            
                        case SECEDE:
                            barrel = barrelIds.get(rand.nextInt(barrelIds.size()));
                            List<UserGroup> monkeySardines = groupMembersDAO.getMembers(barrel);
                            if (monkeySardines.size() > 0) {
	                            member = monkeySardines.get(rand.nextInt(monkeySardines.size())).getId();
	                            memberList.add(member);
	                            groupMembersDAO.removeMembers(barrel, memberList);
	                            
	                            // Check for the barrel
	                            List<UserGroup> members = groupMembersDAO.getUsersGroups(member);
	                            if (checkListForEntry(members, barrel)) {
	                                racesDetected++;
	                            }
                            }
                            break;
                        default:
                            break;
                    }
                    Thread.sleep(rand.nextInt(ATTENTION_DEFICIT));
                }
                
            // Store exceptions so that the main thread can re-throw it
            } catch (Exception e) {
                if (e.getClass() != DataIntegrityViolationException.class) {
	                fatality = e;
	                
	                // Print out some debug info
	                System.out.println("!!! " + action + ": Barrel - " + barrel + ", Monkey - " + member);
	                String errorMessage = e + "\n";
	                for (StackTraceElement st : e.getStackTrace()) {
	                	if (st.getClassName().contains("sagebionetworks")) {
	                		errorMessage += "    " + st + "\n";
	                	}
	                }
	                System.err.print(errorMessage);
	            	
	            }
            }
            isPlaying = false;
        }
    }

    private boolean checkListForEntry(List<UserGroup> ugs, String entry) {
        boolean detected = false;
        for (UserGroup ug : ugs) {
            if (ug.getId().equals(entry)) {
                detected = true;
                break;
            }
        }
        return detected;
    }
}
