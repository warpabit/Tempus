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
package com.hashmapinc.server.dao.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.hashmapinc.server.common.data.User;
import com.hashmapinc.server.common.data.exception.TempusException;
import com.hashmapinc.server.common.data.id.TenantId;

public interface MailService {

    void updateMailConfiguration();

    void sendEmail(String email, String subject, String message) throws TempusException;
    
    void sendTestMail(JsonNode config, String email) throws TempusException;
    
    void sendActivationEmail(String activationLink, String email) throws TempusException;
    
    void sendAccountActivatedEmail(String loginLink, String email) throws TempusException;
    
    void sendResetPasswordEmail(String passwordResetLink, String email) throws TempusException;
    
    void sendPasswordWasResetEmail(String loginLink, String email) throws TempusException;

    void sendAttributeMissingMail(String deviceName , TenantId tenantId) throws TempusException;

    void sendAssetNotPresentMail(String deviceName ,String assetName, TenantId tenantId) throws TempusException;

    void sendExpiryRemainderMailToUser(String email) throws TempusException;

    void sendAccountExpiryMail(String email) throws TempusException;

    void sendNewUserSignInNotificationToSysAdmin(User user)throws TempusException;

}
