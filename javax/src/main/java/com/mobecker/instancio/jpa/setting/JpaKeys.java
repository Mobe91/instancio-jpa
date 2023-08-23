/*
 * Copyright 2023 - 2023 Moritz Becker.
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
import org.instancio.internal.settings.InternalKey;
import org.instancio.settings.SettingKey;
import org.instancio.settings.Settings;

/**
 * Defines all keys supported by instancio-jpa.
 *
 * @see SettingKey
 * @see Settings
 * @since 1.0.0
 */
public final class JpaKeys {

    private static final List<SettingKey<Object>> ALL_KEYS = new ArrayList<>();

    /**
     * A reference to the JPA metamodel.
     * default is null; property name {@code jpaMetamodel}.
     */
    public static final SettingKey<Metamodel> METAMODEL = register(
        null, Metamodel.class, null, false);

    /**
     * Specifies whether instancio-jpa should apply the nullability information provided by the JPA metamodel to
     * the Instancio model used for generating object instances.
     * default is true; property name {@code jpa.useNullability}.
     */
    public static final SettingKey<Boolean> USE_JPA_NULLABILITY = register(
        "jpa.useNullability", Boolean.class, Boolean.TRUE, false);

    /**
     * Specifies whether instancio-jpa provided
     * {@link org.instancio.spi.InstancioServiceProvider.GeneratorProvider}s should apply. This setting will be
     * active for Instancio models created via
     * {@link com.mobecker.instancio.jpa.InstancioJpa#jpaModel(Class, Metamodel)} unless overriding settings are
     * supplied.
     * default is false; property name {@code jpa.enableGeneratorProviders}.
     */
    public static final SettingKey<Boolean> ENABLE_GENERATOR_PROVIDERS = register(
        "jpa.enableGeneratorProviders", Boolean.class, true, false);

    /**
     * A list of fully qualified Java type names with an optional field part to represent nodes that should not
     * be handled by the built-in generators provided by instancio-jpa. This is useful if you provide your own
     * {@link org.instancio.spi.InstancioServiceProvider.GeneratorProvider} to generate values for certain nodes.
     * Since Instancio does currently not support ordering of
     * {@link org.instancio.spi.InstancioServiceProvider.GeneratorProvider}s you need to exclude these nodes from
     * being handled by the instancio-jpa provided
     * {@link org.instancio.spi.InstancioServiceProvider.GeneratorProvider}.
     * default is null; property name {@code jpa.generatorProviderExclusions}.
     */
    public static final SettingKey<String> GENERATOR_PROVIDER_EXCLUSIONS = register(
        "jpa.generatorProviderExclusions", String.class, null, true);

    /**
     * Get a list of all JpaKeys.
     *
     * @return a list of all keys defined in {@link JpaKeys}.
     */
    public static List<SettingKey<Object>> all() {
        return Collections.unmodifiableList(ALL_KEYS);
    }

    /**
     * Get an instance of {@link Settings} populated with the provided JPA metamodel as {@link JpaKeys#METAMODEL}
     * and the remaining keys set to their default values.
     *
     * @param metamodel JPA metamodel
     * @return a {@link Settings} instance populated with the provided JPA metamodel and all keys defined in
     *     {@link JpaKeys} set to their default values
     */
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

    /**
     * Returns new settings with only the instancio-jpa specific setting keys contained in the provided settings.
     *
     * @param settings the settings to filter
     * @return a new settings instance with the filtered setting keys
     */
    public static Settings filterJpaKeys(Settings settings) {
        Settings filteredSettings = Settings.create();
        for (SettingKey<Object> key : all()) {
            Object value = settings.get(key);
            if (value != null) {
                filteredSettings.set(key, value);
            }
        }
        return filteredSettings;
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
