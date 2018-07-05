/**
 * Copyright © 2017-2018 Hashmap, Inc
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
package com.hashmapinc.server.dao.schema;

import com.hashmapinc.server.common.data.id.SchemaId;
import com.hashmapinc.server.common.data.schema.Schema;
import com.hashmapinc.server.dao.entity.AbstractEntityService;
import com.hashmapinc.server.dao.exception.IncorrectParameterException;
import com.hashmapinc.server.dao.service.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import static com.hashmapinc.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class SchemaServiceImpl extends AbstractEntityService implements SchemaService {

    public static final String INCORRECT_SCHEMA_ID = "Incorrect schemaId ";

    @Autowired
    private SchemaDao schemaDao;

    @Override
    public Schema findSchemaByName(String name) {
        log.trace("Executing findSchemaByName [{}]", name);
        return schemaDao.findSchemaByName(name);
    }

    @Override
    public Schema findSchemaBySchemaId(SchemaId schemaId) {
        log.trace("Executing findSchemaByTenantId [{}]", schemaId);
        validateId(schemaId, INCORRECT_SCHEMA_ID + schemaId);
        return  schemaDao.findSchemaBySchemaId(schemaId.getId());
    }

    @Override
    public Schema saveSchema(Schema schema) {
        log.trace("Executing saveSchema [{}]", schema);
        //TODO: Add SchemaValidator
        Schema savedSchema = schemaDao.save(schema);
        return savedSchema;
    }

    @Override
    public void deleteSchema(SchemaId schemaId) {

        log.trace("Executing deleteSchema [{}]", schemaId);
        Validator.validateId(schemaId, INCORRECT_SCHEMA_ID + schemaId);
        Schema schema = findSchemaBySchemaId(schemaId);
        if (schema == null) {
            throw new IncorrectParameterException("Unable to delete non-existent schema.");
        }
        schemaDao.removeById(schemaId.getId());
    }
}