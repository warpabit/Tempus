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
package com.hashmapinc.server.common.transport.adaptor;

import com.hashmapinc.tempus.UnitConvertorContext;
import com.hashmapinc.tempus.exception.UnitConvertorContextException;
import com.hashmapinc.tempus.exception.UnitConvertorException;
import com.hashmapinc.tempus.model.Quantity;
import com.hashmapinc.tempus.service.UnitConvertorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnitConverter {

    private static UnitConvertorService unitConvertorService = null;

    private UnitConverter() {}

    static {
        try {
            unitConvertorService = UnitConvertorContext.getInstanceOfUnitConvertorService();
        } catch (UnitConvertorContextException e) {
            log.error(e.getMessage());
        }
    }

    public static Quantity convertToSiUnit(Quantity quantity) {
        try {
            return unitConvertorService.convertToSiUnit(quantity);
        } catch (UnitConvertorException ex) {
            log.info(ex.getMessage());
        }
        return quantity;
    }
}
