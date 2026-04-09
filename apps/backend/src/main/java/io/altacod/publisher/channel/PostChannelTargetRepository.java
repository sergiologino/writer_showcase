package io.altacod.publisher.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostChannelTargetRepository extends JpaRepository<PostChannelTargetEntity, Long> {

    void deleteByPost_Id(Long postId);

    @Query("select t.channelType from PostChannelTargetEntity t where t.post.id = :postId")
    List<ChannelType> findChannelTypesByPostId(@Param("postId") Long postId);

    @Query("select t.post.id as postId, t.channelType as channelType from PostChannelTargetEntity t where t.post.id in :ids")
    List<PostChannelTargetRow> findRowsByPostIdIn(@Param("ids") Collection<Long> ids);

    interface PostChannelTargetRow {
        Long getPostId();

        ChannelType getChannelType();
    }
}
