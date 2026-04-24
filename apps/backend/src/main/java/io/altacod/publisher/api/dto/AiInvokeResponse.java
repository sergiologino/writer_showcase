package io.altacod.publisher.api.dto;

/**
 * @param output Текст для отображения (после разбора JSON интеграции).
 * @param tokensUsed Токены последнего вызова (из ответа интеграции), при ошибке null.
 * @param postTokensTotal Актуальная накопленная сумма по статье, если в запросе был {@code postId}.
 */
public record AiInvokeResponse(
        boolean ok,
        String output,
        String errorCode,
        Integer tokensUsed,
        Long postTokensTotal
) {
    public static AiInvokeResponse ofFailure(String output, String errorCode) {
        return new AiInvokeResponse(false, output, errorCode, null, null);
    }

    public static AiInvokeResponse ofSuccess(String output, Integer tokensUsed, Long postTokensTotal) {
        return new AiInvokeResponse(true, output, null, tokensUsed, postTokensTotal);
    }
}
