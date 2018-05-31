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

package com.hashmapinc.server.dao.sql.theme;

import com.hashmapinc.server.dao.model.sql.ThemeEntity;
import com.hashmapinc.server.dao.util.SqlDao;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@SqlDao
public interface ThemeRepository extends CrudRepository<ThemeEntity, String> {

    @Query("SELECT th FROM ThemeEntity th WHERE th.is_enabled = 'true'")
    ThemeEntity  findEnabledTheme();

//    @Query("SELECT th FROM ThemeEntity th WHERE th.value = :value")
    ThemeEntity findByValue (/*@Param("value") */String value);
}