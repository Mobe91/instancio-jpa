/*
 * Copyright 2023 - 2024 Moritz Becker.
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
package com.mobecker.instancio.jpa.testsuite;

import static org.assertj.core.api.Assertions.assertThat;

import com.mobecker.instancio.jpa.setting.JpaKeys;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import org.instancio.internal.context.PropertiesLoader;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Test;

public class PropertiesLoaderTest {

    @Test
    void instancioJpaProperties() {
        // When
        Settings settings = loadSettingsFromFile("instancio-test.properties");
        boolean useJpaNullability = settings.get(JpaKeys.USE_JPA_NULLABILITY);
        String generatorProviderExclusions = settings.get(JpaKeys.GENERATOR_PROVIDER_EXCLUSIONS);

        // Then
        assertThat(useJpaNullability).isFalse();
        assertThat(generatorProviderExclusions).isEqualTo("test");
    }

    private static Settings loadSettingsFromFile(String file) {
        try {
            Method loadMethod = PropertiesLoader.class.getDeclaredMethod("load", String.class);
            loadMethod.setAccessible(true);
            return Settings.from((Properties) loadMethod.invoke(null, file));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
