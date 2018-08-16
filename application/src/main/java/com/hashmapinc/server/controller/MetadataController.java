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

import com.hashmapinc.server.common.data.MetadataIngestionEntries;
import com.hashmapinc.server.common.data.id.TenantId;
import com.hashmapinc.server.common.data.metadata.MetadataConfig;
import com.hashmapinc.server.common.data.metadata.MetadataConfigId;
import com.hashmapinc.server.dao.metadataingestion.MetadataConfigService;
import com.hashmapinc.server.exception.TempusException;
import com.hashmapinc.server.service.security.model.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class MetadataController extends BaseController {

    @Autowired
    MetadataConfigService metadataConfigService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/metadata/config")
    @ResponseBody
    public MetadataConfig saveMetadataConfig(MetadataConfig metadataConfig) throws TempusException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            metadataConfig.setOwnerId(tenantId.getId().toString());
            return metadataConfigService.save(metadataConfig);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/metadata/config/{metadataConfigId}")
    @ResponseBody
    public MetadataConfig getMetadataConfigById(@PathVariable("metadataConfigId") String strMetadataConfigId) throws TempusException {
        checkParameter("metadataConfigId", strMetadataConfigId);
        try {
            MetadataConfigId metadataConfigId = new MetadataConfigId(toUUID(strMetadataConfigId));
            return metadataConfigService.findById(metadataConfigId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/metadataconfigs")
    @ResponseBody
    public List<MetadataConfig> getTenantMetadataConfigs() throws TempusException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return metadataConfigService.findByTenant(tenantId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/metadataconfig/{metadataConfigId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteMetaDataConfig(@PathVariable("metadataConfigId") String strMetadataConfigId) throws TempusException {
        checkParameter("metadataConfigId", strMetadataConfigId);
        try {
            MetadataConfigId metadataConfigId = new MetadataConfigId(toUUID(strMetadataConfigId));
            metadataConfigService.delete(metadataConfigId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/metadataconfig/test/{metadataConfigId}")
    @ResponseStatus(value = HttpStatus.OK)
    public Boolean testSource(@PathVariable("metadataConfigId") String strMetadataConfigId) throws TempusException {
        checkParameter("metadataConfigId", strMetadataConfigId);
        try {
            MetadataConfigId metadataConfigId = new MetadataConfigId(toUUID(strMetadataConfigId));
            return metadataConfigService.testSource(metadataConfigId);
        } catch (Exception e) {
            throw handleException(e);
        }

    }

    @PreAuthorize("#oauth2.isClient() and #oauth2.hasScope('server')")
    @PostMapping(value = "/metadataconfig/insert")
    @ResponseBody
    public void insert(MetadataIngestionEntries metadataIngestionEntries) throws TempusException {
        SecurityUser securityUser = getCurrentUser();
        try {
            metadataIngestionService.save(
                    metadataIngestionEntries.getTenantId(),
                    metadataIngestionEntries.getMetadataConfigId(),
                    metadataIngestionEntries.getMetadataSourceName(),
                    metadataIngestionEntries.getMetaDataKvEntries());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}