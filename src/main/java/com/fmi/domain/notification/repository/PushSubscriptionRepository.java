package com.fmi.domain.notification.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.data.PushSubscription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUser(User user);

    List<PushSubscription> findByUserIn(List<User> users);

    boolean existsByUserAndEndpoint(User user, String endpoint);

    void deleteByUserAndEndpoint(User user, String endpoint);
}
