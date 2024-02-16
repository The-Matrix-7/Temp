import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import tartan.smarthome.resources.StaticTartanStateEvaluator;
import tartan.smarthome.resources.iotcontroller.IoTValues;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class to check if Tartan Smart Home System appropriately responds to
 * state change requests.
 */
public class StaticTartanStateEvaluatorTest {
    private StaticTartanStateEvaluator evaluator;

    public StaticTartanStateEvaluatorTest() {
        this.evaluator = new StaticTartanStateEvaluator();
    }

    /**
     * Creates a test state that will serve as input to each unit test.
     * 
     * @return the test state
     */
    public Map<String, Object> testState() {
        Map<String, Object> initialState = new HashMap<String, Object>();
        initialState.put(IoTValues.TEMP_READING, 0);
        initialState.put(IoTValues.HUMIDITY_READING, 0);
        initialState.put(IoTValues.TARGET_TEMP, 0);
        initialState.put(IoTValues.HUMIDIFIER_STATE, false);
        initialState.put(IoTValues.DOOR_STATE, false);
        initialState.put(IoTValues.LIGHT_STATE, false);
        initialState.put(IoTValues.PROXIMITY_STATE, false);
        initialState.put(IoTValues.ALARM_STATE, false);
        initialState.put(IoTValues.HEATER_STATE, false);
        initialState.put(IoTValues.CHILLER_STATE, false);
        initialState.put(IoTValues.HVAC_MODE, "0");
        initialState.put(IoTValues.ALARM_PASSCODE, "0");
        initialState.put(IoTValues.GIVEN_PASSCODE, "0");
        initialState.put(IoTValues.AWAY_TIMER, false);
        initialState.put(IoTValues.ALARM_ACTIVE, false);
        initialState.put(IoTValues.DOOR_LOCK_STATE, false);
        return initialState;
    }

    @Test
    /**
     * When the house is vacant, the light can't be turned on
     */
    public void r1Test() {
        StringBuffer log = new StringBuffer();
        Map<String, Object> initialState = testState();

        // Given
        // Set the house to vacant
        initialState.put(IoTValues.PROXIMITY_STATE, false);
        // Light is initially turned off
        initialState.put(IoTValues.LIGHT_STATE, false);

        // When
        Map<String, Object> newState = evaluator.evaluateState(initialState, log);
        boolean lightStateAfterEvaluation = (boolean) newState.get(IoTValues.LIGHT_STATE);

        // Then
        assertFalse(lightStateAfterEvaluation, "When the house is vacant, the light should not be turned on");
        
        // Optionally, you can print the log for debugging or verification
        //System.out.println("Evaluation Log: " + log.toString());
    }

    

    @Test
    /**
     * Test that the alarm is sounded when it's enabled and the house gets suddenly
     * occupied.
     */
    public void r4Test() {
        Map<String, Object> initialState = testState();
        initialState.put(IoTValues.ALARM_STATE, true);

        initialState.put(IoTValues.PROXIMITY_STATE, true);
        initialState.put(IoTValues.DOOR_STATE, false);
        initialState.put(IoTValues.GIVEN_PASSCODE, "incorrect");

        StringBuffer log = new StringBuffer();
        Map<String, Object> newState = evaluator.evaluateState(initialState, log);

        assertTrue((Boolean) newState.get(IoTValues.ALARM_ACTIVE));
        assertTrue(log.toString().contains("Activating alarm"));
    }

    @Test
    /**
     * Checks if the system closes the door when the house is vacant (R3).
     */
    public void r3Test() {
        StringBuffer log = new StringBuffer();
        Map<String, Object> initialState = testState();

        // initial conditions
        initialState.put(IoTValues.DOOR_STATE, true); // door is open
        initialState.put(IoTValues.PROXIMITY_STATE, true); // house is occupied
        initialState.put(IoTValues.ALARM_STATE, false); // alarm is not armed

        Map<String, Object> newState = evaluator.evaluateState(initialState, log);
        boolean newDoorState = (boolean) newState.get(IoTValues.DOOR_STATE);
        assertTrue(newDoorState, "Door is open if no break in is detected and resident is in their house");


        // resident leaves property
        initialState.put(IoTValues.DOOR_STATE, true); // door is open
        initialState.put(IoTValues.PROXIMITY_STATE, false); // house is vacant
        initialState.put(IoTValues.ALARM_STATE, false); // alarm is not armed


        newState = evaluator.evaluateState(initialState, log);
        newDoorState = (boolean) newState.get(IoTValues.DOOR_STATE);
        assertFalse(newDoorState, "Door should not be open while the house is vacant");

    }

