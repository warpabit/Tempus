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
package com.hashmapinc.server.dao.template;

import com.datastax.driver.core.utils.UUIDs;
import com.hashmapinc.server.common.data.template.TemplateMetadata;
import com.hashmapinc.server.dao.model.sql.TemplateMetadataEntity;
import com.hashmapinc.server.dao.sql.template.TemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    public TemplateMetadata getTemplate(String id) {
        return templateRepository.findById(id)
                .map(TemplateMetadataEntity::toData)
                .orElse(null);
    }

    public List<TemplateMetadata> getAllTemplates() {
        return StreamSupport.stream(templateRepository.findAll().spliterator(), false)
                .map(TemplateMetadataEntity::toData)
                .collect(Collectors.toList());
    }

    public TemplateMetadata save(TemplateMetadata templateMetadata) {
        TemplateMetadataEntity freshEntity = new TemplateMetadataEntity(templateMetadata);
        if (freshEntity.getId() == null) {
            freshEntity.setId(UUIDs.timeBased());
        }
        return templateRepository.save(freshEntity).toData();
    }

    public void delete(String id) {
        templateRepository.deleteById(id);
    }


}
