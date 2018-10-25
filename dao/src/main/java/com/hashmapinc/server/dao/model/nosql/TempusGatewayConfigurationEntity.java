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
package com.hashmapinc.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.hashmapinc.server.common.data.TempusGatewayConfiguration;
import com.hashmapinc.server.common.data.id.TempusGatewayConfigurationId;
import com.hashmapinc.server.common.data.id.TenantId;
import com.hashmapinc.server.dao.model.BaseEntity;
import com.hashmapinc.server.dao.model.ModelConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

import static com.hashmapinc.server.dao.model.ModelConstants.*;


@NoArgsConstructor
@Data
@Table(name = TEMPUS_GATEWAY_CONFIGURATION_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public class TempusGatewayConfigurationEntity implements BaseEntity<TempusGatewayConfiguration> {
    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = TEMPUS_GATEWAY_CONFIGURATION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.TEMPUS_GATEWAY_CONFIGURATION_REPLICAS_PROPERTY)
    private int replicas;

    @Column(name = ModelConstants.TEMPUS_GATEWAY_CONFIGURATION_GATEWAY_TOKEN_PROPERTY)
    private String gatewayToken;

    public TempusGatewayConfigurationEntity(TempusGatewayConfiguration tempusGatewayConfiguration) {
        if (tempusGatewayConfiguration.getId() != null) {
            this.setId(tempusGatewayConfiguration.getId().getId());
        }
        this.tenantId = tempusGatewayConfiguration.getTenantId().getId();
        this.replicas = tempusGatewayConfiguration.getReplicas();
        this.gatewayToken = tempusGatewayConfiguration.getGatewayToken();
    }

    @Override
    public TempusGatewayConfiguration toData() {
        TempusGatewayConfiguration tempusGatewayConfiguration = new TempusGatewayConfiguration(new TempusGatewayConfigurationId(getId()));
        tempusGatewayConfiguration.setCreatedTime(UUIDs.unixTimestamp(getId()));
        tempusGatewayConfiguration.setTenantId(new TenantId(tenantId));
        tempusGatewayConfiguration.setReplicas(replicas);
        tempusGatewayConfiguration.setGatewayToken(gatewayToken);
        return tempusGatewayConfiguration;
    }
}