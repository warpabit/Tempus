/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * Modifications © 2017-2018 Hashmap, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hashmapinc.server.mqtt.rpc;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hashmapinc.server.actors.plugin.PluginProcessingContext;
import com.hashmapinc.server.common.data.Device;
import com.hashmapinc.server.common.data.page.TextPageData;
import com.hashmapinc.server.common.data.plugin.PluginMetaData;
import com.hashmapinc.server.common.data.security.DeviceCredentials;
import com.hashmapinc.server.controller.AbstractControllerTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
public abstract class AbstractMqttServerSideRpcIntegrationTest extends AbstractControllerTest {

    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final Long TIME_TO_HANDLE_REQUEST = 500L;

    private Long asyncContextTimeoutToUseRpcPlugin;


    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        asyncContextTimeoutToUseRpcPlugin = getAsyncContextTimeoutToUseRpcPlugin();

        loginTenantAdmin();
    }

    @Test
    public void testServerMqttOneWayRpc() throws Exception {
        Device device = new Device();
        device.setName("Test One-Way Server-Side RPC");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        client.subscribe("v1/devices/me/rpc/request/+", 1);
        client.setCallback(new TestMqttCallback(client));

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        log.info("Received Device Id: " + deviceId);
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
    }

    @Test
    public void testServerMqttOneWayRpcDeviceOffline() throws Exception {
        Device device = new Device();
        device.setName("Test One-Way Server-Side RPC Device Offline");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isRequestTimeout(),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerMqttOneWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String nonExistentDeviceId = UUIDs.timeBased().toString();

        String result = doPostAsync("/api/plugins/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(PluginProcessingContext.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    @Test
    public void testServerMqttTwoWayRpc() throws Exception {
        Device device = new Device();
        device.setName("Test Two-Way Server-Side RPC");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        log.info("Created Device");
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
        log.info("Received Access Token");

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        client.subscribe("v1/devices/me/rpc/request/+", 1);
        client.setCallback(new TestMqttCallback(client));

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", result);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceOffline() throws Exception {
        Device device = new Device();
        device.setName("Test Two-Way Server-Side RPC Device Offline");
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isRequestTimeout(),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String nonExistentDeviceId = UUIDs.timeBased().toString();

        String result = doPostAsync("/api/plugins/rpc/twoway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(PluginProcessingContext.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    private Device getSavedDevice(Device device) throws Exception {
        return doPost("/api/device", device, Device.class);
    }

    private DeviceCredentials getDeviceCredentials(Device savedDevice) throws Exception {
        return doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    private Long getAsyncContextTimeoutToUseRpcPlugin() throws Exception {
        TextPageData<PluginMetaData> plugins = doGetTyped("/api/plugin/system?limit=1&textSearch=system rpc plugin",
                new TypeReference<TextPageData<PluginMetaData>>(){});
        Long systemRpcPluginTimeout = plugins.getData().iterator().next().getConfiguration().get("defaultTimeout").asLong();
        return systemRpcPluginTimeout + TIME_TO_HANDLE_REQUEST;
    }

    private static class TestMqttCallback implements MqttCallback {

        private final MqttAsyncClient client;

        TestMqttCallback(MqttAsyncClient client) {
            this.client = client;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            log.info("Message Arrived: " + Arrays.toString(mqttMessage.getPayload()));
            MqttMessage message = new MqttMessage();
            String responseTopic = requestTopic.replace("request", "response");
            message.setPayload("{\"value1\":\"A\", \"value2\":\"B\"}".getBytes("UTF-8"));
            client.publish(responseTopic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
