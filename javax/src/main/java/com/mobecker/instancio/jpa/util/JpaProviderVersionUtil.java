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

package com.mobecker.instancio.jpa.util;

public final class JpaProviderVersionUtil {

    private static final String HIBERNATE_VERSION_CLASS_NAME = "org.hibernate.Version";

    private JpaProviderVersionUtil() { }

    public static boolean isHibernate5OrOlder() {
        return isHibernate() && getHibernateMajorVersion() <= 5;
    }

    public static boolean isHibernate6OrNewer() {
        return isHibernate() && getHibernateMajorVersion() >= 6;
    }

    private static boolean isHibernate() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(HIBERNATE_VERSION_CLASS_NAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static int getHibernateMajorVersion() {
        String version = org.hibernate.Version.getVersionString();
        String[] versionParts = version.split("[\\.-]");
        return Integer.parseInt(versionParts[0]);
    }
}
