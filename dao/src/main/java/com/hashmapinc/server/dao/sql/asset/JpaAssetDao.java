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
package com.hashmapinc.server.dao.sql.asset;

import com.google.common.util.concurrent.ListenableFuture;
import com.hashmapinc.server.common.data.EntitySubtype;
import com.hashmapinc.server.common.data.EntityType;
import com.hashmapinc.server.common.data.TempusResourceCriteriaSpec;
import com.hashmapinc.server.common.data.UUIDConverter;
import com.hashmapinc.server.common.data.asset.Asset;
import com.hashmapinc.server.common.data.id.AssetId;
import com.hashmapinc.server.common.data.id.DataModelObjectId;
import com.hashmapinc.server.common.data.id.EntityId;
import com.hashmapinc.server.common.data.id.TenantId;
import com.hashmapinc.server.common.data.page.TextPageLink;
import com.hashmapinc.server.dao.DaoUtil;
import com.hashmapinc.server.dao.asset.AssetDao;
import com.hashmapinc.server.dao.model.ModelConstants;
import com.hashmapinc.server.dao.model.sql.AssetEntity;
import com.hashmapinc.server.dao.querybuilder.ResourceCriteria;
import com.hashmapinc.server.dao.querybuilder.TempusResourcePredicate;
import com.hashmapinc.server.dao.sql.JpaAbstractSearchTextDao;
import com.hashmapinc.server.dao.sql.querydsl.JPATempusResourcePredicateBuilder;
import com.hashmapinc.server.dao.util.SqlDao;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Component
public class JpaAssetDao extends JpaAbstractSearchTextDao<AssetEntity, Asset> implements AssetDao {

    @Autowired
    private AssetRepository assetRepository;

    @Override
    protected Class<AssetEntity> getEntityClass() {
        return AssetEntity.class;
    }

    @Override
    protected CrudRepository<AssetEntity, String> getCrudRepository() {
        return assetRepository;
    }

