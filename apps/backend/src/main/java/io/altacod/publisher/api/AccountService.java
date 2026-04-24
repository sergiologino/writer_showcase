package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.AddProfilePhotoRequest;
import io.altacod.publisher.api.dto.MeResponse;
import io.altacod.publisher.api.dto.ProfilePhotoDto;
import io.altacod.publisher.api.dto.SetAvatarRequest;
import io.altacod.publisher.api.dto.UpdateProfilePayload;
import io.altacod.publisher.api.dto.UserSummaryDto;
import io.altacod.publisher.api.dto.WorkspaceSummaryDto;
import io.altacod.publisher.config.PublisherSecurityProperties;
import io.altacod.publisher.media.MediaAssetRepository;
import io.altacod.publisher.media.MediaType;
import io.altacod.publisher.security.SecurityUserPrincipal;
import io.altacod.publisher.user.UserEntity;
import io.altacod.publisher.user.UserProfileMediaEntity;
import io.altacod.publisher.user.UserProfileMediaRepository;
import io.altacod.publisher.user.UserRepository;
import io.altacod.publisher.workspace.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class AccountService {

    private static final int MAX_PROFILE_PHOTOS = 40;

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PublisherSecurityProperties securityProperties;
    private final UserProfileMediaRepository userProfileMediaRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public AccountService(
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            PublisherSecurityProperties securityProperties,
            UserProfileMediaRepository userProfileMediaRepository,
            MediaAssetRepository mediaAssetRepository
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.securityProperties = securityProperties;
        this.userProfileMediaRepository = userProfileMediaRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        var user = requireCurrentUser();
        var memberships = membershipRepository.findByUserOrderByIdAsc(user);
        List<WorkspaceSummaryDto> workspaces = memberships.stream()
                .map(m -> new WorkspaceSummaryDto(
                        m.getWorkspace().getId(),
                        m.getWorkspace().getName(),
                        m.getWorkspace().getSlug(),
                        m.getRole()
                ))
                .toList();
        var userDto = toUserSummary(user, workspaces);
        return new MeResponse(userDto, workspaces);
    }

    @Transactional
    public MeResponse updateProfile(UpdateProfilePayload payload) {
        var user = requireCurrentUser();
        user.setDisplayName(payload.displayName().trim());
        user.setLocale(blankToNull(payload.locale()));
        user.setTimezone(blankToNull(payload.timezone()));
        user.setTheme(payload.theme());
        user.touch(Instant.now());
        userRepository.save(user);
        return me();
    }

    @Transactional
    public MeResponse setAvatar(long workspaceId, SetAvatarRequest request) {
        var user = requireCurrentUser();
        if (!membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this workspace");
        }
        if (request.mediaAssetId() == null) {
            user.setAvatarMedia(null);
        } else {
            var media = mediaAssetRepository.findByIdAndWorkspaceId(request.mediaAssetId(), workspaceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
            if (media.getType() != MediaType.IMAGE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar must be an image");
            }
            user.setAvatarMedia(media);
        }
        user.touch(Instant.now());
        userRepository.save(user);
        return me();
    }

    @Transactional
    public MeResponse addProfilePhoto(long workspaceId, AddProfilePhotoRequest request) {
        var user = requireCurrentUser();
        if (!membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this workspace");
        }
        if (userProfileMediaRepository.countByUser_Id(user.getId()) >= MAX_PROFILE_PHOTOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile photo limit reached");
        }
        var media = mediaAssetRepository.findByIdAndWorkspaceId(request.mediaAssetId(), workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        if (media.getType() != MediaType.IMAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only images can be added to the profile gallery");
        }
        if (userProfileMediaRepository.findByUser_IdAndMediaAsset_Id(user.getId(), media.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Photo already in gallery");
        }
        int next = userProfileMediaRepository.findMaxSortOrderByUserId(user.getId()) + 1;
        userProfileMediaRepository.save(new UserProfileMediaEntity(user, media, next, Instant.now()));
        return me();
    }

    @Transactional
    public MeResponse removeProfilePhoto(long workspaceId, long mediaAssetId) {
        var user = requireCurrentUser();
        if (!membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this workspace");
        }
        mediaAssetRepository.findByIdAndWorkspaceId(mediaAssetId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        var link = userProfileMediaRepository.findByUser_IdAndMediaAsset_Id(user.getId(), mediaAssetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not in gallery"));
        userProfileMediaRepository.delete(link);
        if (user.getAvatarMedia() != null && user.getAvatarMedia().getId().equals(mediaAssetId)) {
            user.setAvatarMedia(null);
        }
        user.touch(Instant.now());
        userRepository.save(user);
        return me();
    }

    private UserEntity requireCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private UserSummaryDto toUserSummary(UserEntity user, List<WorkspaceSummaryDto> workspaces) {
        boolean admin = user.isAdmin() || securityProperties.isListedAdmin(user.getEmail());
        String publicSlug = workspaces.isEmpty() ? null : workspaces.get(0).slug();
        String avatarUrl = null;
        if (publicSlug != null && user.getAvatarMedia() != null) {
            avatarUrl = publicMediaFileUrl(publicSlug, user.getAvatarMedia().getId());
        }
        var photoRows = userProfileMediaRepository.findByUserIdWithMediaOrderBySortOrder(user.getId());
        String slug = publicSlug;
        List<ProfilePhotoDto> photos = photoRows.stream()
                .map(upm -> {
                    var m = upm.getMediaAsset();
                    String url = slug != null ? publicMediaFileUrl(slug, m.getId()) : null;
                    return new ProfilePhotoDto(m.getId(), url != null ? url : "", m.getMimeType());
                })
                .toList();
        return new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getLocale(),
                user.getTimezone(),
                user.getTheme(),
                admin,
                avatarUrl,
                photos
        );
    }

    private static String publicMediaFileUrl(String workspaceSlug, long mediaId) {
        return "/api/public/w/" + workspaceSlug + "/media/" + mediaId + "/file";
    }
}
