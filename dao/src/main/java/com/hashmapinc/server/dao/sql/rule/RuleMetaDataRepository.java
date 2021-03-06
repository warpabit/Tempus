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
package com.hashmapinc.server.dao.sql.rule;

import com.hashmapinc.server.dao.model.sql.RuleMetaDataEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
@Repository
public interface RuleMetaDataRepository extends CrudRepository<RuleMetaDataEntity, String> {

    List<RuleMetaDataEntity> findByPluginToken(String pluginToken);

    @Query("SELECT rmd FROM RuleMetaDataEntity rmd WHERE rmd.tenantId = :tenantId " +
            "AND LOWER(rmd.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND rmd.id > :idOffset ORDER BY rmd.id")
    List<RuleMetaDataEntity> findByTenantIdAndPageLink(@Param("tenantId") String tenantId,
                                                       @Param("textSearch") String textSearch,
                                                       @Param("idOffset") String idOffset,
                                                       Pageable pageable);

    @Query("SELECT rmd FROM RuleMetaDataEntity rmd WHERE rmd.tenantId IN (:tenantId, :nullTenantId) " +
            "AND LOWER(rmd.searchText) LIKE LOWER(CONCAT(:textSearch, '%')) " +
            "AND rmd.id > :idOffset ORDER BY rmd.id")
    List<RuleMetaDataEntity> findAllTenantRulesByTenantId(@Param("tenantId") String tenantId,
                                                          @Param("nullTenantId") String nullTenantId,
                                                          @Param("textSearch") String textSearch,
                                                          @Param("idOffset") String idOffset,
                                                          Pageable pageable);
}
