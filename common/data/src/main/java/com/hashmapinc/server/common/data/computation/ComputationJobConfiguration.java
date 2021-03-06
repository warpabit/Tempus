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
package com.hashmapinc.server.common.data.computation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
@Data
@EqualsAndHashCode
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = KafkaKubelessTrigger.class, name = "KUBELESS-KAFKA"),
        @JsonSubTypes.Type(value = CronKubelessTrigger.class, name = "KUBELESS-CRON"),
        @JsonSubTypes.Type(value = SparkComputationJob.class, name = "SPARK"),
        @JsonSubTypes.Type(value = KinesisLambdaTrigger.class, name = "LAMBDA-KINESIS")
})
public abstract class ComputationJobConfiguration implements Serializable {

    private static final long serialVersionUID = 6428888846471945035L;

    private String key;

    public ComputationJobConfiguration() {
        super();
    }

    protected void markSuspended(){
        log.warn("Suspend method not implemented for [{}]", this.getClass().getName());
    }
}
