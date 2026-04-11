package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Параметры сети для исходящих HTTP-вызовов (Telegram, ВК и т.д.).
 * Браузер на Windows часто ходит через системный прокси; JVM по умолчанию — нет.
 */
@ConfigurationProperties(prefix = "publisher.http")
public class PublisherOutboundHttpProperties {

    /**
     * Установить {@code java.net.useSystemProxies=true} до создания {@link org.springframework.web.client.RestClient}.
     */
    private boolean useSystemProxies = true;

    /**
     * При проблемах с IPv6-маршрутом: {@code java.net.preferIPv4Stack=true}.
     */
    private boolean preferIpv4 = false;

    public boolean isUseSystemProxies() {
        return useSystemProxies;
    }

    public void setUseSystemProxies(boolean useSystemProxies) {
        this.useSystemProxies = useSystemProxies;
    }

    public boolean isPreferIpv4() {
        return preferIpv4;
    }

    public void setPreferIpv4(boolean preferIpv4) {
        this.preferIpv4 = preferIpv4;
    }

    /**
     * Вызывать один раз до любых исходящих запросов через JDK HttpClient.
     */
    public void applyJvmNetworkingProperties() {
        if (useSystemProxies) {
            System.setProperty("java.net.useSystemProxies", "true");
        }
        if (preferIpv4) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
    }
}
