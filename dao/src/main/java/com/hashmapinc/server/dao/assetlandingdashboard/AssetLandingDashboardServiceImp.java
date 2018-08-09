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
package com.hashmapinc.server.dao.assetlandingdashboard;

import com.hashmapinc.server.common.data.AssetLandingDashboard;
import com.hashmapinc.server.common.data.id.DashboardId;
import com.hashmapinc.server.common.data.id.DataModelObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetLandingDashboardServiceImp implements AssetLandingDashboardService{

    @Autowired
    AssetLandingDashboardServiceDao assetLandingDashboardServiceDao;

    @Override
    public AssetLandingDashboard save(AssetLandingDashboard assetLandingDashboard) {
        return assetLandingDashboardServiceDao.save(assetLandingDashboard);
    }

    @Override
    public List<AssetLandingDashboard> findByDataModelObjectId(DataModelObjectId dataModelObjectId) {
        return assetLandingDashboardServiceDao.findByDataModelObjectId(dataModelObjectId);
    }

    @Override
    public AssetLandingDashboard findByDashboardId(DashboardId dashboardId) {
        return assetLandingDashboardServiceDao.findByDashboardId(dashboardId);
    }

    @Override
    public void removeByDashboardId(DashboardId dashboardId) {
        assetLandingDashboardServiceDao.removeByDashBoardId(dashboardId);
    }
}
