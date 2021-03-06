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
package com.hashmapinc.server.service.computation;

import com.hashmapinc.kubeless.apis.KubelessV1beta1CronTriggerApi;
import com.hashmapinc.kubeless.apis.KubelessV1beta1FunctionApi;
import com.hashmapinc.kubeless.apis.KubelessV1beta1KafkaTriggerApi;
import com.hashmapinc.kubeless.models.V1beta1AbstractType;
import com.hashmapinc.kubeless.models.V1beta1Function;
import com.hashmapinc.kubeless.models.V1beta1FunctionSpec;
import com.hashmapinc.kubeless.models.triggers.V1beta1CronJobTrigger;
import com.hashmapinc.kubeless.models.triggers.V1beta1CronJobTriggerSpec;
import com.hashmapinc.kubeless.models.triggers.V1beta1KafkaTrigger;
import com.hashmapinc.kubeless.models.triggers.V1beta1KafkaTriggerSpec;
import com.hashmapinc.server.common.data.computation.*;
import com.hashmapinc.server.common.data.id.TenantId;
import com.hashmapinc.server.utils.KubelessConnectionCache;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Response;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1ServiceSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class KubelessFunctionService implements ServerlessFunctionService {

    public static final String DEFAULT_NAMESPACE = "default";
    public static final String TXT = "txt";
    public static final String TXT_ZIP = "txt+zip";
    public static final String BASE64 = "base64";
    public static final String BASE64_ZIP = "base64+zip";
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int CREATED = 201;
    public static final int OK = 200;
    public static final String EXCEPTION = "Exception occured : {}";
    public static final String IO_EXECPTION = "IOExecption occured : {}";

    @Value("${kubernetes.cluster_mode_enabled}")
    private boolean clusterModeEnabled;


    @Value("${kubernetes.kube_config_path}")
    private String kublessConfigPath;

    private Base64.Decoder decoder = Base64.getDecoder();

    @Override
    public boolean deployFunction(Computations computation) {
        try {
            KubelessV1beta1FunctionApi functionApi = KubelessConnectionCache.getInstance(KubelessV1beta1FunctionApi.class.getName(),
                    clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);
            KubelessComputationMetadata md = (KubelessComputationMetadata) computation.getComputationMetadata();
            int resposeCode = INTERNAL_SERVER_ERROR;
            switch (md.getFunctionContentType()) {
                case TXT_ZIP:
                case TXT:
                    String textContent = convertToTxt(md.getFunctionContent());
                    md.setFunctionContent(textContent);
                    V1beta1AbstractType<V1beta1FunctionSpec> v1beta1Function = createV1beta1Function(md, computation.getTenantId());
                    Call call = functionApi.createFunctionCall((V1beta1Function)v1beta1Function);
                    Response response = call.execute();
                    resposeCode = response.code();
                    break;
                case BASE64_ZIP:
                case BASE64:
                    v1beta1Function = createV1beta1Function(md, computation.getTenantId());
                    call = functionApi.createFunctionCall((V1beta1Function)v1beta1Function);
                    response = call.execute();
                    resposeCode = response.code();
                    break;
                    default:
                        break;
            }

            if ( resposeCode == CREATED) {
                log.info("Function was deployed!!");
                return true;
            }
        }
        catch (ApiException e){
            log.info("Kubeless api kubeconfig exception for deployFunction function : {}", e);
        } catch (IOException e) {
            log.info("IOExecption : {}", e);
        }
        return false;
    }

    @Override
    public boolean functionExists(Computations computation) {
        try {
            KubelessComputationMetadata md = (KubelessComputationMetadata)(computation.getComputationMetadata());
            KubelessV1beta1FunctionApi functionApi = KubelessConnectionCache.getInstance(KubelessV1beta1FunctionApi.class.getName(),
                    clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);

            Call call = functionApi.getFunctionCall(md.getFunction());
            Response response = call.execute();
            if (response.code() == OK)
                return true;
        } catch (ApiException e) {
            log.error("Kubeless api exception for fetch function : {}", e);
        } catch (IOException e) {
            log.error("Exception occured in call execution : {}", e);
        } catch (Exception e) {
            log.error(EXCEPTION, e);
        }

        return false;
    }

    @Override
    public boolean deleteFunction(Computations computation) {
        try {
            KubelessComputationMetadata md = (KubelessComputationMetadata)(computation.getComputationMetadata());
            KubelessV1beta1FunctionApi functionApi = KubelessConnectionCache.getInstance(KubelessV1beta1FunctionApi.class.getName(),
                    clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);

            Call call = functionApi.deleteFunctionCall(md.getFunction());
            Response response = call.execute();
            if (response.code() == OK) {
                log.info("Deleted kubeless funtion from kubernetes!!!");
                return true;
            }
        } catch (ApiException e) {
            log.error("Kubeless api exception for fetch function : {}", e);
        } catch (IOException e) {
            log.error("Exception occured in call execution : {}", e);
        } catch (Exception e) {
            log.error(EXCEPTION,e);
        }
        return false;
    }

    @Override
    public boolean createTrigger(ComputationJob computationJob) {
        try {
            if (computationJob.getConfiguration().getClass() == KafkaKubelessTrigger.class) {
                V1beta1KafkaTrigger v1beta1KafkaTrigger = createKafkaTrigger(computationJob);
                KubelessV1beta1KafkaTriggerApi triggerApi = KubelessConnectionCache.getInstance(KubelessV1beta1KafkaTriggerApi.class.getName(),
                        clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);
                Call call = triggerApi.createKafkaTriggerCall(v1beta1KafkaTrigger);
                Response result = call.execute();
                if(result.code() == CREATED) {
                    return true;
                }

            } else if (computationJob.getConfiguration().getClass() == CronKubelessTrigger.class) {
                V1beta1CronJobTrigger v1beta1CronJobTrigger = createCronTrigger(computationJob);
                KubelessV1beta1CronTriggerApi triggerApi = KubelessConnectionCache.getInstance(KubelessV1beta1CronTriggerApi.class.getName(),
                        clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);
                Call call = triggerApi.createCronTriggerCall(v1beta1CronJobTrigger);
                Response result = call.execute();
                if(result.code() == CREATED) {
                    return true;
                }
            }
        } catch (ApiException e) {
            log.error("Kubeless api exception for create trigger : {}", e);
        } catch (IOException e) {
            log.error(IO_EXECPTION, e);
        }
        return false;
    }

    private V1beta1KafkaTrigger createKafkaTrigger(ComputationJob computationJob) {
        KafkaKubelessTrigger trigger = (KafkaKubelessTrigger)computationJob.getConfiguration();
        V1beta1KafkaTrigger v1beta1KafkaTrigger = new V1beta1KafkaTrigger();
        V1ObjectMeta metaData = new V1ObjectMeta().name(computationJob.getName());
        v1beta1KafkaTrigger.setMetadata(metaData);
        V1beta1KafkaTriggerSpec spec = createV1beta1KafkaTriggerSpec(trigger, computationJob.getTenantId());
        v1beta1KafkaTrigger.setMetadata(metaData);
        v1beta1KafkaTrigger.setSpec(spec);

        return v1beta1KafkaTrigger;
    }

    private V1beta1CronJobTrigger createCronTrigger(ComputationJob computationJob) {
        CronKubelessTrigger trigger = (CronKubelessTrigger) computationJob.getConfiguration();
        V1beta1CronJobTrigger v1beta1CronJobTrigger = new V1beta1CronJobTrigger();
        V1ObjectMeta metaData = new V1ObjectMeta().name(computationJob.getName());
        V1beta1CronJobTriggerSpec spec = new V1beta1CronJobTriggerSpec();
        spec.setFunctionName(trigger.getFunctionName());
        spec.setSchedule(trigger.getSchedule());
        v1beta1CronJobTrigger.setMetadata(metaData);
        v1beta1CronJobTrigger.setSpec(spec);

        return v1beta1CronJobTrigger;
    }

    private V1beta1KafkaTriggerSpec createV1beta1KafkaTriggerSpec(KafkaKubelessTrigger trigger, TenantId tenantId) {
        V1LabelSelector labelSelector = new V1LabelSelector();
        labelSelector.putMatchLabelsItem("created-by", tenantId.toString());

        Map<String, String> functionSelector = trigger.getFunctionSelector();
        functionSelector.entrySet().stream()
                .forEach(e -> labelSelector.putMatchLabelsItem(e.getKey(), e.getValue()));

        V1beta1KafkaTriggerSpec spec = new V1beta1KafkaTriggerSpec();
        spec.setTopic(trigger.getTopic());
        spec.setLabelSelector(labelSelector);
        return spec;
    }

    @Override
    public boolean triggerExists(ComputationJob computationJob) {
        try {
            if (computationJob.getConfiguration().getClass() == KafkaKubelessTrigger.class) {
                KubelessV1beta1KafkaTriggerApi triggerApi = KubelessConnectionCache.getInstance(KubelessV1beta1KafkaTriggerApi.class.getName(),
                        clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);
                Call call = triggerApi.getKafkaTriggerCall(computationJob.getName());
                Response response = call.execute();
                if (response.code() == OK)
                    return true;
            } else if (computationJob.getConfiguration().getClass() == CronKubelessTrigger.class) {
                KubelessV1beta1CronTriggerApi triggerApi = KubelessConnectionCache.getInstance(KubelessV1beta1CronTriggerApi.class.getName(),
                        clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);
                Call call = triggerApi.getCronTriggerCall(computationJob.getName());
                Response response = call.execute();
                if (response.code() == OK)
                    return true;
            }
        } catch (ApiException e) {
            log.error("Kubeless api exception for check trigger : {}", e);
        } catch (IOException e) {
            log.error(IO_EXECPTION, e);
        }
        return false;
    }

    @Override
    public boolean deleteTrigger(ComputationJob computationJob) {
        try {
            if (computationJob.getConfiguration().getClass() == KafkaKubelessTrigger.class) {
                KubelessV1beta1KafkaTriggerApi triggerApi = KubelessConnectionCache.getInstance(KubelessV1beta1KafkaTriggerApi.class.getName(),
                        clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);
                Call call = triggerApi.deleteKafkaTriggerCall(computationJob.getName());
                Response response = call.execute();
                if (response.code() == OK)
                    return true;
            } else if (computationJob.getConfiguration().getClass() == CronKubelessTrigger.class) {
                KubelessV1beta1CronTriggerApi triggerApi = KubelessConnectionCache.getInstance(KubelessV1beta1CronTriggerApi.class.getName(),
                        clusterModeEnabled, kublessConfigPath, DEFAULT_NAMESPACE);
                Call call = triggerApi.deleteCronTriggerCall(computationJob.getName());
                Response response = call.execute();
                if (response.code() == OK)
                    return true;
            }
        } catch (ApiException e) {
            log.error("Kubeless api exception for deleteFunction trigger : {}", e);
        } catch (IOException e) {
            log.error(IO_EXECPTION, e);
        }
        return false;
    }

    private V1beta1Function createV1beta1Function(ComputationMetadata computationMetadata, TenantId tenantId) {
        try {
            V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
            v1ObjectMeta.setName(((KubelessComputationMetadata)computationMetadata).getFunction());
            v1ObjectMeta.setNamespace(DEFAULT_NAMESPACE);
            Map<String, String> labelMap = getSelectorMap(v1ObjectMeta.getName(), tenantId);
            v1ObjectMeta.setLabels(labelMap);
            V1beta1FunctionSpec v1beta1FunctionSpec = createV1beta1FunctionSpec(computationMetadata, tenantId);
            V1beta1Function v1beta1Function = new V1beta1Function();
            v1beta1Function.setMetadata(v1ObjectMeta);
            v1beta1Function.setSpec(v1beta1FunctionSpec);
            return v1beta1Function;
        }
        catch (Exception e) {
            log.info(EXCEPTION, e);
        }
        return null;
    }

    private Map<String, String> getSelectorMap(String name, TenantId tenantId) {
        Map<String, String> map = new HashMap<>();
        map.put("function", name);
        map.put("created-by", tenantId.toString());
        return map;
    }

    private V1beta1FunctionSpec createV1beta1FunctionSpec(ComputationMetadata computationMetadata, TenantId tenantId) {
        KubelessComputationMetadata md = (KubelessComputationMetadata)computationMetadata;
        V1beta1FunctionSpec v1beta1FunctionSpec = new V1beta1FunctionSpec();
        byte[] funcBytes = decoder.decode(md.getFunctionContent());
        String funcContent = new String(funcBytes);
        v1beta1FunctionSpec.setFunction(funcContent);
        if(md.getDependencies() != null) {
            byte[] dependencyBytes = decoder.decode(md.getDependencies());
            String dependencyContent = new String(dependencyBytes);
            v1beta1FunctionSpec.dependencies(dependencyContent);
        }
        v1beta1FunctionSpec.setHandler(md.getHandler());
        v1beta1FunctionSpec.setRuntime(md.getRuntime());
        v1beta1FunctionSpec.functionContentType("text");
        V1ServiceSpec serviceSpec = new V1ServiceSpec();
        serviceSpec.selector(getSelectorMap(md.getFunction(), tenantId));
        v1beta1FunctionSpec.setServiceSpec(serviceSpec);
        v1beta1FunctionSpec.setTimeout(md.getTimeout());
        return v1beta1FunctionSpec;
    }

    private String convertToTxt(String functionContent){
        byte[] decodedContent = decoder.decode(functionContent);
        return new String(decodedContent);
    }

}
