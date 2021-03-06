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
package com.hashmapinc.server.extensions.api.plugins.rest;

import com.hashmapinc.server.common.data.id.PluginId;
import com.hashmapinc.server.extensions.api.plugins.PluginApiCallSecurityContext;
import com.hashmapinc.server.extensions.api.plugins.msg.ToPluginActorMsg;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

@SuppressWarnings("rawtypes")
public interface PluginRestMsg extends ToPluginActorMsg {

    RestRequest getRequest();

    DeferredResult<ResponseEntity> getResponseHolder();
    
    PluginApiCallSecurityContext getSecurityCtx();
    
    @Override
    default PluginId getPluginId() {
        return getSecurityCtx().getPluginId();
    }

}
