package io.altacod.publisher.post.engagement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostCommentEntity, Long> {

    long countByPost_Id(Long postId);

    Page<PostCommentEntity> findByPost_IdOrderByCreatedAtDesc(Long postId, Pageable pageable);
}
