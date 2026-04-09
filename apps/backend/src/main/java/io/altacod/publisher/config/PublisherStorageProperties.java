package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "publisher.storage")
public class PublisherStorageProperties {

    /**
     * Каталог для файлов (относительный к рабочей директории процесса или абсолютный).
     */
    private String localRoot = "storage";

    /**
     * Максимальный размер загрузки (байт).
     */
    private long maxUploadBytes = 52_428_800L;

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }
}
