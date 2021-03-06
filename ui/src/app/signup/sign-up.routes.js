/*
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
/* eslint-disable import/no-unresolved, import/default */

import signUpTemplate from './signup.tpl.html';
import activationLinkTemplate from './activation-link.tpl.html'
import policyTemplate from './policy.tpl.html'
/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function SignUpRoutes($stateProvider) {
    $stateProvider.state('signup', {
        url: '/signup',
        module: 'public',
        views: {
            "@": {
                controller: 'SignUpController',
                controllerAs: 'vm',
                templateUrl: signUpTemplate
            }
        },
        data: {
            pageTitle: 'Sign Up'
        }
    }).state('activationLink', {
        url: '/activationLink',
        module: 'public',
        params: {'email': ''},
        views: {
              "@": {
                  controller: 'ActivationLinkController',
                  controllerAs: 'vm',
                  templateUrl: activationLinkTemplate
              }
        },
        data: {
              pageTitle: 'Activation Link'
        }
    }).state('policy', {
        url: '/policy',
        module: 'public',
        views: {
            "@": {
                templateUrl: policyTemplate
            }
        },
        data: {
            pageTitle: 'Privacy Policy'
        }
    });
}
