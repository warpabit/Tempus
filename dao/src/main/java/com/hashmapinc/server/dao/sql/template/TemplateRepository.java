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
package com.hashmapinc.server.dao.sql.template;

import com.hashmapinc.server.dao.model.sql.TemplateMetadataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends CrudRepository<TemplateMetadataEntity, String> {

    List<TemplateMetadataEntity> findByName(String name);

    @Query("SELECT t FROM TemplateMetadataEntity t WHERE LOWER(t.name) LIKE LOWER(CONCAT(:searchText, '%')) AND t.id > :idOffset ORDER BY t.id")
    List<TemplateMetadataEntity> findTemplate(@Param("searchText") String searchText,
                                              @Param("idOffset") String idOffset,
                                              Pageable pageable);

    @Query(value = "SELECT t from TemplateMetadataEntity t WHERE LOWER(t.name) LIKE LOWER(CONCAT(:searchText, '%')) ORDER BY t.id",
            countQuery = "SELECT count(*) FROM TemplateMetadataEntity")
    Page<TemplateMetadataEntity> findAll(@Param("searchText") String searchText, Pageable pageable);
}
