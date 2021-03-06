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
package com.hashmapinc.server.service.metadataingestion;

import com.hashmapinc.server.common.data.metadata.MetadataConfigId;
import com.hashmapinc.server.common.data.metadata.MetadataQuery;
import com.hashmapinc.server.common.data.metadata.MetadataQueryId;
import com.hashmapinc.server.common.data.page.TextPageData;
import com.hashmapinc.server.common.data.page.TextPageLink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static com.hashmapinc.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class MetadataQueryServiceImpl implements MetadataQueryService {

    @Value("${metadata-ingestion.query.service_url}")
    private String serviceUrl;

    @Value("${metadata-ingestion.query.config_path}")
    private String configQueryPath;

    private static final String PATH_SEPARATOR = "/";

    @Autowired
    @Qualifier("clientRestTemplate")
    private RestTemplate restTemplate;

    private static final String INCORRECT_CONFIG_ID = "Incorrect metadata config id ";
    private static final String INCORRECT_QUERY_ID = "Incorrect metadata query id ";

    @Override
    public MetadataQuery save(MetadataQuery query) {
        log.trace("Executing MetadataQueryServiceImpl.save [{}]", query);
        if (query.getId() == null) {
            return restTemplate.postForObject(serviceUrl, query, MetadataQuery.class);
        } else {
            restTemplate.put(serviceUrl, query);
            return query;
        }
    }

    @Override
    public MetadataQuery findById(MetadataQueryId id) {
        log.trace("Executing MetadataQueryServiceImpl.findById [{}]", id);
        validateId(id, INCORRECT_QUERY_ID + id);
        String url = serviceUrl + PATH_SEPARATOR + id.getId();
        return restTemplate.getForObject(url, MetadataQuery.class);
    }

    @Override
    public TextPageData<MetadataQuery> findAllByMetadataConfigId(MetadataConfigId metadataConfigId, TextPageLink pageLink) {
        log.trace("Executing MetadataQueryServiceImpl.findAllByMetadataConfigId [{}]", metadataConfigId);
        validateId(metadataConfigId, INCORRECT_CONFIG_ID + metadataConfigId);

        String url = serviceUrl + PATH_SEPARATOR + configQueryPath + PATH_SEPARATOR + metadataConfigId.getId();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("limit", pageLink.getLimit());

        if (pageLink.getIdOffset() != null)
            builder.queryParam("idOffset", pageLink.getIdOffset());

        ResponseEntity<TextPageData<MetadataQuery>> response = restTemplate.exchange(builder.build().encode().toUri(),
                HttpMethod.GET, null, new ParameterizedTypeReference<TextPageData<MetadataQuery>>() {
        });

        return response.getBody();
    }

    @Override
    public void delete(MetadataQueryId id) {
        log.trace("Executing MetadataQueryServiceImpl.delete [{}]", id);
        validateId(id, INCORRECT_QUERY_ID + id);
        String url = serviceUrl + PATH_SEPARATOR + id.getId();
        restTemplate.delete(url);
    }
}