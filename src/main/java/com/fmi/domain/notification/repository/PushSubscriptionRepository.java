package com.fmi.domain.notification.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.data.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUser(User user);

    List<PushSubscription> findByUserIn(List<User> users);

    void deleteByUserAndEndpoint(User user, String endpoint);
}
