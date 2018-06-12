/*
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
/* eslint-disable import/no-unresolved, import/default, no-unused-vars */
import './data_models.scss';
import addDataModel from './add-datamodel.tpl.html';

/*@ngInject*/
export function AddDataModelController($scope, $mdDialog, saveItemFunction, helpLinks, $log) {

    var vm = this;
    vm.helpLinks = helpLinks;
    vm.item = {};

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function add() {
        saveItemFunction(vm.item).then(function success(item) {
            vm.item = item;
            $scope.theForm.$setPristine();
            $mdDialog.hide();
        });
    }
}


/*@ngInject*/
export function DataModelsController($scope, $log, $rootScope, $state, $stateParams, userService, deviceService, types, attributeService, $q, dashboardService, applicationService, entityService, tempusboardService, utils, $filter, dashboardUtils, $mdDialog, $document, $translate) {
	var vm = this;
    //vm.save = save;

    vm.openDataModelDialog = openDataModelDialog;
    vm.cancel = cancel;
    vm.saveDataModelFunc = saveDataModelFunc;
    vm.AddDataModelController = AddDataModelController;


    function openDataModelDialog($event) {

        $mdDialog.show({
            controller: vm.AddDataModelController,
            controllerAs: 'vm',
            templateUrl: addDataModel,
            parent: angular.element($document[0].body),
            locals: {saveItemFunction: vm.saveDataModelFunc},
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
        }, function () {
        });
    }


    function saveDataModelFunc(dataModel) { $log.log(dataModel);
        var deferred = $q.defer();
        return deferred.promise;
    }

    function cancel() {
        $mdDialog.cancel();
    }

}