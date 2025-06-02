package com.move.contentservice.service;

import com.move.contentservice.dto.MediaDTO;
import com.move.contentservice.exception.ResourceNotFoundException;
import com.move.contentservice.mapper.MediaMapper;
import com.move.contentservice.model.Content;
import com.move.contentservice.model.Media;
import com.move.contentservice.repository.ContentRepository;
import com.move.contentservice.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final ContentRepository contentRepository;
    private final MediaMapper mediaMapper;
    private final Path fileStorageLocation;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${app.media.base-url:}")
    private String mediaBaseUrl;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    public MediaService(MediaRepository mediaRepository, ContentRepository contentRepository, MediaMapper mediaMapper,
                        @Value("${file.upload-dir}") String uploadDir) {
        this.mediaRepository = mediaRepository;
        this.contentRepository = contentRepository;
        this.mediaMapper = mediaMapper;
        this.uploadDir = uploadDir;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Récupère un media par son ID
     */
    public MediaDTO getMediaById(String mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
        MediaDTO dto = mediaMapper.toDTO(media);

        // Ajouter l'URL d'accès au fichier pour le frontend
        enrichMediaWithUrl(dto);

        return dto;
    }

    /**
     * Récupère tous les médias associés à un contenu
     */
    public List<MediaDTO> getMediaByContentId(String contentId) {
        List<Media> mediaList = mediaRepository.findByContentId(contentId);
        List<MediaDTO> dtos = mediaList.stream().map(mediaMapper::toDTO).toList();

        // Ajouter les URLs d'accès aux fichiers pour le frontend
        dtos.forEach(this::enrichMediaWithUrl);

        return dtos;
    }

    /**
     * Récupère l'image de couverture d'un contenu
     */
    public MediaDTO getCoverByContentId(String contentId) {
        Media cover = mediaRepository.findByContentIdAndMediaType(contentId, "COVER")
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cover image not found for content id: " + contentId));

        MediaDTO dto = mediaMapper.toDTO(cover);
        enrichMediaWithUrl(dto);

        return dto;
    }

    /**
     * Récupère les médias d'un type spécifique (ALBUM, VIDEO) pour un contenu
     */
    public List<MediaDTO> getMediaByContentIdAndType(String contentId, String mediaType) {
        List<Media> mediaList = mediaRepository.findByContentIdAndMediaType(contentId, mediaType);
        List<MediaDTO> dtos = mediaList.stream().map(mediaMapper::toDTO).toList();

        // Ajouter les URLs d'accès aux fichiers pour le frontend
        dtos.forEach(this::enrichMediaWithUrl);

        return dtos;
    }

    /**
     * Récupère les photos (ALBUM) pour un contenu
     */
    public List<MediaDTO> getPhotosByContentId(String contentId) {
        return getMediaByContentIdAndType(contentId, "ALBUM");
    }

    /**
     * Récupère les vidéos (VIDEO) pour un contenu
     */
    public List<MediaDTO> getVideosByContentId(String contentId) {
        return getMediaByContentIdAndType(contentId, "VIDEO");
    }

    /**
     * Ajoute les URLs d'accès aux fichiers pour le frontend
     */
    private void enrichMediaWithUrl(MediaDTO mediaDTO) {
        // URL pour accéder au fichier par mediaId (recommandé)
        String fileUrl = getBaseUrl() + "/api/media/file/" + mediaDTO.getId();

        // URL alternative pour accéder au fichier par contentId/fileName
        String alternativeUrl = getBaseUrl() + "/api/media/files/" + mediaDTO.getContentId() + "/" + mediaDTO.getFileName();

        // URL pour accéder à la miniature si elle existe
        String thumbnailUrl = null;
        if (mediaDTO.getThumbnailName() != null && !mediaDTO.getThumbnailName().isEmpty()) {
            thumbnailUrl = getBaseUrl() + "/api/media/files/" + mediaDTO.getContentId() + "/" + mediaDTO.getThumbnailName();
        }

        mediaDTO.setFileUrl(fileUrl);
        mediaDTO.setAlternativeUrl(alternativeUrl);
        mediaDTO.setThumbnailUrl(thumbnailUrl);
    }

    /**
     * Détermine l'URL de base pour accéder aux fichiers
     */
    private String getBaseUrl() {
        if (mediaBaseUrl != null && !mediaBaseUrl.isEmpty()) {
            return mediaBaseUrl;
        }

        if (contextPath != null && !contextPath.isEmpty()) {
            return contextPath;
        }

        return "";
    }

    /**
     * Upload d'un média avec tous les paramètres
     */
    @Transactional
    public MediaDTO uploadMedia(
            String contentId,
            MultipartFile file,
            String title,
            String description,
            String mediaType,
            Integer displayOrder
    ) throws IOException {
        // Vérifier si le contenu existe
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + contentId));

        // Si c'est une couverture et qu'il en existe déjà une, la supprimer
        if ("COVER".equals(mediaType)) {
            List<Media> existingCovers = mediaRepository.findByContentIdAndMediaType(contentId, "COVER");
            if (!existingCovers.isEmpty()) {
                // Supprime l'ancienne couverture
                deleteMedia(existingCovers.get(0).getId());
            }
        }

        // Créer un dossier pour le contenu si nécessaire
        String contentDir = uploadDir + "/" + contentId;
        Path contentDirPath = Paths.get(contentDir);
        if (!Files.exists(contentDirPath)) {
            Files.createDirectories(contentDirPath);
        }

        // Générer un nom de fichier unique
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path targetLocation = contentDirPath.resolve(fileName);

        // Copier le fichier
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Déterminer si c'est une photo ou une vidéo en fonction du type MIME
        String fileType = file.getContentType();
        if (mediaType == null || mediaType.isEmpty()) {
            if (fileType != null && fileType.startsWith("video/")) {
                mediaType = "VIDEO";
            } else if (fileType != null && fileType.startsWith("image/")) {
                mediaType = "ALBUM";
            } else {
                mediaType = "ALBUM"; // Par défaut
            }
        }

        // Création du media
        Media media = Media.builder()
                .title(title)
                .description(description)
                .fileType(fileType)
                .fileSize(String.valueOf(file.getSize()))
                .uploadDate(new Date())
                .contentId(contentId)
                .fileName(fileName)
                .mediaType(mediaType)
                .displayOrder(displayOrder)
                .build();

        // Sauvegarder l'entité
        Media savedMedia = mediaRepository.save(media);

        // Ajouter le média à la liste des médias du contenu
        content.getMedia().add(savedMedia);
        contentRepository.save(content);

        MediaDTO dto = mediaMapper.toDTO(savedMedia);
        enrichMediaWithUrl(dto);

        return dto;
    }

    /**
     * Upload d'un média avec paramètres simplifiés
     */
    @Transactional
    public MediaDTO uploadMedia(String contentId, MultipartFile file, String title, String description) throws IOException {
        // Appel de la méthode complète avec des valeurs par défaut
        return uploadMedia(contentId, file, title, description, "ALBUM", null);
    }

    /**
     * Upload d'une image de couverture
     */
    @Transactional
    public MediaDTO uploadCoverImage(String contentId, MultipartFile file, String title, String description) throws IOException {
        // Appel avec le type COVER
        return uploadMedia(contentId, file, title, description, "COVER", 0);
    }

    /**
     * Upload d'une photo d'album
     */
    @Transactional
    public MediaDTO uploadAlbumPhoto(String contentId, MultipartFile file, String title, String description, Integer displayOrder) throws IOException {
        return uploadMedia(contentId, file, title, description, "ALBUM", displayOrder);
    }

    /**
     * Upload d'une vidéo
     */
    @Transactional
    public MediaDTO uploadVideo(String contentId, MultipartFile file, String title, String description, Integer displayOrder) throws IOException {
        return uploadMedia(contentId, file, title, description, "VIDEO", displayOrder);
    }

    /**
     * Suppression d'un média
     */
    @Transactional
    public void deleteMedia(String mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        String fileName = media.getFileName();
        String contentDir = uploadDir + "/" + media.getContentId();
        Path mediaFilePath = Paths.get(contentDir).resolve(fileName);

        if (Files.exists(mediaFilePath)) {
            Files.delete(mediaFilePath);
        }

        Content content = contentRepository.findById(media.getContentId())
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + media.getContentId()));

        content.getMedia().removeIf(m -> m.getId().equals(mediaId));
        contentRepository.save(content);

        mediaRepository.deleteById(mediaId);
    }

    /**
     * Mise à jour des informations d'un média
     */
    @Transactional
    public MediaDTO updateMediaInfo(String mediaId, String title, String description, String mediaType, Integer displayOrder) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        media.setTitle(title);
        media.setDescription(description);

        if (mediaType != null) {
            // Si on change le type en COVER et qu'il existe déjà une couverture, supprimer l'ancienne
            if ("COVER".equals(mediaType) && !mediaType.equals(media.getMediaType())) {
                List<Media> existingCovers = mediaRepository.findByContentIdAndMediaType(media.getContentId(), "COVER");
                for (Media existingCover : existingCovers) {
                    if (!existingCover.getId().equals(mediaId)) {
                        try {
                            deleteMedia(existingCover.getId());
                        } catch (IOException e) {
                            // Log exception
                            System.err.println("Erreur lors de la suppression de l'ancienne couverture: " + e.getMessage());
                        }
                    }
                }
            }
            media.setMediaType(mediaType);
        }

        if (displayOrder != null) {
            media.setDisplayOrder(displayOrder);
        }

        Media updatedMedia = mediaRepository.save(media);
        MediaDTO dto = mediaMapper.toDTO(updatedMedia);
        enrichMediaWithUrl(dto);

        return dto;
    }

    /**
     * Mise à jour simplifiée des informations d'un média
     */
    @Transactional
    public MediaDTO updateMediaInfo(String mediaId, String title, String description) {
        // Appel de la méthode complète avec des valeurs par défaut
        return updateMediaInfo(mediaId, title, description, null, null);
    }

    /**
     * Charge un fichier en tant que ressource
     * Méthode ancienne - ne prend pas en compte l'organisation par dossier contentId
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + fileName, ex);
        }
    }

    /**
     * Charge un fichier en tant que ressource en utilisant contentId/fileName
     * Cette méthode prend en compte l'organisation par dossier contentId
     */
    public Resource loadFileAsResourceByContentId(String contentId, String fileName) {
        try {
            Path contentDirPath = this.fileStorageLocation.resolve(contentId);
            Path filePath = contentDirPath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + contentId + "/" + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + contentId + "/" + fileName, ex);
        }
    }
}