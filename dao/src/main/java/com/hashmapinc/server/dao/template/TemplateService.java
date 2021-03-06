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

import com.hashmapinc.server.common.data.id.TemplateId;
import com.hashmapinc.server.common.data.page.PaginatedResult;
import com.hashmapinc.server.common.data.page.TextPageData;
import com.hashmapinc.server.common.data.page.TextPageLink;
import com.hashmapinc.server.common.data.template.TemplateMetadata;
import com.hashmapinc.server.dao.exception.DataValidationException;
import com.hashmapinc.server.dao.service.DataValidator;
import com.hashmapinc.server.dao.sql.template.JpaBaseTemplateDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class TemplateService {

    @Autowired
    private JpaBaseTemplateDao dao;

    public TemplateMetadata getTemplate(TemplateId id) {
        return dao.findById(id.getId());
    }

    public TextPageData<TemplateMetadata> getTemplate(TextPageLink pageLink) {
        return new TextPageData<>(dao.findByPageLink(pageLink), pageLink);
    }

    public List<TemplateMetadata> getAllTemplates() {
        return dao.find();
    }

    public TemplateMetadata save(TemplateMetadata templateMetadata) {
        templateValidator.validate(templateMetadata);
        return dao.save(templateMetadata);
    }

    public void delete(TemplateId id) {
        dao.removeById(id.getId());
    }


    public PaginatedResult<TemplateMetadata> getTemplatesByPage(int limit, int pageNum, String textSearch) {
        return dao.findByPageNumber(limit, pageNum, textSearch);
    }

    private DataValidator<TemplateMetadata> templateValidator =
            new DataValidator<TemplateMetadata>() {
                @Override
                protected void validateDataImpl(TemplateMetadata template) {
                    if (StringUtils.isEmpty(template.getName())) {
                        throw new DataValidationException("Template name should be specified!.");
                    }
                }

                @Override
                protected void validateCreate(TemplateMetadata template) {
                    boolean duplicateNamePresent = dao.findAll().stream()
                            .map(TemplateMetadata::getName)
                            .anyMatch(t -> Objects.equals(t, template.getName()));
                    if(duplicateNamePresent) {
                        throw new DataValidationException("Template is already created for name " + template.getName());
                    }
                }
            };
}
