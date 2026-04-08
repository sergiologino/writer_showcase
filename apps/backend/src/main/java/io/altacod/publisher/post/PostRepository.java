package io.altacod.publisher.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    Optional<PostEntity> findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Query("""
            select p from PostEntity p
            where p.workspace.id = :workspaceId
            and (:status is null or p.status = :status)
            and (:q is null or lower(p.title) like lower(concat('%', :q, '%'))
                      or lower(coalesce(p.excerpt, '')) like lower(concat('%', :q, '%')))
            """)
    Page<PostEntity> findWorkspaceFeed(
            @Param("workspaceId") Long workspaceId,
            @Param("status") PostStatus status,
            @Param("q") String q,
            Pageable pageable
    );

    boolean existsByWorkspaceIdAndSlug(Long workspaceId, String slug);

    Optional<PostEntity> findByWorkspaceIdAndSlug(Long workspaceId, String slug);

    @Query("""
            select p from PostEntity p
            join p.workspace w
            where w.slug = :workspaceSlug
            and p.visibility = io.altacod.publisher.post.PostVisibility.PUBLIC
            and p.status = io.altacod.publisher.post.PostStatus.PUBLISHED
            order by coalesce(p.publishedAt, p.createdAt) desc
            """)
    Page<PostEntity> findPublicByWorkspaceSlug(@Param("workspaceSlug") String workspaceSlug, Pageable pageable);

    @Query("""
            select p from PostEntity p
            join p.workspace w
            where p.slug = :slug and w.slug = :workspaceSlug
            and p.visibility = io.altacod.publisher.post.PostVisibility.PUBLIC
            and p.status = io.altacod.publisher.post.PostStatus.PUBLISHED
            """)
    Optional<PostEntity> findPublishedPublic(
            @Param("slug") String slug,
            @Param("workspaceSlug") String workspaceSlug
    );
}
