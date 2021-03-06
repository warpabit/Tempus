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
package com.hashmapinc.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.hashmapinc.server.common.data.Logo;
import com.hashmapinc.server.common.data.Tenant;
import com.hashmapinc.server.common.data.Theme;
import com.hashmapinc.server.common.data.UserSettings;
import com.hashmapinc.server.dao.logo.LogoService;
import com.hashmapinc.server.dao.theme.ThemeService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
public abstract class BaseUserSettingsControllerTest extends AbstractControllerTest {

    @Autowired
    ThemeService themeService;

    @Autowired
    LogoService logoService;


    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSaveUserSettingsByCustomerUser() throws Exception {
        loginCustomerUser();
        UserSettings userSettings = new UserSettings();
        userSettings.setKey("assignedDeviceTypes");
        userSettings.setJsonValue(mapper.readTree("{\"deviceType\":\"DT_A\"}"));
        doPost("/api/settings", userSettings).andExpect(status().isOk());

        doGet("/api/settings/assignedDeviceTypes")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.jsonValue.deviceType", is("DT_A")));
    }

    @Test
    public void testFindAdminSettingsByKey() throws Exception {
        loginSysAdmin();
        doGet("/api/settings/general")
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.key", is("general")))
        .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://localhost:8080")));
        
        doGet("/api/settings/mail")
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.key", is("mail")))
        .andExpect(jsonPath("$.jsonValue.smtpProtocol", is("smtp")))
        .andExpect(jsonPath("$.jsonValue.smtpHost", is("localhost")))
        .andExpect(jsonPath("$.jsonValue.smtpPort", is("25")));

        try {
            UserSettings userSettings = doGet("/api/settings/unknown", UserSettings.class);
            Assert.fail();
        } catch (Exception e) {
            //Jackson Bind will fail to map it user setting in case of null
            e.printStackTrace();
        }
    }

    
    @Test
    public void testSaveAdminSettings() throws Exception {
        loginSysAdmin();
        UserSettings userSettings = doGet("/api/settings/general", UserSettings.class);
        
        JsonNode jsonValue = userSettings.getJsonValue();
        ((ObjectNode) jsonValue).put("baseUrl", "http://myhost.org");
        userSettings.setJsonValue(jsonValue);

        doPost("/api/settings", userSettings).andExpect(status().isOk());
        
        doGet("/api/settings/general")
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://myhost.org")));
        
        ((ObjectNode) jsonValue).put("baseUrl", "http://localhost:8080");
        userSettings.setJsonValue(jsonValue);
        
        doPost("/api/settings", userSettings)
        .andExpect(status().isOk());
    }

    @Test
    public void testSaveUserSettingsWithEmptyKey() throws Exception {
        loginCustomerUser();
        UserSettings userSettings = new UserSettings();
        userSettings.setJsonValue(mapper.readTree("{\"deviceType\":\"DT_A\"}"));
        userSettings.setKey(null);
        doPost("/api/settings", userSettings)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Key should be specified")));
    }
    
    @Test
    public void testChangeUserSettingsKey() throws Exception {
        loginCustomerUser();
        UserSettings userSettings = new UserSettings();
        userSettings.setJsonValue(mapper.readTree("{\"deviceType\":\"DT_A\"}"));
        userSettings.setKey("deviceTypes");
        doPost("/api/settings", userSettings).andExpect(status().isOk());

        userSettings = doGet("/api/settings/deviceTypes", UserSettings.class);

        userSettings.setKey("newKey");
        doPost("/api/settings", userSettings)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("is prohibited")));
    }
    
    @Test
    public void testSaveAdminSettingsWithNewJsonStructure() throws Exception {
        loginSysAdmin();
        UserSettings userSettings = doGet("/api/settings/mail", UserSettings.class);
        JsonNode json = userSettings.getJsonValue();
        ((ObjectNode) json).put("newKey", "my new value");
        userSettings.setJsonValue(json);
        doPost("/api/settings", userSettings)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Provided json structure is different")));
    }
    
    @Test
    public void testSaveAdminSettingsWithNonTextValue() throws Exception {
        loginSysAdmin();
        UserSettings userSettings = doGet("/api/settings/mail", UserSettings.class);
        JsonNode json = userSettings.getJsonValue();
        ((ObjectNode) json).put("timeout", 10000L);
        userSettings.setJsonValue(json);
        doPost("/api/settings", userSettings)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Provided json structure can't contain non-text values")));
    }
    
    @Test
    public void testSendTestMail() throws Exception {
        loginSysAdmin();
        UserSettings userSettings = doGet("/api/settings/mail", UserSettings.class);
        doPost("/api/settings/testMail", userSettings)
        .andExpect(status().isOk());
    }

    @Test
    public void testgetEnabledTheme() throws Exception{

        Theme theme = new Theme();
        theme.setThemeName("Tempus Dark");
        theme.setThemeValue("themeDark");
        theme.setThemeStatus(true);
        themeService.saveTheme(theme);
        Theme themi = doGet("/api/theming", Theme.class);
        Assert.assertEquals(theme.getThemeStatus(),themi.getThemeStatus());
        themeService.deleteThemeEntryByvalue("themeDark");

    }


    @Test
    public void getThemes() throws Exception{

        loginSysAdmin();

        Theme theme1 = new Theme();
        theme1.setThemeName("Tempus Blue");
        theme1.setThemeValue("themeBlue");
        theme1.setThemeStatus(false);
        themeService.saveTheme(theme1);

        Theme theme2 = new Theme();
        theme2.setThemeName("Tempus Dark");
        theme2.setThemeValue("themeDark");
        theme2.setThemeStatus(false);
        themeService.saveTheme(theme2);

        List<Theme> theme = doGet("/api/settings/themes", List.class);
        Assert.assertEquals(2, theme.size());

        themeService.deleteThemeEntryByvalue("themeDark");
        themeService.deleteThemeEntryByvalue("themeBlue");

    }

    @Test
    public void getLogo() throws Exception{

        byte[] aByteArray = {0xa,0x2,0xf,(byte)0xff,(byte)0xff,(byte)0xff};

        Logo logo = new Logo();
        logo.setName("test.jpg");
        logo.setDisplay(true);
        logo.setFile(aByteArray);
        logoService.saveLogo(logo);

        Logo logoNew = doGet("/api/logo", Logo.class);

        Assert.assertEquals(logo.isDisplay(),logoNew.isDisplay());

        logoService.deleteLogoByName(logoNew.getName());

    }

    @Test
    public void testGetUserLogo() throws Exception {
        loginSysAdmin();
        String logo = doGet("/api/settings/"+tenantId.getId()+"/logo", String.class);
        Assert.assertEquals(logo.contains("TEST LOGO"),true);
    }
}
