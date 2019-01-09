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
package com.hashmapinc.server.controller;

import com.hashmapinc.server.common.data.template.TemplateMetadata;
import com.hashmapinc.server.dao.template.TemplateService;
import com.hashmapinc.server.exception.TempusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class TemplateController extends BaseController {

    @Autowired
    private TemplateService templateService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/templates/save", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TemplateMetadata save(@RequestBody TemplateMetadata source) throws TempusException {
        try {
            return templateService.save(source);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/templates/delete")
    public void delete(@RequestParam String id) throws TempusException {
        try {
            templateService.delete(id);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/templates/get", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TemplateMetadata get(@RequestParam String id) throws TempusException {
        try {
            return templateService.getTemplate(id);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/templates/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<TemplateMetadata> getAll() throws TempusException {
        try {
            return templateService.getAllTemplates();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
