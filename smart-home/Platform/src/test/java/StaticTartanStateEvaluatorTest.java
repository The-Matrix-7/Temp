import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import tartan.smarthome.resources.StaticTartanStateEvaluator;
import tartan.smarthome.resources.iotcontroller.IoTValues;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class to check if Tartan Smart Home System appropriately responds to state change requests.
 */
public class StaticTartanStateEvaluatorTest {
    private StaticTartanStateEvaluator evaluator;

    public StaticTartanStateEvaluatorTest() {
        this.evaluator = new StaticTartanStateEvaluator();
    }

    /**
     * Creates a test state that will serve as input to each unit test.
     * @return   the test state
     */
    public Map<String,Object> testState() {
        Map<String,Object> initialState = new HashMap<String,Object>();
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
        // TODO: implement unit test
        StringBuffer log = new StringBuffer();
        Map<String, Object> initialState = testState();
    
        // Given
        // Set the house to vacant
        initialState.put(IoTValues.PROXIMITY_STATE, false);
        // Light is initially turned off
        initialState.put(IoTValues.LIGHT_STATE, false);
        // When
        Map<String, Object> newState = evaluator.evaluateState(initialState);
        boolean lightStateAfterEvaluation = (boolean) newState.get(IoTValues.LIGHT_STATE);
        // Then
        Assert.assertFalse("When the house is vacant, the light should not be turned on", lightStateAfterEvaluation);
    }
    

    @Test
    /**
     */
    public void test2() {
        //TODO: implement unit test  
    }

    @Test
    /**
     */
    public void test3() {
        //TODO: implement unit test  
    }
    
    @Test
    /**
     */
    public void test4() {
        //TODO: implement unit test  
    }
}

