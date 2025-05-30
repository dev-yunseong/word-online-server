package com.wordonline.server.matching.component;

import com.wordonline.server.game.component.SessionManager;
import com.wordonline.server.game.domain.SessionObject;
import com.wordonline.server.matching.dto.MatchedInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

@Slf4j
@Component
public class MatchingManager {

    private final Queue<String> matchingQueue = new LinkedList<>();
    private static int sessionIdCounter = 0;

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private SessionManager sessionManager;

    public void enqueue(String userId) {
        matchingQueue.add(userId);

    }

    public boolean tryMatchUsers() {
        if (matchingQueue.size() < 2)
            return false;

        sessionIdCounter++;

        String uid1 = matchingQueue.poll();
        String uid2 = matchingQueue.poll();
        String sessionId = "session-" + sessionIdCounter;
        MatchedInfoDto matchedInfoDto = new MatchedInfoDto(
                "Successfully Matched",
                uid1,
                uid2,
                sessionId
        );
        template.convertAndSend(
                String.format("/queue/match-status/%s", uid1),
                matchedInfoDto);
        template.convertAndSend(
                String.format("/queue/match-status/%s", uid2),
                matchedInfoDto);

        log.info("matched {} and {}", uid1, uid2);
        try {
            Thread.sleep(2000);
            sessionManager.createSession(new SessionObject(sessionId, uid1, uid2, template));
            return true;
        } catch (InterruptedException e) {
            log.error("Error while creating session", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