    @Override
    public List<Asset> findAssetsByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findByTenantId(
                        UUIDConverter.fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndIdIn(UUIDConverter.fromTimeUUID(tenantId), UUIDConverter.fromTimeUUIDs(assetIds))));
    }

    @Override
    public List<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findByTenantIdAndCustomerId(
                        UUIDConverter.fromTimeUUID(tenantId),
                        UUIDConverter.fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndCustomerIdAndIdIn(UUIDConverter.fromTimeUUID(tenantId), UUIDConverter.fromTimeUUID(customerId), UUIDConverter.fromTimeUUIDs(assetIds))));
    }

    @Override
    public Optional<Asset> findAssetsByTenantIdAndName(UUID tenantId, String name) {
        Asset asset = DaoUtil.getData(assetRepository.findByTenantIdAndName(UUIDConverter.fromTimeUUID(tenantId), name));
        return Optional.ofNullable(asset);
    }

    @Override
    public List<Asset> findAssetsByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findByTenantIdAndType(
                        UUIDConverter.fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Asset> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findByTenantIdAndCustomerIdAndType(
                        UUIDConverter.fromTimeUUID(tenantId),
                        UUIDConverter.fromTimeUUID(customerId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantAssetTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantAssetTypesToDto(tenantId, assetRepository.findTenantAssetTypes(UUIDConverter.fromTimeUUID(tenantId))));
    }

    @Override
    public List<Asset> findAssetsByDataModelObjectId(UUID dataModelObjectId) {
        return DaoUtil.convertDataList(assetRepository
                                               .findByDataModelObjectId(
                                                       UUIDConverter.fromTimeUUID(dataModelObjectId)));
    }

    @Override
    public List<Asset> findAll(TempusResourceCriteriaSpec tempusResourceCriteriaSpec, TextPageLink textPageLink) {
        final Optional<BooleanExpression> finalPredicate = getFinalPredicate(tempusResourceCriteriaSpec, textPageLink);
        final PageRequest pageable = PageRequest.of(0, textPageLink.getLimit(), new Sort(new Sort.Order(Sort.Direction.ASC, ModelConstants.ID_PROPERTY)));
        return finalPredicate.isPresent() ? DaoUtil.convertDataList(assetRepository.findAll(finalPredicate.get(), pageable).getContent()) : Collections.emptyList();
    }

    private Optional<BooleanExpression> getFinalPredicate(TempusResourceCriteriaSpec tempusResourceCriteriaSpec, TextPageLink textPageLink) {
        final List<BooleanExpression> predicates = getBasicPredicates(tempusResourceCriteriaSpec, textPageLink);
        final Optional<BooleanExpression> predicatesRight = getPredicatesRelatedWithDataModelObjects(tempusResourceCriteriaSpec);
        return predicates.stream().reduce(BooleanExpression::and).map(expression -> {
            if(predicatesRight.isPresent()){
                return expression.and(predicatesRight.get());
            } else {
                return expression;
            }
        });
    }

    private Optional<BooleanExpression> getPredicatesRelatedWithDataModelObjects(TempusResourceCriteriaSpec tempusResourceCriteriaSpec) {
        final JPATempusResourcePredicateBuilder predicateBuilder1 = new JPATempusResourcePredicateBuilder(getEntityClass());
        final Map<DataModelObjectId, Set<? extends EntityId>> dataModelIdAndEntityIdSpec = tempusResourceCriteriaSpec.getDataModelIdAndEntityIdSpec();
        final Set<DataModelObjectId> dataModelObjectIds = dataModelIdAndEntityIdSpec.entrySet().stream().filter(entrySet -> entrySet.getValue().isEmpty()).map(Map.Entry::getKey).collect(Collectors.toSet());

        dataModelObjectIds.forEach(dataModelObjectId ->
                predicateBuilder1.with(new ResourceCriteria(ModelConstants.ASSET_DATA_MODEL_OBJECT_ID, ResourceCriteria.Operation.EQUALS, UUIDConverter.fromTimeUUID(dataModelObjectId.getId()))));


        Optional<BooleanExpression> finalPredicate = predicateBuilder1.build().stream().map(TempusResourcePredicate::getValue).reduce(BooleanExpression::or);


        for(Map.Entry<DataModelObjectId, Set<? extends EntityId>> dataModelIdAndEntityId : dataModelIdAndEntityIdSpec.entrySet()){
            if(!dataModelIdAndEntityId.getValue().isEmpty()){
                final JPATempusResourcePredicateBuilder tempPredicateBuilder = new JPATempusResourcePredicateBuilder(getEntityClass());
                tempPredicateBuilder.with(new ResourceCriteria(ModelConstants.ASSET_DATA_MODEL_OBJECT_ID, ResourceCriteria.Operation.EQUALS, UUIDConverter.fromTimeUUID(dataModelIdAndEntityId.getKey().getId())));
                Set<? extends EntityId> accessibleTempusResourceIdsOnly = dataModelIdAndEntityId.getValue();
                final String[] entityIds = accessibleTempusResourceIdsOnly.stream().map(id -> UUIDConverter.fromTimeUUID(id.getId())).toArray(String[]::new);
                if(accessibleTempusResourceIdsOnly.size() == 1){
                    tempPredicateBuilder.with(new ResourceCriteria(ModelConstants.ID_PROPERTY, ResourceCriteria.Operation.EQUALS,entityIds[0]));
                } else if (accessibleTempusResourceIdsOnly.size() > 1) {
                    tempPredicateBuilder.with(new ResourceCriteria(ModelConstants.ID_PROPERTY, ResourceCriteria.Operation.CONTAINS, entityIds));
                }
                final Optional<BooleanExpression> predicate = tempPredicateBuilder.build().stream().map(TempusResourcePredicate::getValue).reduce(BooleanExpression::and);
                if(finalPredicate.isPresent()){
                    if(predicate.isPresent()) {
                        finalPredicate = finalPredicate.map(expression -> expression.or(predicate.get()));
                    }
                } else {
                    finalPredicate = predicate;
                }
            }
        }
        return finalPredicate;
    }

    private List<BooleanExpression> getBasicPredicates(TempusResourceCriteriaSpec tempusResourceCriteriaSpec, TextPageLink textPageLink) {

        final String idOffset = textPageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(textPageLink.getIdOffset());

        final JPATempusResourcePredicateBuilder basicPredicateBuilder = new JPATempusResourcePredicateBuilder(getEntityClass())
                .with(new ResourceCriteria(ModelConstants.ID_PROPERTY, ResourceCriteria.Operation.GREATER_THAN, idOffset))
                .with(new ResourceCriteria(ModelConstants.ASSET_TENANT_ID_PROPERTY, ResourceCriteria.Operation.EQUALS, UUIDConverter.fromTimeUUID(tempusResourceCriteriaSpec.getTenantId().getId())));

        if(textPageLink.getTextSearch() != null){
            basicPredicateBuilder.with(new ResourceCriteria(ModelConstants.SEARCH_TEXT_PROPERTY, ResourceCriteria.Operation.LIKE, textPageLink.getTextSearch()));
        }

        tempusResourceCriteriaSpec.getCustomerId().ifPresent(customerId ->
                basicPredicateBuilder.with(new ResourceCriteria(ModelConstants.ASSET_CUSTOMER_ID_PROPERTY, ResourceCriteria.Operation.EQUALS, UUIDConverter.fromTimeUUID(customerId.getId()))));

        tempusResourceCriteriaSpec.getType().ifPresent(type ->
                basicPredicateBuilder.with(new ResourceCriteria(ModelConstants.ASSET_TYPE_PROPERTY, ResourceCriteria.Operation.EQUALS, type)));

        final List<TempusResourcePredicate<BooleanExpression>> tempusResourcePredicates = basicPredicateBuilder.build();
        return tempusResourcePredicates.stream().map(TempusResourcePredicate::getValue).collect(Collectors.toList());
    }


    private List<EntitySubtype> convertTenantAssetTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(new TenantId(tenantId), EntityType.ASSET, type));
            }
        }
        return list;
    }
}
