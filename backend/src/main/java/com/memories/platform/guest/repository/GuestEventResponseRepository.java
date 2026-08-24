package com.memories.platform.guest.repository;

import com.memories.platform.guest.entity.GuestEventResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuestEventResponseRepository extends JpaRepository<GuestEventResponse, UUID> {

    List<GuestEventResponse> findAllByGuestIdAndEventIdIn(
            UUID guestId,
            Collection<UUID> eventIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select response
            from GuestEventResponse response
            where response.guestId = :guestId
              and response.eventId = :eventId
            """)
    Optional<GuestEventResponse> findForUpdateByGuestIdAndEventId(
            @Param("guestId") UUID guestId,
            @Param("eventId") UUID eventId
    );
}
