package com.move.communitynotificationservice.config;

import com.move.communitynotificationservice.model.ContentMeta;
import com.move.communitynotificationservice.repository.ContentMetaRepository;
import com.move.communitynotificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ContentEventListener {

    private final ContentMetaRepository contentMetaRepository;
    private final NotificationService notificationService;

    public static final String CONTENT_CREATED_QUEUE = "content.created.queue";

    public ContentEventListener(ContentMetaRepository contentMetaRepository,
                                NotificationService notificationService) {
        this.contentMetaRepository = contentMetaRepository;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = CONTENT_CREATED_QUEUE)
    public void handleContentCreated(Map<String, Object> event) {
        try {
            String contentId = (String) event.get("contentId");
            String userId = (String) event.get("userId");
            String title = (String) event.get("title");

            if (contentId == null || userId == null) {
                System.err.println("❌ Champs manquants dans l’événement content.created: " + event);
                return;
            }

            ContentMeta meta = new ContentMeta();
            meta.setId(contentId);
            meta.setUserId(userId);
            meta.setTitle(title);

            contentMetaRepository.save(meta);
            System.out.println("✅ Content metadata saved: " + meta);

            // 👇 Envoi de notification automatique
            notificationService.createNotification(
                    userId,
                    "Votre contenu \"" + title + "\" a été bien enregistré !"
            );

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de content.created: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