    @Test
    /**
     * Checks if door remains closed regardless of if the alarm is armed or the proximity sensor detects anything 
     */
    public void doorRemainsClosed() {
        StringBuffer log = new StringBuffer();
        Map<String, Object> initialState = testState();

        initialState.put(IoTValues.DOOR_STATE, false); // door is closed
        initialState.put(IoTValues.PROXIMITY_STATE, false); // house is vacant
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        
        Map<String, Object> newState = evaluator.evaluateState(initialState, log);
        boolean newDoorState = (boolean) newState.get(IoTValues.DOOR_STATE);
        assertFalse(newDoorState, "Door should remain closed");

        initialState.put(IoTValues.DOOR_STATE, false); // door is closed
        initialState.put(IoTValues.PROXIMITY_STATE, true); // house is occupied
        initialState.put(IoTValues.ALARM_STATE, false); // alarm is not armed

        newState = evaluator.evaluateState(initialState, log);
        newDoorState = (boolean) newState.get(IoTValues.DOOR_STATE);
        assertFalse(newDoorState, "Door should remain closed");
    }


    @Test
    /**
     * Checks if the system does not change the state of the door when a possible break in is detected
     */
    public void doorStateBreakInDetected() {
        StringBuffer log = new StringBuffer();
        Map<String, Object> initialState = testState();

        initialState.put(IoTValues.DOOR_STATE, false); // door is closed
        initialState.put(IoTValues.PROXIMITY_STATE, true); // house is occupied
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        
        Map<String, Object>newState = evaluator.evaluateState(initialState, log);
        Boolean newDoorState = (boolean) newState.get(IoTValues.DOOR_STATE);
        assertFalse(newDoorState, "Door should remain closed");

        initialState.put(IoTValues.DOOR_STATE, true); // door is open
        initialState.put(IoTValues.PROXIMITY_STATE, false); // house is vacant
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        
        newState = evaluator.evaluateState(initialState, log);
        newDoorState = (boolean) newState.get(IoTValues.DOOR_STATE);
        assertTrue(newDoorState, "Door should remain open");

    }


    @Test
    /**
     * R9 (the correct passcode is required to disable the alarm)
     */
    public void r9Test() {
        Map<String, Object> initialState = testState();
        StringBuffer log = new StringBuffer();
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        initialState.put(IoTValues.ALARM_PASSCODE, "correct"); // Set the passcode
        initialState.put(IoTValues.GIVEN_PASSCODE, "incorrect"); // give the incorrect passcode
        initialState.put(IoTValues.PROXIMITY_STATE, true); // house is not empty
        initialState.put(IoTValues.DOOR_STATE, false); // door is closed

        System.out.println("Initial State: " + initialState);

        Map<String, Object> newState = evaluator.evaluateState(initialState, log);

        //System.out.println("New State: " + newState);
        //System.out.println("Log: " + log);

        assertEquals(true, newState.get(IoTValues.ALARM_STATE),
                "Alarm should be not be disabled with incorrect passcode"); // try the wrong code

        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        initialState.put(IoTValues.GIVEN_PASSCODE, "correct"); // give the correct passcode
        initialState.put(IoTValues.PROXIMITY_STATE, true); // house is not empty
        initialState.put(IoTValues.DOOR_STATE, false); // door is closed

        newState = evaluator.evaluateState(initialState, log);

       // System.out.println("New State: " + newState);
        //System.out.println("Log: " + log);

        assertEquals(false, newState.get(IoTValues.ALARM_STATE), "Alarm should not be armed with correct passcode"); // try correct code

        // test no passcode given
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        initialState.put(IoTValues.GIVEN_PASSCODE, ""); // give no code
        initialState.put(IoTValues.PROXIMITY_STATE, true); // house is not empty
        initialState.put(IoTValues.DOOR_STATE, false); // door is closed

        newState = evaluator.evaluateState(initialState, log);

        //System.out.println("New State: " + newState);
        //System.out.println("Log: " + log);

        assertEquals(true, newState.get(IoTValues.ALARM_STATE), "Alarm should not be armed with no passcode"); // try correct code


    }



