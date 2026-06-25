package com.fashion.auth.repository;

import com.fashion.auth.model.CommentReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReplyRepository extends JpaRepository<CommentReply, String> {

  
    @Query("SELECT cr FROM CommentReply cr WHERE cr.comment.id = :commentId AND cr.shop.id = :shopId")
    Optional<CommentReply> findByCommentIdAndShopId(@Param("commentId") String commentId, @Param("shopId") String shopId);

   
    @Query("SELECT cr FROM CommentReply cr WHERE cr.comment.id = :commentId ORDER BY cr.createdAt ASC")
    List<CommentReply> findByCommentIdOrderByCreatedAtAsc(@Param("commentId") String commentId);

    
    @Query("SELECT cr FROM CommentReply cr WHERE cr.shop.id = :shopId ORDER BY cr.createdAt DESC")
    List<CommentReply> findByShopIdOrderByCreatedAtDesc(@Param("shopId") String shopId);

   
    @Query("SELECT COUNT(cr) FROM CommentReply cr WHERE cr.comment.id = :commentId")
    long countByCommentId(@Param("commentId") String commentId);

   
    @Query("SELECT COUNT(cr) > 0 FROM CommentReply cr WHERE cr.comment.id = :commentId AND cr.shop.id = :shopId")
    boolean existsByCommentIdAndShopId(@Param("commentId") String commentId, @Param("shopId") String shopId);
}