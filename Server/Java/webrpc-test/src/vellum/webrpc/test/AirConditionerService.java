/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vellum.webrpc.test;

import vellum.webrpc.WebRPCService;

/**
 * Class that simulates a connected air conditioning unit.
 */
public class AirConditionerService extends WebRPCService {
    private static AirConditionerStatus status = new AirConditionerStatus();

    public AirConditionerStatus getStatus() {
        return status;
    }

    public boolean isOn() {
        return status.isOn();
    }

    public void setOn(boolean on) {
        System.out.println("Setting unit power to " + (on ? "on" : "off"));

        status.setOn(on);
    }

    public int getTemperature() {
        return status.getTemperature();
    }

    public void setTemperature(int temperature) {
        if (temperature < 32 || temperature > 96) {
            throw new IllegalArgumentException("Invalid temperature.");
        }

        System.out.println("Setting unit temperature to " + temperature);

        status.setTemperature(temperature);
    }

    public int getFanSpeed() {
        return status.getFanSpeed();
    }

    public void setFanSpeed(int fanSpeed) {
        if (fanSpeed < 1 || fanSpeed > 3) {
            throw new IllegalArgumentException("Invalid fan speed.");
        }

        System.out.println("Setting unit fan speed to " + fanSpeed);

        status.setFanSpeed(fanSpeed);
    }
}