    @Test
    /**
     * Intruder Defense: When sensors in the house detect the possible presence of an intruder, 
     * lock the door and send "possible intruder detected" messages to the access panels. 
     * Keep the door locked until the sensors provide an "all clear" signal, 
     * at which time "all clear" messages are sent to the access panels.
     */
    public void intruderDefenseTest() {
        Map<String, Object> initialState = testState();
        StringBuffer log = new StringBuffer();
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        initialState.put(IoTValues.PROXIMITY_STATE, false); // house is empty/no one around
        initialState.put(IoTValues.DOOR_STATE, false); // door is closed
        initialState.put(IoTValues.LIGHT_STATE, false); // lights are off
        initialState.put(IoTValues.DOOR_LOCK_STATE, true); // door is locked
        initialState.put(IoTValues.GIVEN_PASSCODE, ""); // give no passcode

        Map<String, Object> newState = evaluator.evaluateState(initialState, log);

        System.out.println("New State: " + newState);
        System.out.println("Log: " + log);
        assertFalse((boolean) newState.get(IoTValues.DOOR_STATE), "Door should be closed still");
        assertTrue((boolean) newState.get(IoTValues.ALARM_STATE), "Alarm should still be armed"); 
        assertFalse((boolean) newState.get(IoTValues.PROXIMITY_STATE), "There should not be anything flagging proximity sensor");
        assertFalse((boolean) newState.get(IoTValues.LIGHT_STATE), "Lights should still be off");
        assertTrue((boolean) newState.get(IoTValues.DOOR_LOCK_STATE), "Door should be locked still"); 


        // simulate a potential intruder
        initialState.put(IoTValues.PROXIMITY_STATE, true); // potential intruder
        initialState.put(IoTValues.DOOR_LOCK_STATE, true); // door locked
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        // TODO: non-registered user

        newState = evaluator.evaluateState(initialState, log);

        assertFalse((boolean) newState.get(IoTValues.DOOR_STATE), "Door should be closed still"); // Door should be locked when no one around
        assertTrue((boolean) newState.get(IoTValues.DOOR_LOCK_STATE), "Door should be locked still"); // Door should be locked when no one around


        // simulate user that is not an intruder
        initialState.put(IoTValues.PROXIMITY_STATE, true); // potential intruder
        initialState.put(IoTValues.DOOR_LOCK_STATE, true); // door locked
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        // TODO: registered user

        newState = evaluator.evaluateState(initialState, log);

        assertFalse((boolean) newState.get(IoTValues.DOOR_STATE), "Door should be closed still"); // door remains closed
        assertTrue((boolean) newState.get(IoTValues.DOOR_LOCK_STATE), "Door should be locked still"); // door remains locked

    
    }

    @Test
    /**
     * Checks if the appropriate intruder detection and all clear messages have been received by the access panel (log)
     */
    public void accessPanelLogTest() {
        Map<String, Object> initialState = testState();
        StringBuffer log = new StringBuffer();
        
        // initial conditions - no break in occurred
        initialState.put(IoTValues.PROXIMITY_STATE, false); // no one is on the property
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        
        Map<String, Object> newState = evaluator.evaluateState(initialState, log);

        // check that there are no intruder detected or all clear messages displayed on the access panel
        assertEquals(-1, log.lastIndexOf("Potential Intruder Detected - locking door"));  
        assertEquals(-1, log.lastIndexOf("All Clear - intruder no longer detected"));


        // potential intruder detected by sensors
        initialState.put(IoTValues.PROXIMITY_STATE, true); // someone detected on property
        initialState.put(IoTValues.DOOR_LOCK_STATE, true); // door is currently locked
        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        // TODO: non-registered user

        newState = evaluator.evaluateState(initialState, log);

        assertNotEquals(-1, log.lastIndexOf("Potential Intruder Detected - locking door"), "Access panel should display potential intruder message");


        // all clear condition
        initialState.put(IoTValues.INTRUDER_DETECTED, true);  // intruder was previously detected on the property
        initialState.put(IoTValues.PROXIMITY_STATE, false); // potential intruder leaves the property
        
        newState = evaluator.evaluateState(initialState, log);  

        assertNotEquals(-1, log.lastIndexOf("All Clear - intruder no longer detected"), "Access panel should display all clear message");

        System.out.println(log);
    }
}

