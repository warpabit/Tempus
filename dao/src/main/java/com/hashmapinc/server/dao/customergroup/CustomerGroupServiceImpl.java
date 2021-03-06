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
package com.hashmapinc.server.dao.customergroup;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hashmapinc.server.common.data.*;
import com.hashmapinc.server.common.data.asset.Asset;
import com.hashmapinc.server.common.data.id.*;
import com.hashmapinc.server.common.data.page.TextPageData;
import com.hashmapinc.server.common.data.page.TextPageLink;
import com.hashmapinc.server.dao.asset.AssetService;
import com.hashmapinc.server.dao.customer.CustomerDao;
import com.hashmapinc.server.dao.datamodel.DataModelObjectService;
import com.hashmapinc.server.dao.device.DeviceService;
import com.hashmapinc.server.dao.entity.AbstractEntityService;
import com.hashmapinc.server.dao.exception.DataValidationException;
import com.hashmapinc.server.dao.exception.IncorrectParameterException;
import com.hashmapinc.server.dao.model.ModelConstants;
import com.hashmapinc.server.dao.service.DataValidator;
import com.hashmapinc.server.dao.service.PaginatedRemover;
import com.hashmapinc.server.dao.service.Validator;
import com.hashmapinc.server.dao.tenant.TenantDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.hashmapinc.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class CustomerGroupServiceImpl extends AbstractEntityService implements CustomerGroupService {

    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_CUSTOMER_GROUP_ID = "Incorrect customerGroupId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_USER_ID = "Incorrect userId";

    @Autowired
    protected CustomerGroupDao customerGroupDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private DataModelObjectService dataModelObjectService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;


    private List<UserId> findUserIdsByCustomerGroupId(CustomerGroupId customerGroupId){
        return customerGroupDao.findUserIdsByCustomerGroupId(customerGroupId.getId());
    }


    @Override
    public CustomerGroup findByCustomerGroupId(CustomerGroupId customerGroupId) {
        log.trace("Executing findByCustomerGroupId [{}]", customerGroupId);
        Validator.validateId(customerGroupId, INCORRECT_CUSTOMER_ID + customerGroupId);
        CustomerGroup customerGroup = customerGroupDao.findById(customerGroupId.getId());
        if (customerGroup != null) {
            customerGroup.setUserIds(findUserIdsByCustomerGroupId(customerGroupId));
        }
        return customerGroup;
    }

    @Override
    public TextPageData<CustomerGroup> findByUserId(UserId userId, TextPageLink pageLink) {
        log.trace("Executing findByUserId userId: [{}]", userId);
        Validator.validateId(userId, INCORRECT_USER_ID + userId);
        List<CustomerGroup> customerGroups = customerGroupDao.findByUserId(userId.getId(), pageLink);
        return new TextPageData<>(customerGroups, pageLink);
    }


    @Override
    public ListenableFuture<CustomerGroup> findCustomerGroupByIdAsync(CustomerGroupId customerGroupId) {
        log.trace("Executing findCustomerGroupByIdAsync [{}]", customerGroupId);
        validateId(customerGroupId, INCORRECT_CUSTOMER_ID + customerGroupId);
        ListenableFuture<CustomerGroup> customerGroupDaoByIdAsync = customerGroupDao.findByIdAsync(customerGroupId.getId());

        return Futures.transform(customerGroupDaoByIdAsync, (Function<? super CustomerGroup, ? extends CustomerGroup>) customerGroup -> {
            if (customerGroup != null) {
                customerGroup.setUserIds(findUserIdsByCustomerGroupId(customerGroupId));
            }
            return customerGroup;
        });

    }

    @Override
    public Optional<CustomerGroup> findCustomerByTenantIdAndCustomerIdAndTitle(TenantId tenantId , CustomerId customerId , String title) {
        log.trace("Executing findCustomerByTenantIdAndCustomerIdAndTitle [{}] [{}] [{}]", tenantId, customerId, title);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Optional<CustomerGroup> optionalCustomerGroup = customerGroupDao.findCustomerGroupsByTenantIdAndCustomerIdAndTitle(tenantId.getId(), customerId.getId(), title);
        return optionalCustomerGroup.map(customerGroup -> {
            customerGroup.setUserIds(findUserIdsByCustomerGroupId(customerGroup.getId()));
            return customerGroup;
        });
    }

    @Override
    public CustomerGroup saveCustomerGroup(CustomerGroup customerGroup) {
        log.trace("Executing saveCustomerGroup [{}]", customerGroup);
        customerGroupValidator.validate(customerGroup);
        List<String> policies = customerGroup.getPolicies();
        if(policies != null) {
            customerGroup.setPolicies(policies.stream().distinct().collect(Collectors.toList()));
        }
        CustomerGroup savedCustomerGroup = customerGroupDao.save(customerGroup);
        savedCustomerGroup.setUserIds(findUserIdsByCustomerGroupId(savedCustomerGroup.getId()));
        return savedCustomerGroup;
    }

    @Override
    public void deleteCustomerGroup(CustomerGroupId customerGroupId) {
        log.trace("Executing deleteCustomerGroup [{}]", customerGroupId);
        Validator.validateId(customerGroupId, INCORRECT_CUSTOMER_GROUP_ID + customerGroupId);
        CustomerGroup customerGroup = findByCustomerGroupId(customerGroupId);
        if (customerGroup == null) {
            throw new IncorrectParameterException("Unable to delete non-existent customer group.");
        }
        deleteEntityRelations(customerGroupId);
        customerGroupDao.removeById(customerGroupId.getId());
        customerGroupDao.deleteUserIdsForCustomerGroupId(customerGroupId.getId());
    }

    @Override
    public TextPageData<CustomerGroup> findCustomerGroupsByTenantIdAndCustomerId(TenantId tenantId , CustomerId customerId , TextPageLink pageLink) {
        log.trace("Executing findCustomerGroupsByTenantIdAndCustomerId, tenantId [{}], coustomerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<CustomerGroup> customerGroups = customerGroupDao.findCustomerGroupsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        List<CustomerGroup> customerGroupsWithUserIds =
                customerGroups.stream()
                        .peek(customerGroup -> customerGroup.setUserIds(findUserIdsByCustomerGroupId(customerGroup.getId())))
                        .collect(Collectors.toList());

        return new TextPageData<>(customerGroupsWithUserIds, pageLink);
    }

    @Override
    public void deleteCustomerGroupsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomerGroupsByCustomerId, tenantId [{}] and customerId [{}]", tenantId, customerId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        new CustomerGroupRemover(tenantId).removeEntities(customerId);
    }

    @Override
    public CustomerGroup assignUsers(CustomerGroupId customerGroupId, List<UserId> userIds) {
        log.trace("Executing assignUsers, CustomerGroupId [{}] and userIds [{}]", customerGroupId, userIds);
        Validator.validateId(customerGroupId, INCORRECT_CUSTOMER_GROUP_ID + customerGroupId);
        customerGroupDao.assignUsers(customerGroupId, userIds);
        CustomerGroup customerGroup = customerGroupDao.findById(customerGroupId.getId());
        customerGroup.setUserIds(userIds);
        return customerGroup;
    }

    @Override
    public CustomerGroup unassignUsers(CustomerGroupId customerGroupId , List<UserId> userIds) {
        log.trace("Executing unassignUsers, CustomerGroupId [{}] and userIds [{}]", customerGroupId, userIds);
        Validator.validateId(customerGroupId, INCORRECT_CUSTOMER_GROUP_ID + customerGroupId);
        customerGroupDao.unassignUsers(customerGroupId, userIds);
        return customerGroupDao.findById(customerGroupId.getId());
    }

    @Override
    public List<String> findGroupPoliciesForUser(UserId userId) {
        log.trace("Executing findGroupPoliciesForUser, userId [{}]", userId);
        Validator.validateId(userId, INCORRECT_USER_ID + userId);
        List<CustomerGroup> customerGroups = customerGroupDao.findByUserId(userId.getId(), new TextPageLink(Integer.MAX_VALUE));
        return customerGroups.stream().flatMap(customerGroup -> customerGroup.getPolicies().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public Map<String, Map<String, String>> findGroupPolicies(CustomerGroupId customerGroupId) {
        log.trace("Executing findGroupPolicies, customerGroupId [{}]", customerGroupId);
        Validator.validateId(customerGroupId, INCORRECT_CUSTOMER_GROUP_ID + customerGroupId);
        CustomerGroup customerGroup = customerGroupDao.findById(customerGroupId.getId());
        List<String> policies = customerGroup.getPolicies();
        if(policies != null) {
            List<UserPermission> userPermissions = policies.stream().map(UserPermission::new).collect(Collectors.toList());
            return getDisplayablePermissions(userPermissions);
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<String, Map<String, String>> getDisplayablePermissions(List<UserPermission> userPermissions) {
        Map<String, Map<String, String> > displayablePermissions = new HashMap<>();
        for (UserPermission userPermission : userPermissions) {
            Map<String, String> attrs = new HashMap<>();
            boolean isAccessedToSingleResource = !userPermission.getResources().isEmpty() && userPermission.getResources().size() == 1;
            boolean isOneOfTheAttributeEntitiesDeleted = false;
            if (isAccessedToSingleResource) {
                EntityType entityType = (EntityType) userPermission.getResources().toArray()[0];

                Map<UserPermission.ResourceAttribute, String> resourceAttribute = userPermission.getResourceAttributes();
                for (Map.Entry<UserPermission.ResourceAttribute, String> resAttrEntry : resourceAttribute.entrySet()) {
                    UserPermission.ResourceAttribute attrName = resAttrEntry.getKey();
                    String attrId = resAttrEntry.getValue();
                    String nameOfEntity = getNameOfEntity(entityType, attrName, attrId);
                    if (!nameOfEntity.isEmpty()) {
                        attrs.put(attrName.toString(), nameOfEntity);
                    }
                    else {
                        isOneOfTheAttributeEntitiesDeleted = true;
                        break;
                    }
                }
            }
            if (!isOneOfTheAttributeEntitiesDeleted)
                displayablePermissions.put(userPermission.getPermissionExpr(), attrs);
        }
        return displayablePermissions;
    }

    private String getNameOfEntity(EntityType entityType, UserPermission.ResourceAttribute attrName, String attrId){
        switch (attrName){
            case DATA_MODEL_ID: return dataModelObjectService.findById(new DataModelObjectId(UUID.fromString(attrId))).getName();
            case ID: return getNameOfEntityByResource(entityType, attrId);
        }
        return "";
    }

    private String getNameOfEntityByResource(EntityType entityType, String attrId) {
        if(entityType.equals(EntityType.ASSET)){
            Asset assetById = assetService.findAssetById(new AssetId(UUID.fromString(attrId)));
            return (assetById == null) ? "" : assetById.getName();
        } else if(entityType.equals(EntityType.DEVICE)){
            Device deviceById = deviceService.findDeviceById(new DeviceId(UUID.fromString(attrId)));
            return (deviceById == null) ? "" : deviceById.getName();
        } else {
            return "";
        }
    }

    private class CustomerGroupRemover extends PaginatedRemover<CustomerId, CustomerGroup> {

        private TenantId tenantId;

        CustomerGroupRemover(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        protected List<CustomerGroup> findEntities(CustomerId customerId, TextPageLink pageLink) {
            return customerGroupDao.findCustomerGroupsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(CustomerGroup entity) {
            deleteCustomerGroup(new CustomerGroupId(entity.getUuidId()));
        }

    }

    private DataValidator<CustomerGroup> customerGroupValidator =
            new DataValidator<CustomerGroup>() {

                @Override
                protected void validateCreate(CustomerGroup customerGroup) {
                    customerGroupDao
                            .findCustomerGroupsByTenantIdAndCustomerIdAndTitle(
                                customerGroup.getTenantId().getId(), customerGroup.getCustomerId().getId(), customerGroup.getTitle()
                            )
                            .ifPresent(c -> {
                                    throw new DataValidationException("Customer Group with such title already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(CustomerGroup customerGroup) {
                    customerGroupDao
                            .findCustomerGroupsByTenantIdAndCustomerIdAndTitle(
                                    customerGroup.getTenantId().getId(), customerGroup.getCustomerId().getId(), customerGroup.getTitle()
                            )
                            .ifPresent(c -> {
                                if (!c.getId().equals(customerGroup.getId())) {
                                    throw new DataValidationException("Customer Group with such title already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(CustomerGroup customerGroup) {
                    if (StringUtils.isEmpty(customerGroup.getTitle())) {
                        throw new DataValidationException("Customer Group title should be specified!");
                    }

                    TenantId tenantId = customerGroup.getTenantId();
                    if (tenantId == null) {
                        tenantId = new TenantId(ModelConstants.NULL_UUID);
                        customerGroup.setTenantId(tenantId);
                    } else {
                        Tenant tenant = tenantDao.findById(customerGroup.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Customer Group is referencing to non-existent tenant!");
                        }
                    }

                    CustomerId customerId = customerGroup.getCustomerId();
                    if (customerId == null) {
                        customerId = new CustomerId(ModelConstants.NULL_UUID);
                        customerGroup.setCustomerId(customerId);
                    } else {
                        Customer customer = customerDao.findById(customerGroup.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Customer Group is referencing to non-existent Customer!");
                        } else if (!customer.getTenantId().getId().equals(customerGroup.getTenantId().getId())) {
                            throw new DataValidationException("Customer Group can't be assigned to customer from different tenant!");
                        }
                    }

                    List<String> policies = customerGroup.getPolicies();
                    if(policies != null) {
                        for (String policyExpression : policies) {
                            try {
                                UserPermission userPermission = new UserPermission(policyExpression); //NOSONAR
                            } catch (IllegalArgumentException e) {
                                throw new DataValidationException(e.getMessage());
                            }
                        }
                    }
                }
            };
}
