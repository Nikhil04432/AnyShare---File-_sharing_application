package com.nikworkspace.AnyShare.repository;

import com.nikworkspace.AnyShare.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    List<Transfer> findBySessionIdOrderByCompletedAtDesc(UUID sessionId);

    @Query("SELECT t FROM Transfer t WHERE t.sender.id = :userId OR t.receiver.id = :userId ORDER BY t.completedAt DESC")
    List<Transfer> findByUserIdOrderByCompletedAtDesc(UUID userId);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.sender.id = :userId")
    Long countBySenderId(UUID userId);

    @Query("SELECT SUM(t.fileSize) FROM Transfer t WHERE t.sender.id = :userId")
    Long sumFileSizeBySenderId(UUID userId);
}
