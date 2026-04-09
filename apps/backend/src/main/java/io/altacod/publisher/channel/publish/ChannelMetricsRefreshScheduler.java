package io.altacod.publisher.channel.publish;

import io.altacod.publisher.channel.ChannelDeliveryStatus;
import io.altacod.publisher.channel.ChannelOutboundLogEntity;
import io.altacod.publisher.channel.ChannelOutboundLogRepository;
import io.altacod.publisher.channel.ChannelType;
import io.altacod.publisher.channel.WorkspaceChannelEntity;
import io.altacod.publisher.channel.WorkspaceChannelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Периодически подтягивает метрики (лайки, репосты, просмотры) для постов ВК; ОК — при наличии ответа API.
 */
@Component
public class ChannelMetricsRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChannelMetricsRefreshScheduler.class);

    private final ChannelOutboundLogRepository outboundLogRepository;
    private final WorkspaceChannelRepository channelRepository;
    private final ChannelDispatchService dispatchService;
    private final ObjectMapper objectMapper;

    public ChannelMetricsRefreshScheduler(
            ChannelOutboundLogRepository outboundLogRepository,
            WorkspaceChannelRepository channelRepository,
            ChannelDispatchService dispatchService,
            ObjectMapper objectMapper
    ) {
        this.outboundLogRepository = outboundLogRepository;
        this.channelRepository = channelRepository;
        this.dispatchService = dispatchService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${publisher.channels.metrics-poll-ms:120000}")
    @Transactional
    public void tick() {
        Instant staleBefore = Instant.now().minus(Duration.ofMinutes(20));
        List<ChannelOutboundLogEntity> due = outboundLogRepository.findDueForMetrics(
                ChannelDeliveryStatus.SENT,
                List.of(ChannelType.VK, ChannelType.ODNOKLASSNIKI),
                staleBefore,
                PageRequest.of(0, 30)
        );
        for (ChannelOutboundLogEntity row : due) {
            try {
                long wsId = row.getPost().getWorkspace().getId();
                if (row.getChannelType() == ChannelType.VK) {
                    WorkspaceChannelEntity ch = channelRepository
                            .findByWorkspaceIdAndChannelType(wsId, ChannelType.VK)
                            .orElse(null);
                    if (ch == null || !ch.isEnabled()) {
                        continue;
                    }
                    JsonNode cfg = objectMapper.readTree(ch.getConfigJson());
                    String token = cfg.has("accessToken") && !cfg.get("accessToken").isNull()
                            ? cfg.get("accessToken").asText()
                            : null;
                    dispatchService.refreshVkMetrics(row, token);
                }
                // ОК: метрики темы требуют отдельных методов и прав; оставляем место для расширения.
            } catch (Exception e) {
                log.debug("Metrics refresh skipped for log {}: {}", row.getId(), e.getMessage());
            }
        }
    }
}
