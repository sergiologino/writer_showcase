package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.PublicCommentDto;
import io.altacod.publisher.api.dto.PublicEngagementDto;
import io.altacod.publisher.post.PostEntity;
import io.altacod.publisher.post.PostRepository;
import io.altacod.publisher.post.engagement.PostCommentEntity;
import io.altacod.publisher.post.engagement.PostCommentRepository;
import io.altacod.publisher.post.engagement.PostLikeEntity;
import io.altacod.publisher.post.engagement.PostLikeRepository;
import io.altacod.publisher.user.UserEntity;
import io.altacod.publisher.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PostEngagementService {

    private static final int MAX_COMMENTS = 200;

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final UserRepository userRepository;

    public PostEngagementService(
            PostRepository postRepository,
            PostLikeRepository postLikeRepository,
            PostCommentRepository postCommentRepository,
            UserRepository userRepository
    ) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCommentRepository = postCommentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PublicEngagementDto getEngagement(String workspaceSlug, String postSlug, Long userId) {
        PostEntity post = requirePublicPost(workspaceSlug, postSlug);
        long likes = postLikeRepository.countByPostId(post.getId());
        long comments = postCommentRepository.countByPost_Id(post.getId());
        boolean liked = userId != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), userId);
        var page = postCommentRepository.findByPost_IdOrderByCreatedAtDesc(
                post.getId(), PageRequest.of(0, MAX_COMMENTS));
        List<PostCommentEntity> rows = new ArrayList<>(page.getContent());
        Collections.reverse(rows);
        List<PublicCommentDto> dtos = rows.stream()
                .map(c -> new PublicCommentDto(
                        c.getId(),
                        c.getUser().getDisplayName(),
                        c.getBody(),
                        c.getCreatedAt()
                ))
                .toList();
        return new PublicEngagementDto(likes, comments, liked, dtos);
    }

    @Transactional
    public PublicEngagementDto toggleLike(String workspaceSlug, String postSlug, long userId) {
        PostEntity post = requirePublicPost(workspaceSlug, postSlug);
        if (postLikeRepository.existsByPostIdAndUserId(post.getId(), userId)) {
            postLikeRepository.deleteByPostIdAndUserId(post.getId(), userId);
        } else {
            postLikeRepository.save(new PostLikeEntity(post.getId(), userId, Instant.now()));
        }
        return getEngagement(workspaceSlug, postSlug, userId);
    }

    @Transactional
    public PublicEngagementDto addComment(String workspaceSlug, String postSlug, long userId, String body) {
        PostEntity post = requirePublicPost(workspaceSlug, postSlug);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty comment");
        }
        postCommentRepository.save(new PostCommentEntity(post, user, trimmed, Instant.now()));
        return getEngagement(workspaceSlug, postSlug, userId);
    }

    private PostEntity requirePublicPost(String workspaceSlug, String postSlug) {
        return postRepository.findPublishedPublic(postSlug, workspaceSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }
}
