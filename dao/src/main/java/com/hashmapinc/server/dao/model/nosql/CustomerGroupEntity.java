/**
 * Copyright © 2017-2018 Hashmap, Inc
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
import com.fasterxml.jackson.databind.JsonNode;
import com.hashmapinc.server.common.data.CustomerGroup;
import com.hashmapinc.server.common.data.User;
import com.hashmapinc.server.common.data.id.CustomerGroupId;
import com.hashmapinc.server.common.data.id.CustomerId;
import com.hashmapinc.server.common.data.id.TenantId;
import com.hashmapinc.server.dao.model.ModelConstants;
import com.hashmapinc.server.dao.model.SearchTextEntity;
import com.hashmapinc.server.dao.model.type.JsonCodec;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import java.util.List;
import java.util.UUID;

import static com.hashmapinc.server.dao.model.ModelConstants.*;

@NoArgsConstructor
@Data
@Table(name = CUSTOMER_GROUP_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public class CustomerGroupEntity implements SearchTextEntity<CustomerGroup> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = CUSTOMER_GROUP_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = CUSTOMER_GROUP_TITLE_PROPERTY)
    private String title;

    @Column(name = CUSTOMER_GROUP_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @ElementCollection()
    @CollectionTable(name = ModelConstants.CUSTOMER_GROUP_POLICY_COLUMN_FAMILY_NAME, joinColumns = @JoinColumn(name = ModelConstants.CUSTOMER_GROUP_POLICY_ID_PROPERTY))
    @Column(name = ModelConstants.CUSTOMER_GROUP_POLICY_PROPERTY)
    private List<String> policies;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public CustomerGroupEntity(CustomerGroup customerGroup) {
        if (customerGroup.getId() != null) {
            this.setId(customerGroup.getId().getId());
        }
        this.tenantId = customerGroup.getTenantId().getId();
        this.customerId = customerGroup.getCustomerId().getId();
        this.title = customerGroup.getTitle();
        this.policies = customerGroup.getPolicies();
        this.additionalInfo = customerGroup.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public CustomerGroup toData() {
        CustomerGroup customerGroup = new CustomerGroup(new CustomerGroupId(getId()));
        customerGroup.setCreatedTime(UUIDs.unixTimestamp(getId()));
        customerGroup.setTenantId(new TenantId(tenantId));
        customerGroup.setCustomerId(new CustomerId(customerId));
        customerGroup.setTitle(title);
        customerGroup.setAdditionalInfo(additionalInfo);
        customerGroup.setPolicies(policies);
        return customerGroup;
    }
}
