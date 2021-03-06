--
-- Copyright © 2016-2018 The Thingsboard Authors
-- Modifications © 2017-2018 Hashmap, Inc
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE TABLE IF NOT EXISTS user_settings (
    id varchar(31) NOT NULL CONSTRAINT user_settings_pkey PRIMARY KEY,
    json_value varchar,
    key varchar(255),
    user_id varchar(31)
);

CREATE TABLE IF NOT EXISTS alarm (
    id varchar(31) NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY,
    ack_ts bigint,
    clear_ts bigint,
    additional_info varchar,
    end_ts bigint,
    originator_id varchar(31),
    originator_type integer,
    propagate boolean,
    severity varchar(255),
    start_ts bigint,
    status varchar(255),
    tenant_id varchar(31),
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS asset (
    id varchar(31) NOT NULL CONSTRAINT asset_pkey PRIMARY KEY,
    additional_info varchar,
    customer_id varchar(31),
    name varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    type varchar(255),
    data_model_object_id varchar(31) DEFAULT '1b21dd2138140008080808080808080'
);

CREATE TABLE IF NOT EXISTS audit_log (
    id varchar(31) NOT NULL CONSTRAINT audit_log_pkey PRIMARY KEY,
    tenant_id varchar(31),
    customer_id varchar(31),
    entity_id varchar(31),
    entity_type varchar(255),
    entity_name varchar(255),
    user_id varchar(31),
    user_name varchar(255),
    action_type varchar(255),
    action_data varchar(1000000),
    action_status varchar(255),
    action_failure_details varchar(1000000)
);

CREATE TABLE IF NOT EXISTS attribute_kv (
  entity_type varchar(255),
  entity_id varchar(31),
  attribute_type varchar(255),
  attribute_key varchar(255),
  bool_v boolean,
  str_v varchar(10000000),
  long_v bigint,
  dbl_v double precision,
  json_v varchar,
  last_update_ts bigint,
  unit varchar,
  CONSTRAINT attribute_kv_unq_key UNIQUE (entity_type, entity_id, attribute_type, attribute_key)
);

CREATE TABLE IF NOT EXISTS metadata_entries(
    tenant_id varchar(31) NOT NULL,
    metadata_config_id varchar(31) NOT NULL,
    datasource_name varchar(255) NOT NULL,
    attribute varchar(255) NOT NULL,
    key varchar(255) NOT NULL,
    value varchar(10000000) NOT NULL,
    last_update_ts bigint,
    CONSTRAINT metadata_entries_pkey PRIMARY KEY (tenant_id, metadata_config_id, datasource_name, attribute, key)
);

CREATE TABLE IF NOT EXISTS component_descriptor (
    id varchar(31) NOT NULL CONSTRAINT component_descriptor_pkey PRIMARY KEY,
    actions varchar(255),
    clazz varchar,
    configuration_descriptor varchar,
    name varchar(255),
    scope varchar(255),
    search_text varchar(255),
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS customer (
    id varchar(31) NOT NULL CONSTRAINT customer_pkey PRIMARY KEY,
    additional_info varchar,
    address varchar,
    address2 varchar,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    phone varchar(255),
    search_text varchar(255),
    state varchar(255),
    tenant_id varchar(31),
    title varchar(255),
    zip varchar(255),
    data_model_id varchar(31) DEFAULT '1b21dd2138140008080808080808080'
);

CREATE TABLE IF NOT EXISTS dashboard (
    id varchar(31) NOT NULL CONSTRAINT dashboard_pkey PRIMARY KEY,
    configuration varchar(10000000),
    assigned_customers varchar(1000000),
    search_text varchar(255),
    tenant_id varchar(31),
    title varchar(255),
    type varchar DEFAULT 'DEFAULT'
);

CREATE TABLE IF NOT EXISTS device (
    id varchar(31) NOT NULL CONSTRAINT device_pkey PRIMARY KEY,
    additional_info varchar,
    customer_id varchar(31),
    type varchar(255),
    name varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    data_model_object_id varchar(31) DEFAULT '1b21dd2138140008080808080808080'
);

CREATE TABLE IF NOT EXISTS data_model (
    id varchar (31) NOT NULL CONSTRAINT data_model_pkey PRIMARY KEY,
    name varchar(255),
    tenant_id varchar(31),
    search_text varchar(255),
    additional_info varchar,
    last_update_ts bigint
);

CREATE TABLE IF NOT EXISTS device_credentials (
    id varchar(31) NOT NULL CONSTRAINT device_credentials_pkey PRIMARY KEY,
    credentials_id varchar,
    credentials_type varchar(255),
    credentials_value varchar,
    device_id varchar(31)
);

CREATE TABLE IF NOT EXISTS event (
    id varchar(31) NOT NULL CONSTRAINT event_pkey PRIMARY KEY,
    body varchar,
    entity_id varchar(31),
    entity_type varchar(255),
    event_type varchar(255),
    event_uid varchar(255),
    tenant_id varchar(31),
    CONSTRAINT event_unq_key UNIQUE (tenant_id, entity_type, entity_id, event_type, event_uid)
);

CREATE TABLE IF NOT EXISTS plugin (
    id varchar(31) NOT NULL CONSTRAINT plugin_pkey PRIMARY KEY,
    additional_info varchar,
    api_token varchar(255),
    plugin_class varchar(255),
    configuration varchar,
    name varchar(255),
    public_access boolean,
    search_text varchar(255),
    state varchar(255),
    tenant_id varchar(31)
);

CREATE TABLE IF NOT EXISTS relation (
    from_id varchar(31),
    from_type varchar(255),
    to_id varchar(31),
    to_type varchar(255),
    relation_type_group varchar(255),
    relation_type varchar(255),
    additional_info varchar,
    CONSTRAINT relation_unq_key UNIQUE (from_id, from_type, relation_type_group, relation_type, to_id, to_type)
);

CREATE TABLE IF NOT EXISTS rule (
    id varchar(31) NOT NULL CONSTRAINT rule_pkey PRIMARY KEY,
    action varchar,
    additional_info varchar,
    filters varchar,
    name varchar(255),
    plugin_token varchar(255),
    processor varchar,
    search_text varchar(255),
    state varchar(255),
    tenant_id varchar(31),
    weight integer
);

CREATE TABLE IF NOT EXISTS tenant (
    id varchar(31) NOT NULL CONSTRAINT tenant_pkey PRIMARY KEY,
    additional_info varchar,
    address varchar,
    address2 varchar,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    phone varchar(255),
    region varchar(255),
    search_text varchar(255),
    state varchar(255),
    title varchar(255),
    zip varchar(255),
    logo varchar(255)
);

CREATE TABLE IF NOT EXISTS ts_kv (
    entity_type varchar(255) NOT NULL,
    entity_id varchar(31) NOT NULL,
    key varchar(255) NOT NULL,
    ts bigint NOT NULL,
    bool_v boolean,
    str_v varchar(10000000),
    long_v bigint,
    dbl_v double precision,
    json_v varchar,
    unit varchar,
    CONSTRAINT ts_kv_unq_key UNIQUE (entity_type, entity_id, key, ts)
);

CREATE TABLE IF NOT EXISTS ts_kv_latest (
    entity_type varchar(255) NOT NULL,
    entity_id varchar(31) NOT NULL,
    key varchar(255) NOT NULL,
    ts bigint NOT NULL,
    bool_v boolean,
    str_v varchar(10000000),
    long_v bigint,
    dbl_v double precision,
    json_v varchar,
    unit varchar,
    CONSTRAINT ts_kv_latest_unq_key UNIQUE (entity_type, entity_id, key)
);

CREATE TABLE IF NOT EXISTS ds_kv (
    entity_type varchar(255) NOT NULL,
    entity_id varchar(31) NOT NULL,
    key varchar(255) NOT NULL,
    ds double precision NOT NULL,
    bool_v boolean,
    str_v varchar(10000000),
    long_v bigint,
    dbl_v double precision,
    json_v varchar,
    unit varchar,
    CONSTRAINT ds_kv_unq_key UNIQUE (entity_type, entity_id, key, ds)
);

CREATE TABLE IF NOT EXISTS ds_kv_latest (
    entity_type varchar(255) NOT NULL,
    entity_id varchar(31) NOT NULL,
    key varchar(255) NOT NULL,
    ds double precision NOT NULL,
    bool_v boolean,
    str_v varchar,
    long_v bigint,
    dbl_v double precision,
    json_v varchar,
    unit varchar,
    CONSTRAINT ds_kv_latest_unq_key UNIQUE (entity_type, entity_id, key)
);

CREATE TABLE IF NOT EXISTS widget_type (
    id varchar(31) NOT NULL CONSTRAINT widget_type_pkey PRIMARY KEY,
    alias varchar(255),
    bundle_alias varchar(255),
    descriptor varchar(1000000),
    name varchar(255),
    tenant_id varchar(31)
);

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id varchar(31) NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    alias varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    title varchar(255)
);

CREATE TABLE IF NOT EXISTS node_metric (
    id varchar (31) NOT NULL CONSTRAINT node_metric_pkey PRIMARY KEY,
    host varchar NOT NULL,
    port integer NOT NULL,
    status varchar,
    rpc_session_count integer DEFAULT 0,
    device_session_count integer DEFAULT 0,
    CONSTRAINT unq_node UNIQUE (host, port)
);

CREATE TABLE IF NOT EXISTS computations (
    id varchar(31) NOT NULL CONSTRAINT computations_pkey PRIMARY KEY,
    search_text varchar,
    computation_name varchar,
    type varchar,
    tenant_id varchar(31)
);


CREATE TABLE IF NOT EXISTS computation_job (
    id varchar(31) NOT NULL CONSTRAINT computation_job_pkey PRIMARY KEY,
    job_name varchar,
    search_text varchar,
    computation_id varchar,
    job_configuration varchar,
    state varchar(255),
    tenant_id varchar(31)
);

CREATE TABLE IF NOT EXISTS theme (
    id varchar (31) NOT NULL CONSTRAINT theme_pkey PRIMARY KEY,
    name varchar,
    value varchar,
    is_enabled boolean
 );

CREATE TABLE IF NOT EXISTS logo (
    id varchar (31) NOT NULL CONSTRAINT logo_pkey PRIMARY KEY,
    name varchar,
    enabled boolean,
    file bytea
 );

 CREATE TABLE IF NOT EXISTS data_model (
   id varchar (31) NOT NULL CONSTRAINT data_model_pkey PRIMARY KEY,
   name varchar(255),
   tenant_id varchar(31),
   search_text varchar(255),
   additional_info varchar,
   last_update_ts bigint
);

CREATE TABLE IF NOT EXISTS data_model_object (
    id varchar(31) NOT NULL CONSTRAINT data_model_object_pkey PRIMARY KEY,
    name varchar(250),
    description varchar,
    data_model_id varchar(31),
    parent_id varchar(31),
    type varchar(250),
    customer_id varchar(31),
    search_text varchar(255),
    logo_file VARCHAR
);

CREATE TABLE IF NOT EXISTS attribute_definition (
    name varchar(250),
    attr_value varchar,
    value_type varchar(100),
    data_model_object_id varchar(31),
    source varchar(250),
    key_attribute boolean,
    CONSTRAINT attr_def_unq_key UNIQUE (name, data_model_object_id)
);

CREATE TABLE IF NOT EXISTS asset_landing_info (
    id varchar(31) NOT NULL CONSTRAINT asset_landing_info_pkey PRIMARY KEY,
    data_model_id varchar(31),
    data_model_object_id varchar(31)
);

CREATE TABLE IF NOT EXISTS installed_schema_versions(executed_scripts varchar(255) UNIQUE);
CREATE TABLE IF NOT EXISTS customer_group (
    id varchar(31) NOT NULL CONSTRAINT customer_group_pkey PRIMARY KEY,
    title varchar(255),
    tenant_id varchar(31),
    customer_id varchar(31),
    additional_info varchar,
    search_text varchar(255)
);

CREATE TABLE IF NOT EXISTS user_groups (
    user_id varchar(31),
    group_id varchar(31)
);

CREATE TABLE IF NOT EXISTS customer_group_policy (
    group_id varchar(31),
    policy varchar
);

CREATE TABLE IF NOT EXISTS tempus_gateway_configuration (
    id varchar(31) NOT NULL CONSTRAINT tempus_gateway_configuration_pkey PRIMARY KEY,
    tenant_id varchar(31),
    replicas integer NOT NULL,
    gateway_token varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS spark_computation_meta_data (
    id varchar(31) NOT NULL CONSTRAINT spark_computations_pkey PRIMARY KEY,
    json_descriptor varchar,
    jar_name varchar,
    jar_path varchar,
    main_class varchar,
    args_format varchar,
    args_type varchar
);

CREATE TABLE IF NOT EXISTS kubeless_computation_meta_data (
    id varchar(31) NOT NULL CONSTRAINT kubeless_computations_pkey PRIMARY KEY,
    namespace varchar,
    function varchar,
    handler varchar,
    runtime varchar,
    dependencies varchar,
    func_type varchar,
    timeout varchar,
    checksum varchar
);

CREATE TABLE IF NOT EXISTS tenant_unit_system (
    tenant_id varchar(31) NOT NULL CONSTRAINT tenant_unit_system_pkey PRIMARY KEY,
    unit_system varchar
);

CREATE TABLE IF NOT EXISTS templates (
    id varchar(31) NOT NULL CONSTRAINT templates_pkey PRIMARY KEY,
    name varchar,
    body varchar
);

CREATE TABLE IF NOT EXISTS lambda_computation_meta_data (
    id varchar(31) NOT NULL CONSTRAINT lambda_computations_pkey PRIMARY KEY,
    file_path varchar,
    function_name varchar,
    function_handler varchar,
    runtime varchar,
    description varchar,
    lambda_timeout integer,
    memory_size integer,
    region varchar
);