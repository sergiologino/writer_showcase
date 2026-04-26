package io.altacod.publisher.channel;

public enum ChannelType {
    TELEGRAM,
    VK,
    /** Одноклассники (OK.ru) */
    ODNOKLASSNIKI,
    /** Мессенджер МАКС (MAX Bot API, platform-api.max.ru) */
    MAX,
    /**
     * Страница Facebook; исходящие запросы идут через noteapp-ai-integration
     * ({@code POST /api/social/posts}, platform {@code facebook}).
     */
    FACEBOOK,
    /**
     * X (Twitter) API v2; исходящие запросы через noteapp-ai-integration ({@code platform x}).
     */
    X
}
