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
package com.hashmapinc.server.transport.mqtt.session;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hashmapinc.server.common.data.Device;
import com.hashmapinc.server.common.data.id.SessionId;
import com.hashmapinc.server.common.data.kv.AttributeKvEntry;
import com.hashmapinc.server.common.data.kv.KvEntry;
import com.hashmapinc.server.common.msg.core.AttributesUpdateNotification;
import com.hashmapinc.server.common.msg.core.GetAttributesResponse;
import com.hashmapinc.server.common.msg.core.ResponseMsg;
import com.hashmapinc.server.common.msg.core.ToDeviceRpcRequestMsg;
import com.hashmapinc.server.common.msg.kv.AttributesKVMsg;
import com.hashmapinc.server.common.msg.session.*;
import com.hashmapinc.server.common.msg.session.ex.SessionException;
import com.hashmapinc.server.common.transport.adaptor.JsonConverter;
import com.hashmapinc.server.common.transport.session.DeviceAwareSessionContext;
import com.hashmapinc.server.transport.mqtt.MqttTopics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.*;
import com.hashmapinc.server.transport.mqtt.MqttTransportHandler;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewayDeviceSessionCtx extends DeviceAwareSessionContext {

    private static final Gson GSON = new Gson();
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);
    public static final String DEVICE_PROPERTY = "device";
    public static final String VALUES = "values";

    private GatewaySessionCtx parent;
    private final MqttSessionId sessionId;
    private volatile boolean closed;
    private AtomicInteger msgIdSeq = new AtomicInteger(0);

    public GatewayDeviceSessionCtx(GatewaySessionCtx parent, Device device) {
        super(parent.getProcessor(), parent.getAuthService(), device);
        this.parent = parent;
        this.sessionId = new MqttSessionId();
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    @Override
    public SessionType getSessionType() {
        return SessionType.ASYNC;
    }

    @Override
    public void onMsg(SessionActorToAdaptorMsg sessionMsg) throws SessionException {
        Optional<MqttMessage> message = getToDeviceMsg(sessionMsg);
        message.ifPresent(parent::writeAndFlush);
    }

    @Override
    public void onMsg(ToDeviceMsg msg) throws SessionException {
        Optional<MqttMessage> message = getToDeviceMsg(msg);
        message.ifPresent(parent::writeAndFlush);
    }

    private Optional<MqttMessage> getToDeviceMsg(ToDeviceMsg msg) {
        MqttMessage result = null;
        if (msg.getMsgType() == MsgType.POST_DEVICE_EVENT) {
            EventToDeviceResponseMsg eventRspMsg = (EventToDeviceResponseMsg) msg;
            int requestId = eventRspMsg.getRequestId();
            if (requestId >= 0) {
                result = MqttTransportHandler.createMqttPubAckMsg(requestId);
            }
        }
        return Optional.ofNullable(result);
    }

    private Optional<MqttMessage> getToDeviceMsg(SessionActorToAdaptorMsg sessionMsg) {
        ToDeviceMsg msg = sessionMsg.getMsg();
        switch (msg.getMsgType()) {
            case STATUS_CODE_RESPONSE:
                ResponseMsg<?> responseMsg = (ResponseMsg) msg;
                if (responseMsg.isSuccess()) {
                    MsgType requestMsgType = responseMsg.getRequestMsgType();
                    Integer requestId = responseMsg.getRequestId();
                    if (requestId >= 0 && (requestMsgType == MsgType.POST_ATTRIBUTES_REQUEST || requestMsgType == MsgType.POST_TELEMETRY_REQUEST
                            || requestMsgType == MsgType.POST_TELEMETRY_REQUEST_DEPTH)) {
                       return Optional.of(MqttTransportHandler.createMqttPubAckMsg(requestId));
                    }
                }
                break;
            case GET_ATTRIBUTES_RESPONSE:
                GetAttributesResponse response = (GetAttributesResponse) msg;
                if (response.isSuccess()) {
                    return Optional.of(createMqttPublishMsg(MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, response));
                } else {
                    //TODO: push error handling to the gateway
                }
                break;
            case ATTRIBUTES_UPDATE_NOTIFICATION:
                AttributesUpdateNotification notification = (AttributesUpdateNotification) msg;
                return Optional.of(createMqttPublishMsg(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, notification.getData()));
            case TO_DEVICE_RPC_REQUEST:
                ToDeviceRpcRequestMsg rpcRequest = (ToDeviceRpcRequestMsg) msg;
                return Optional.of(createMqttPublishMsg(MqttTopics.GATEWAY_RPC_TOPIC, rpcRequest));
            default:
                break;
        }
        return Optional.empty();
    }

    @Override
    public void onMsg(SessionCtrlMsg msg) throws SessionException {
        //Do nothing
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public long getTimeout() {
        return 0;
    }

    private MqttMessage createMqttPublishMsg(String topic, GetAttributesResponse response) {
        JsonObject result = new JsonObject();
        result.addProperty("id", response.getRequestId());
        result.addProperty(DEVICE_PROPERTY, device.getName());
        Optional<AttributesKVMsg> responseData = response.getData();
        if (responseData.isPresent()) {
            AttributesKVMsg msg = responseData.get();
            if (msg.getClientAttributes() != null) {
                addValues(result, msg.getClientAttributes());
            }
            if (msg.getSharedAttributes() != null) {
                addValues(result, msg.getSharedAttributes());
            }
        }
        return createMqttPublishMsg(topic, result);
    }

    private void addValues(JsonObject result, List<AttributeKvEntry> kvList) {
        if (kvList.size() == 1) {
            addValueToJson(result, "value", kvList.get(0));
        } else {
            JsonObject values;
            if (result.has(VALUES)) {
                values = result.get(VALUES).getAsJsonObject();
            } else {
                values = new JsonObject();
                result.add(VALUES, values);
            }
            kvList.forEach(value -> addValueToJson(values, value.getKey(), value));
        }
    }

    private void addValueToJson(JsonObject json, String name, KvEntry entry) {
        switch (entry.getDataType()) {
            case BOOLEAN:
                entry.getBooleanValue().ifPresent(aBoolean -> json.addProperty(name, aBoolean));
                break;
            case STRING:
                entry.getStrValue().ifPresent(aString -> json.addProperty(name, aString));
                break;
            case DOUBLE:
                entry.getDoubleValue().ifPresent(aDouble -> json.addProperty(name, aDouble));
                break;
            case LONG:
                entry.getLongValue().ifPresent(aLong -> json.addProperty(name, aLong));
                break;
            case JSON:
                JsonParser parser = new JsonParser();
                JsonObject jsonObject = parser.parse(entry.getJsonValue().toString()).getAsJsonObject();
                json.add(name, jsonObject);
                break;
        }
    }

    private MqttMessage createMqttPublishMsg(String topic, AttributesKVMsg data) {
        JsonObject result = new JsonObject();
        result.addProperty(DEVICE_PROPERTY, device.getName());
        result.add("data", JsonConverter.toJson(data, false));
        return createMqttPublishMsg(topic, result);
    }

    private MqttMessage createMqttPublishMsg(String topic, ToDeviceRpcRequestMsg data) {
        JsonObject result = new JsonObject();
        result.addProperty(DEVICE_PROPERTY, device.getName());
        result.add("data", JsonConverter.toJson(data, true));
        return createMqttPublishMsg(topic, result);
    }

    private MqttPublishMessage createMqttPublishMsg(String topic, JsonElement json) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, msgIdSeq.incrementAndGet());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(GSON.toJson(json).getBytes(UTF8));
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }

}
