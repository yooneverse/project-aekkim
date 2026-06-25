package com.ssafy.e106.domain.auth.entity;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.auth.enums.AuthProvider;
import com.ssafy.e106.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
    @Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_users_provider_provider_user_id",
                columnNames = {"provider", "providerUserId"}
            )
        }
    )
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'GOOGLE'")
    private AuthProvider provider;

    @Column(nullable = false, length = 120)
    private String providerUserId;

    @Column(name = "finance_user_key", length = 100, unique = true)
    private String financeUserKey;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String displayName;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private Boolean checkinAlertEnabled;

    @Column(nullable = false)
    private Boolean promoAlertEnabled;

    @Column(nullable = false)
    private Boolean optionalConsentAgreed;

    @Column(nullable = false)
    private LocalDateTime connectedAt;

    private LocalDateTime lastSyncedAt;

    private LocalDateTime lastLoginAt;

    @Builder
    public User(
        AuthProvider provider,
        String providerUserId,
        String financeUserKey,
        String email,
        String displayName,
        String profileImageUrl,
        Boolean checkinAlertEnabled,
        Boolean promoAlertEnabled,
        Boolean optionalConsentAgreed,
        LocalDateTime connectedAt,
        LocalDateTime lastSyncedAt,
        LocalDateTime lastLoginAt
    ) {
        this.provider = provider == null ? AuthProvider.GOOGLE : provider;
        this.providerUserId = providerUserId;
        this.financeUserKey = financeUserKey;
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.checkinAlertEnabled = checkinAlertEnabled;
        this.promoAlertEnabled = promoAlertEnabled;
        this.optionalConsentAgreed = optionalConsentAgreed;
        this.connectedAt = connectedAt;
        this.lastSyncedAt = lastSyncedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public void updateOAuthProfile(
        String email,
        String displayName,
        String profileImageUrl,
        LocalDateTime lastLoginAt
    ) {
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.lastLoginAt = lastLoginAt;
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void updateLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public void updateFinanceUserKey(String financeUserKey) {
        this.financeUserKey = financeUserKey;
    }

    public void updateAlertSetting(Boolean checkinAlertEnabled, Boolean promoAlertEnabled) {
        this.checkinAlertEnabled = checkinAlertEnabled;
        this.promoAlertEnabled = promoAlertEnabled;
    }

    public void updateOptionalConsent(Boolean optionalConsentAgreed) {
        this.optionalConsentAgreed = optionalConsentAgreed;
    }
}
