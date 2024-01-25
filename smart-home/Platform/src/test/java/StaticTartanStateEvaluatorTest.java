import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        initialState.put(IoTValues.DOOR_STATE, true); // door is open
        initialState.put(IoTValues.PROXIMITY_STATE, false); // house is vacant

        Map<String, Object> newState = evaluator.evaluateState(initialState, log);
        boolean newDoorState = (boolean) newState.get(IoTValues.DOOR_STATE);
        assertFalse(newDoorState, "Door should not be open while the house is vacant");
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

        System.out.println("New State: " + newState);
        System.out.println("Log: " + log);

        assertEquals(true, newState.get(IoTValues.ALARM_STATE),
                "Alarm should be not be disabled with incorrect passcode"); // try the wrong code

        initialState.put(IoTValues.ALARM_STATE, true); // alarm is armed
        initialState.put(IoTValues.GIVEN_PASSCODE, "correct"); // give the correct passcode
        initialState.put(IoTValues.PROXIMITY_STATE, true); // house is not empty
        initialState.put(IoTValues.DOOR_STATE, false); // door is closed

        newState = evaluator.evaluateState(initialState, log);

        System.out.println("New State: " + newState);
        System.out.println("Log: " + log);

        assertEquals(false, newState.get(IoTValues.ALARM_STATE), "Alarm should not be armed with correct passcode"); // try
                                                                                                                     // correct
                                                                                                                     // code
    }

}
