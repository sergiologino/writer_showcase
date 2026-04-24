package io.altacod.publisher.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_ai_routing")
public class AppAiRoutingEntity {

    @Id
    private Long id = 1L;

    @Column(name = "routing_json", nullable = false, columnDefinition = "TEXT")
    private String routingJson = "{}";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppAiRoutingEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getRoutingJson() {
        return routingJson;
    }

    public void setRoutingJson(String routingJson) {
        this.routingJson = routingJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
