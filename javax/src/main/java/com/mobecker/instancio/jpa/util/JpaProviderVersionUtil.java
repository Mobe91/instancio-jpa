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
