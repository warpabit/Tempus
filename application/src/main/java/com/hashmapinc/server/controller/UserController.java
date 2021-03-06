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

import org.apache.commons.lang.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashmapinc.server.common.data.CustomerGroup;
import com.hashmapinc.server.common.data.EntityType;
import com.hashmapinc.server.common.data.Tenant;
import com.hashmapinc.server.common.data.User;
import com.hashmapinc.server.common.data.audit.ActionType;
import com.hashmapinc.server.common.data.id.*;
import com.hashmapinc.server.common.data.page.TextPageData;
import com.hashmapinc.server.common.data.page.TextPageLink;
import com.hashmapinc.server.common.data.security.Authority;
import com.hashmapinc.server.common.data.security.UserCredentials;
import com.hashmapinc.server.dao.model.ModelConstants;
import com.hashmapinc.server.common.data.exception.TempusErrorCode;
import com.hashmapinc.server.common.data.exception.TempusException;
import com.hashmapinc.server.dao.tenant.TenantService;
import com.hashmapinc.server.requests.CreateUserRequest;
import com.hashmapinc.server.requests.IdentityUser;
import com.hashmapinc.server.dao.mail.MailService;
import com.hashmapinc.server.service.recaptcha.RecaptchaService;
import com.hashmapinc.server.service.security.model.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class UserController extends BaseController {

    public static final String USER_ID = "userId";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";
    public static final String ACTIVATE_URL_PATTERN = "%s/api/noauth/activate?activateToken=%s";

    @Value("${identity.url}")
    private String identityUrl;

    @Autowired
    private MailService mailService;

    @Autowired
    @Qualifier("clientRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RecaptchaService captchaService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/user/{userId}")
    @ResponseBody
    public User getUserById(@PathVariable(USER_ID) String strUserId) throws TempusException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() == Authority.CUSTOMER_USER && !authUser.getId().equals(userId)) {
                throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        TempusErrorCode.PERMISSION_DENIED);
            }
            return checkUserId(userId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/user")
    @ResponseBody
    public User saveUser(@RequestBody User user,
                         @RequestParam(required = false) String activationType,
                         HttpServletRequest request) throws TempusException {

        try {
            // activationType = "mail", "link", "external"
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() == Authority.CUSTOMER_USER && !authUser.getId().equals(user.getId())) {
                throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        TempusErrorCode.PERMISSION_DENIED);
            }
            if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
                user.setTenantId(getCurrentUser().getTenantId());
            }
            User savedUser;
            final boolean isNewUser = user.getId() == null;
            if(isNewUser){
                savedUser = createUser(user, activationType, request);
            }else{
                savedUser = updateUser(user);
            }

            logEntityAction(savedUser.getId(), savedUser,
                    savedUser.getCustomerId(),
                    user.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN && isNewUser && savedUser != null) {
                assignDefaultGroupToTenantUser(savedUser.getTenantId(), savedUser.getId());
            }

            return savedUser;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.USER), user,
                    null, user.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }


    @PostMapping(value = "/noauth/user")
    @ResponseBody
    public User saveTrialUser(@RequestBody User user, @RequestParam String recaptchaResponse,
                         HttpServletRequest request) throws TempusException {

        String captchaVerifyMessage =
                captchaService.verifyRecaptcha(recaptchaResponse);

        if (StringUtils.isNotEmpty(captchaVerifyMessage)) {
            throw new TempusException("Invalid captcha . Please sign up again",
                    TempusErrorCode.BAD_REQUEST_PARAMS);
        }

        Tenant savedTenant = null;
        try {
            user.setAuthority(Authority.TENANT_ADMIN);
            savedTenant = checkNotNull(createTenant(user));
            user.setTenantId(savedTenant.getId());

            user = setCurrentTimeInUser(user);

            User savedUser = createUser(user, "mail", request);
            if (savedUser != null) {
                assignDefaultGroupToTenantUser(savedUser.getTenantId(), savedUser.getId());
                mailService.sendNewUserSignInNotificationToSysAdmin(user);
            }
            return savedUser;
        } catch (Exception e) {
            if(savedTenant != null)
                tenantService.deleteTenant(savedTenant.getId());
            throw handleException(e);
        }
    }

    private User setCurrentTimeInUser(User user) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,String> additionalInfo = mapper.convertValue(user.getAdditionalInfo(), Map.class);
        additionalInfo.put("date",Long.toString(atStartOfDay().getTime()));
        additionalInfo.put("trialAccount","true");
        JsonNode jsonNode = mapper.convertValue(additionalInfo, JsonNode.class);
        user.setAdditionalInfo(jsonNode);

        return user;
    }

    private Date atStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = Date.from(Instant.now());
        long currentDateTime = currentDate.getTime();
        calendar.setTimeInMillis(currentDateTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Tenant createTenant(User newUser) {
        Tenant tenant = new Tenant();
        tenant.setTitle(newUser.getEmail());
        return tenantService.saveTenant(tenant);
    }

    private void assignDefaultGroupToTenantUser(TenantId tenantId, UserId userId) {
        TextPageData<CustomerGroup> customerGroupByTenant =
                customerGroupService.findCustomerGroupsByTenantIdAndCustomerId(tenantId, new CustomerId(ModelConstants.NULL_UUID), new TextPageLink(1));
        List<CustomerGroupId> customerGroupIds = customerGroupByTenant.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        userService.assignGroups(userId, customerGroupIds);
    }

    private User updateUser(@RequestBody User user) throws TempusException {
        ResponseEntity<IdentityUser> response = restTemplate.exchange(identityUrl + "/" +user.getId(),
                HttpMethod.PUT, new HttpEntity<>(new IdentityUser(user)), IdentityUser.class);
        User savedUser;
        if(response.getStatusCode().equals(HttpStatus.OK)) {
            savedUser = response.getBody().toUser();
        }else{
            throw new TempusException(response.getBody().toString(), TempusErrorCode.GENERAL);
        }
        return savedUser;
    }

    private User createUser(@RequestBody User user, @RequestParam String activationType, HttpServletRequest request) throws TempusException, com.fasterxml.jackson.core.JsonProcessingException {
        boolean sendEmail = user.getId() == null && activationType.equals("mail");
        ObjectMapper mapper = new ObjectMapper();
        CreateUserRequest userRequest = CreateUserRequest.builder().user(new IdentityUser(user)).activationType(activationType).build();
        User savedUser;

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(identityUrl, userRequest, JsonNode.class);

        if(response.getStatusCode().equals(HttpStatus.CREATED)){
            JsonNode body = response.getBody();
            savedUser = checkNotNull(mapper.treeToValue(body.get("user"), IdentityUser.class).toUser());

            if (sendEmail) {
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        response.getBody().get("activationToken").asText());
                String email = savedUser.getEmail();
                try {
                    mailService.sendActivationEmail(activateUrl, email);
                } catch (TempusException e) {
                    userService.deleteUser(savedUser.getId());
                    throw e;
                }
            }
        }else{
            throw new TempusException(response.getBody().asText(), TempusErrorCode.GENERAL);
        }
        return savedUser;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/user/sendActivationMail")
    @ResponseStatus(value = HttpStatus.OK)
    public void sendActivationEmail(
            @RequestParam(value = "email") String email,
            HttpServletRequest request) throws TempusException {
        try {
            User user = checkNotNull(userService.findUserByEmail(email));
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getId());
            if (!userCredentials.isEnabled()) {
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                mailService.sendActivationEmail(activateUrl, email);
            } else {
                throw new TempusException("User is already active!", TempusErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PostMapping(value = "/noauth/user/resendActivationMail")
    @ResponseStatus(value = HttpStatus.OK)
    public void resendActivationEmail(
            @RequestParam(value = "email") String email,
            HttpServletRequest request) throws TempusException {
        try {
            User user = checkNotNull(userService.findUserByEmail(email));
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getId());
            if (!userCredentials.isEnabled()) {
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                mailService.sendActivationEmail(activateUrl, email);
            } else {
                throw new TempusException("User is already active!", TempusErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/user/{userId}/activationLink", produces = "text/plain")
    @ResponseBody
    public String getActivationLink(
            @PathVariable(USER_ID) String strUserId,
            HttpServletRequest request) throws TempusException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() == Authority.CUSTOMER_USER && !authUser.getId().equals(userId)) {
                throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        TempusErrorCode.PERMISSION_DENIED);
            }
            String userGetUrl = identityUrl + "/" + userId.getId();
            ResponseEntity<IdentityUser> response = restTemplate.getForEntity(userGetUrl, IdentityUser.class);
            if(response.getStatusCode().equals(HttpStatus.OK)){
                IdentityUser identityUser = response.getBody();
                if (!identityUser.isEnabled()) {
                    String baseUrl = constructBaseUrl(request);
                    String activationToken = restTemplate.getForObject(userGetUrl + "/activation-token", String.class);
                    if(!StringUtils.isEmpty(activationToken)){
                        return String.format(ACTIVATE_URL_PATTERN, baseUrl, activationToken);
                    }else{
                        throw new TempusException("Error while getting Activation token", TempusErrorCode.GENERAL);
                    }
                } else {
                    throw new TempusException("User is already active!", TempusErrorCode.BAD_REQUEST_PARAMS);
                }
            }else{
                throw new TempusException("Error while getting user", TempusErrorCode.GENERAL);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/user/{userId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteUser(@PathVariable(USER_ID) String strUserId) throws TempusException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId);
            userService.deleteUser(userId);

            logEntityAction(userId, user,
                    user.getCustomerId(),
                    ActionType.DELETED, null, strUserId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.USER),
                    null,
                    null,
                    ActionType.DELETED, e, strUserId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping(value = "/tenant/{tenantId}/users", params = { "limit" })
    @ResponseBody
    public TextPageData<User> getTenantAdmins(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws TempusException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(userService.findTenantAdmins(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/customer/{customerId}/users", params = { "limit" })
    @ResponseBody
    public TextPageData<User> getCustomerUsers(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws TempusException {
        checkParameter("customerId", strCustomerId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(userService.findCustomerUsers(tenantId, customerId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/user/{userId}/groups", params = { "limit" })
    @ResponseBody
    public TextPageData<CustomerGroup> getGroupsByUserId(
            @PathVariable(USER_ID) String strUserId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws TempusException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return customerGroupService.findByUserId(userId, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/user/{userId}/groups")
    @ResponseBody
    public User assignGroupsToUser(
            @PathVariable(USER_ID) String strUserId ,
            @RequestBody List<UUID> groupUuids) throws TempusException {
        checkParameter(USER_ID, strUserId);
        try{
            UserId userId = new UserId(toUUID(strUserId));
            List<CustomerGroupId> customerGroupIds = groupUuids.stream().map(CustomerGroupId::new).collect(Collectors.toList());
            for (CustomerGroupId customerGroupId: customerGroupIds) {
                checkCustomerGroupId(customerGroupId);
            }
            return checkNotNull(userService.assignGroups(userId, customerGroupIds));
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PutMapping(value = "/user/{userId}/groups")
    @ResponseBody
    public User unassignGroupsFromUser(
            @PathVariable(USER_ID) String strUserId ,
            @RequestBody List<UUID> groupUuids) throws TempusException {
        checkParameter(USER_ID, strUserId);
        try{
            UserId userId = new UserId(toUUID(strUserId));
            List<CustomerGroupId> customerGroupIds = groupUuids.stream().map(CustomerGroupId::new).collect(Collectors.toList());
            for (CustomerGroupId customerGroupId: customerGroupIds) {
                checkCustomerGroupId(customerGroupId);
            }
            return checkNotNull(userService.unassignGroups(userId, customerGroupIds));
        } catch (Exception e){
            throw handleException(e);
        }
    }
}
