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
package com.hashmapinc.server.extensions.core.plugin.telemetry.sub;

import com.hashmapinc.server.common.data.kv.DsKvEntry;

import java.util.*;
import java.util.stream.Collectors;

public class DepthSubscriptionUpdate {

    private int subscriptionId;
    private int errorCode;
    private String errorMsg;
    private Map<String, List<Object>> data;

    public DepthSubscriptionUpdate(int subscriptionId, List<DsKvEntry> data) {
        super();
        this.subscriptionId = subscriptionId;
        this.data = new TreeMap<>();
        for (DsKvEntry dsEntry : data) {
            List<Object> values = this.data.computeIfAbsent(dsEntry.getKey(), k -> new ArrayList<>());
            Object[] value = new Object[3];
            value[0] = dsEntry.getDs();
            value[1] = dsEntry.getValueAsString();
            value[2] = dsEntry.getUnit().orElse("");
            values.add(value);
        }
    }

    public DepthSubscriptionUpdate(int subscriptionId, Map<String, List<Object>> data) {
        super();
        this.subscriptionId = subscriptionId;
        this.data = data;
    }

    public DepthSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode) {
        this(subscriptionId, errorCode, null);
    }

    public DepthSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode, String errorMsg) {
        super();
        this.subscriptionId = subscriptionId;
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorMsg != null ? errorMsg : errorCode.getDefaultMsg();
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public Map<String, List<Object>> getData() {
        return data;
    }

    public Map<String, Double> getLatestValues() {
        if (data == null) {
            return Collections.emptyMap();
        } else {
            return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                List<Object> dataList = e.getValue();
                Object[] latest = (Object[]) dataList.get(dataList.size() - 1);
                return (Double) latest[0];
            }));
        }
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public String toString() {
        return "DepthSubscriptionUpdate [subscriptionId=" + subscriptionId + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", data="
                + data + "]";
    }
}
