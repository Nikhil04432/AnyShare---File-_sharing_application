package com.nikworkspace.AnyShare.repository;

import com.nikworkspace.AnyShare.entity.SessionEntity;
import com.nikworkspace.AnyShare.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {

    Optional<SessionEntity> findByRoomCode(String roomCode);

    @Query("SELECT s FROM SessionEntity s WHERE s.expiresAt < :now AND s.status != 'CLOSED'")
    List<SessionEntity> findExpiredSessions(LocalDateTime now);

    List<SessionEntity> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);
}