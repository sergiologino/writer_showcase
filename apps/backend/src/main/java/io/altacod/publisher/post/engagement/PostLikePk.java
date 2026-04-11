package io.altacod.publisher.post.engagement;

import java.io.Serializable;
import java.util.Objects;

public class PostLikePk implements Serializable {

    private Long postId;
    private Long userId;

    public PostLikePk() {
    }

    public PostLikePk(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostLikePk postLikePk = (PostLikePk) o;
        return Objects.equals(postId, postLikePk.postId) && Objects.equals(userId, postLikePk.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, userId);
    }
}
