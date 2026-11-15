package com.iitsm.notification.service;

import com.iitsm.notification.dto.NotificationResponse;
import com.iitsm.notification.repository.NotificationRepository;
import com.iitsm.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications; private final UserRepository users;
    public NotificationService(NotificationRepository notifications, UserRepository users) { this.notifications=notifications; this.users=users; }
    @Transactional(readOnly=true) public List<NotificationResponse> list(String email) {
        Long id=users.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
        return notifications.findByRecipientIdOrderByCreatedAtDesc(id).stream().map(NotificationResponse::from).toList();
    }
    @Transactional public NotificationResponse markRead(String email, Long id) {
        Long userId=users.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
        var n=notifications.findByIdAndRecipientId(id,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));
        n.setRead(true); return NotificationResponse.from(n);
    }
}
