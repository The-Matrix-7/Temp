package tartan.smarthome.resources;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import tartan.smarthome.resources.iotcontroller.IoTValues;

public class StaticTartanStateEvaluator implements TartanStateEvaluator {

    private String formatLogEntry(String entry) {
        Long timeStamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
        return "[" + sdf.format(new Date(timeStamp)) + "]: " + entry + "\n";
    }

    /**
     * Ensure the requested state is permitted. This method checks each state
     * variable to ensure that the house remains in a consistent state.
     *
     * @param state The new state to evaluate
     * @param log   The log of state evaluations
     * @return The evaluated state
     */
    @Override
    public Map<String, Object> evaluateState(Map<String, Object> inState, StringBuffer log) {

        // These are the state variables that reflect the current configuration of the
        // house

        Integer tempReading = null; // the current temperature
        Integer targetTempSetting = null; // the user-desired temperature setting
        Integer humidityReading = null; // the current humidity
        Integer nightLockStart = null; // the night lock start time
        Integer nightLockEnd = null; // the night lock end time
        Boolean doorState = null; // the state of the door (true if open, false if closed)
        Boolean doorLocked = null; // state of door lock (true if locked, false if unlocked)
        Boolean lightState = null; // the state of the light (true if on, false if off)
        Boolean proximityState = null; // the state of the proximity sensor (true of house occupied, false if vacant)
        Boolean intruderDetected = false; // true if sensors detect a potential intruder, false otherwise
        Boolean alarmState = null; // the alarm state (true if enabled, false if disabled)
        Boolean humidifierState = null; // the humidifier state (true if on, false if off)
        Boolean heaterOnState = null; // the heater state (true if on, false if off)
        Boolean chillerOnState = null; // the chiller state (true if on, false if off)
        Boolean alarmActiveState = null; // the alarm active state (true if alarm sounding, false if alarm not sounding)
        Boolean awayTimerState = false; // assume that the away timer did not trigger this evaluation
        Boolean awayTimerAlreadySet = false;
        Boolean ownersPhoneNearby = null;
        String alarmPassCode = null;
        String hvacSetting = null; // the HVAC mode setting, either Heater or Chiller
        String givenPassCode = "";
        String lockPasscode = "";
        String givenLockPasscode = "";


        System.out.println("Evaluating new state statically");

        Set<String> keys = inState.keySet();
        for (String key : keys) {

            if (key.equals(IoTValues.TEMP_READING)) {
                tempReading = (Integer) inState.get(key);
            } else if (key.equals(IoTValues.HUMIDITY_READING)) {
                humidityReading = (Integer) inState.get(key);
            } else if (key.equals(IoTValues.TARGET_TEMP)) {
                targetTempSetting = (Integer) inState.get(key);
            } else if (key.equals(IoTValues.HUMIDIFIER_STATE)) {
                humidifierState = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.DOOR_STATE)) {
                doorState = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.LIGHT_STATE)) {
                lightState = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.PROXIMITY_STATE)) {
                proximityState = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.ALARM_STATE)) {
                alarmState = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.HEATER_STATE)) {
                heaterOnState = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.CHILLER_STATE)) {
                chillerOnState = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.HVAC_MODE)) {
                hvacSetting = (String) inState.get(key);
            } else if (key.equals(IoTValues.ALARM_PASSCODE)) {
                alarmPassCode = (String) inState.get(key);
            } else if (key.equals(IoTValues.GIVEN_PASSCODE)) {
                givenPassCode = (String) inState.get(key);
            } else if (key.equals(IoTValues.AWAY_TIMER)) {
                // This is a hack!
                awayTimerState = (Boolean) inState.getOrDefault(key, false);
            } else if (key.equals(IoTValues.ALARM_ACTIVE)) {
                alarmActiveState = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.DOOR_LOCK_STATE)) {
                doorLocked = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.INTRUDER_DETECTED)) {
                intruderDetected = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.NIGHT_LOCK_START)) {
                nightLockStart = Integer.valueOf(inState.get(key).toString());
            } else if (key.equals(IoTValues.NIGHT_LOCK_END)) {
                nightLockEnd = Integer.valueOf(inState.get(key).toString());
            } else if (key.equals(IoTValues.OWNERS_PHONE_NEARBY)) {
                ownersPhoneNearby = (Boolean) inState.get(key);
            } else if (key.equals(IoTValues.LOCKED_PASSCODE)) {
                lockPasscode = (String) inState.get(key);
            }else if (key.equals(IoTValues.GIVEN_LOCKED_PASSCODE)) {
                givenLockPasscode = (String) inState.get(key);
            }
        }

        if (lightState == true) {
            // The light was activated
            if (!proximityState) {
                log.append(formatLogEntry("Cannot turn on light because user not home"));
                lightState = false;
            } else {
                log.append(formatLogEntry("Light on"));
            }
        } else if (lightState) {
            log.append(formatLogEntry("Light off"));
        }

        // The door is now open
        if (doorState) {
            if (!proximityState && alarmState) {
                // door open and no one home and the alarm is set - sound alarm
                log.append(formatLogEntry("Break in detected: Activating alarm"));
                alarmActiveState = true;
            }
            // House vacant, close the door
            else if (!proximityState) {
                // close the door
                doorState = false;
                log.append(formatLogEntry("Closed door because house vacant"));
            } 
            else {
                log.append(formatLogEntry("Door open"));
            }

            // The door is open the alarm is to be set and somebody is home - this is not
            // allowed so discard the processStateUpdate
        }

        // The door is now closed
        else if (!doorState) {
            // the door is closed - if the house is suddenly occupied this is a break-in
            if (alarmState && proximityState) {
                log.append(formatLogEntry("Break in detected: Activating alarm"));
                alarmActiveState = true;

                // door is locked due to a potential intruder
                doorLocked = true;
                intruderDetected = true; // used to disable keyless and electronic forms of entry
                log.append(formatLogEntry("Potential Intruder Detected - locking door"));

            } else {
                log.append(formatLogEntry("Closed door"));
            }
        }

        // Auto lock the house
        if (awayTimerState == true) {
            lightState = false;
            doorState = false;
            alarmState = true;
            awayTimerState = false;
            doorLocked = true;
        }

        // the user has arrived
        if (proximityState) {
            log.append(formatLogEntry("House is occupied"));
            // if the alarm has been disabled, then turn on the light for the user

            if (!lightState && !alarmState) {
                lightState = true;
                log.append(formatLogEntry("Turning on light"));
            }

        }

        // alarm is currently armed
        if (alarmState) {
            log.append(formatLogEntry("Alarm enabled"));

            // remove this line as it will never run the code inside it: } else if
            // (alarmState) { // attempt to disable alarm

            if (!proximityState) { // house is empty and there is no intruder
                if (intruderDetected) { // intruder was detected previously
                    intruderDetected = false; // all clear
                    log.append(formatLogEntry("All Clear - intruder no longer detected"));
                }
                alarmState = true;

                log.append(formatLogEntry("Cannot disable the alarm, house is empty"));
            }

            // removed if (alarmActiveState) because alarm should not need to be sounding to
            // be attempted to be disarmed
            if ((givenPassCode.length() > 0 && givenPassCode.compareTo(alarmPassCode) != 0)
                    || givenPassCode.length() == 0) {
                log.append(formatLogEntry("Cannot disable alarm, invalid passcode given"));
                alarmState = true;
                alarmActiveState = false;

            } else {
                log.append(formatLogEntry("Correct passcode entered, disabled alarm"));
                alarmState = false;
            }

        }

        if (!alarmState) {
            log.append(formatLogEntry("Alarm disabled"));
        }

        if (!alarmState) { // alarm disabled
            alarmActiveState = false;
        }

        // determine if the alarm should sound. There are two cases
        // 1. the door is opened when no one is home
        // 2. the house is suddenly occupied
        try {
            if ((alarmState && !doorState && proximityState) || (alarmState && doorState && !proximityState)) {
                log.append(formatLogEntry("Activating alarm"));
                alarmActiveState = true;
            }
        } catch (NullPointerException npe) {
            // Not enough information to evaluate alarm
            log.append(formatLogEntry("Warning: Not enough information to evaluate alarm"));
        }

        // Is the heater needed?
        if (tempReading < targetTempSetting) {
            log.append(formatLogEntry("Turning on heater, target temperature = " + targetTempSetting
                    + "F, current temperature = " + tempReading + "F"));
            heaterOnState = true;

            // Heater already on
        } else {
            // Heater not needed
            heaterOnState = false;
        }

        if (tempReading > targetTempSetting) {
            // Is the heater needed?
            if (chillerOnState != null) {
                if (!chillerOnState) {
                    log.append(formatLogEntry("Turning on air conditioner target temperature = " + targetTempSetting
                            + "F, current temperature = " + tempReading + "F"));
                    chillerOnState = true;
                } // AC already on
            }
        }
        // AC not needed
        else {
            chillerOnState = false;
        }

        if (chillerOnState) {
            hvacSetting = "Chiller";
        } else if (heaterOnState) {
            hvacSetting = "Heater";
        }
        // manage the HVAC control

        if (hvacSetting.equals("Heater")) {

            if (chillerOnState == true) {
                log.append(formatLogEntry("Turning off air conditioner"));
            }

            chillerOnState = false; // can't run AC
            humidifierState = false; // can't run dehumidifier with heater
        }

        if (hvacSetting.equals("Chiller")) {

            if (heaterOnState == true) {
                log.append(formatLogEntry("Turning off heater"));
            }

            heaterOnState = false; // can't run heater when the A/C is on
        }

        if (humidifierState && hvacSetting.equals("Chiller")) {
            log.append(formatLogEntry("Enabled Dehumidifier"));
        } else {
            log.append(formatLogEntry("Automatically disabled dehumidifier when running heater"));
            humidifierState = false;
        }

        if (!intruderDetected) {
            if (ownersPhoneNearby) {    // Keyless Entry
                doorLocked = false;
                log.append(formatLogEntry("Door automatically unlocked for owner's arrival"));
            } else { if (doorLocked) {      // electronic operation
                    // Check if a passcode is required for locking
                    if (!lockPasscode.isEmpty()) {
                        // A passcode is required for locking, check if the given passcode matches
                        if (givenLockPasscode.compareTo(lockPasscode) != 0) {
                            // Incorrect passcode, log and keep the door locked
                            log.append(formatLogEntry("Incorrect passcode given for locking the door"));
                            doorLocked = true;
                        } else {
                            // Correct passcode, unlock the door
                            doorLocked = false;
                            log.append(formatLogEntry("Door unlocked successfully with the correct passcode"));
                        }
                    } else {
                        // No passcode required for locking, unlock the door
                        doorLocked = false;
                        log.append(formatLogEntry("Door unlocked successfully"));
                    }
                }
             }
        }

        if(!doorLocked) {
            // get current time
            LocalDateTime date = LocalDateTime.now();
            int seconds = date.toLocalTime().toSecondOfDay();
            if ((nightLockStart < nightLockEnd && (seconds > nightLockStart && seconds < nightLockEnd)) || 
                (nightLockStart > nightLockEnd) && (seconds < nightLockEnd || seconds > nightLockStart)) {
                // we need different conditions because the night lock should be ideally be able to be set
                // to go through days. 
                // first condition is if the end time is on the same day as the start time. if it is, check if current time is inbetween them.
                // second condition is if the end time is on the next day. if it is, check if the current time is before the lock time would
                // end, or if the current time is after it would be set.
                doorLocked = true;
                log.append(formatLogEntry("Closed door due to the Night Lock."));
            }
        }
    

        Map<String, Object> newState = new Hashtable<>();
        newState.put(IoTValues.DOOR_STATE, doorState);
        newState.put(IoTValues.AWAY_TIMER, awayTimerState);
        newState.put(IoTValues.LIGHT_STATE, lightState);
        newState.put(IoTValues.PROXIMITY_STATE, proximityState);
        newState.put(IoTValues.ALARM_STATE, alarmState);
        newState.put(IoTValues.HUMIDIFIER_STATE, humidifierState);
        newState.put(IoTValues.HEATER_STATE, heaterOnState);
        newState.put(IoTValues.CHILLER_STATE, chillerOnState);
        newState.put(IoTValues.ALARM_ACTIVE, alarmActiveState);
        newState.put(IoTValues.HVAC_MODE, hvacSetting);
        newState.put(IoTValues.ALARM_PASSCODE, alarmPassCode);
        newState.put(IoTValues.GIVEN_PASSCODE, givenPassCode);
        newState.put(IoTValues.DOOR_LOCK_STATE, doorLocked);
        newState.put(IoTValues.INTRUDER_DETECTED, intruderDetected);
        newState.put(IoTValues.OWNERS_PHONE_NEARBY, ownersPhoneNearby);
        return newState;
    }
}