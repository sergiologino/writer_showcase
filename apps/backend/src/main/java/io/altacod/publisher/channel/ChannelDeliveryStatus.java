package io.altacod.publisher.channel;

public enum ChannelDeliveryStatus {
    /** Выбран для публикации, запись в логе ещё не создана или публикация не запускалась. */
    PENDING,
    SENT,
    FAILED,
    /** Отклонено модерацией площадки (не повторяем автоматически). */
    REJECTED
}
