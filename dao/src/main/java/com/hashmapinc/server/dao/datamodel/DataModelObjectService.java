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
package com.hashmapinc.server.dao.datamodel;

import com.hashmapinc.server.common.data.datamodel.DataModelObject;
import com.hashmapinc.server.common.data.id.DataModelId;
import com.hashmapinc.server.common.data.id.DataModelObjectId;

import java.util.List;
import java.util.Set;

public interface DataModelObjectService {
    DataModelObject save(DataModelObject dataModelObject);
    void removeById(DataModelObjectId dataModelObjectId);
    DataModelObject findById(DataModelObjectId dataModelObjectId);
    List<DataModelObject> findByDataModelId(DataModelId dataModelId);
    Set<DataModelObjectId> getAllParentDataModelIdsOf(DataModelObjectId dataModelObjectId);
    List<DataModelObject> findByName(String name);
    void deleteDataModelObjectsByDataModelId(DataModelId dataModelId);
}
