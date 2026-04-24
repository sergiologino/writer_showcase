package io.altacod.publisher.user;

import io.altacod.publisher.media.MediaAssetEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "oauth_provider", length = 32)
    private String oauthProvider;

    @Column(name = "oauth_subject", length = 255)
    private String oauthSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_media_asset_id")
    private MediaAssetEntity avatarMedia;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String locale;
    private String timezone;
    private String theme;

    @Column(name = "is_admin", nullable = false)
    private boolean admin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public UserEntity(
            String email,
            String passwordHash,
            String displayName,
            String locale,
            String timezone,
            String theme,
            Instant now
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.locale = locale;
        this.timezone = timezone;
        this.theme = theme;
        this.admin = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public String getOauthSubject() {
        return oauthSubject;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public void setOauthSubject(String oauthSubject) {
        this.oauthSubject = oauthSubject;
    }

    public MediaAssetEntity getAvatarMedia() {
        return avatarMedia;
    }

    public void setAvatarMedia(MediaAssetEntity avatarMedia) {
        this.avatarMedia = avatarMedia;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLocale() {
        return locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getTheme() {
        return theme;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
