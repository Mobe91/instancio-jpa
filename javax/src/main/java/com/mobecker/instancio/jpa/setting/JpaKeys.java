/*
 * Copyright 2022-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mobecker.instancio.jpa.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.metamodel.Metamodel;
import org.instancio.documentation.ExperimentalApi;
import org.instancio.internal.settings.InternalKey;
import org.instancio.settings.SettingKey;
import org.instancio.settings.Settings;

public final class JpaKeys {

    private static final List<SettingKey<Object>> ALL_KEYS = new ArrayList<>();

    @ExperimentalApi
    public static final SettingKey<Metamodel> METAMODEL = register(
        "jpaMetamodel", Metamodel.class, null, false);
    @ExperimentalApi
    public static final SettingKey<Boolean> USE_JPA_NULLABILITY = register(
        "useJpaNullability", Boolean.class, Boolean.TRUE, false);

    public static List<SettingKey<Object>> all() {
        return Collections.unmodifiableList(ALL_KEYS);
    }

    public static Settings defaults(Metamodel metamodel) {
        Settings settings = Settings.create();
        settings.set(METAMODEL, metamodel);
        for (SettingKey<Object> setting : all()) {
            if (settings.get(setting) == null) {
                settings.set(setting, setting.defaultValue());
            }
        }
        return settings;
    }

    private static <T> SettingKey<T> register(
        final String propertyKey,
        final Class<T> type,
        @javax.annotation.Nullable final Object defaultValue,
        final boolean allowsNullValue) {

        final SettingKey<T> settingKey = new InternalKey<>(
            propertyKey, type, defaultValue, null, allowsNullValue);

        ALL_KEYS.add((SettingKey<Object>) settingKey);
        return settingKey;
    }

    private JpaKeys() { }
}
