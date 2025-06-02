package com.move.contentservice.controller;

import com.move.contentservice.dto.MediaDTO;
import com.move.contentservice.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * Upload d'un média général (photo, vidéo, etc.)
     */
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<MediaDTO> uploadMedia(@RequestParam("file") MultipartFile file,
                                                @RequestParam("contentId") String contentId,
                                                @RequestParam("title") String title,
                                                @RequestParam("description") String description,
                                                @RequestParam(value = "mediaType", defaultValue = "ALBUM") String mediaType,
                                                @RequestParam(value = "displayOrder", required = false) Integer displayOrder) throws IOException {
        MediaDTO uploaded = mediaService.uploadMedia(contentId, file, title, description, mediaType, displayOrder);
        return ResponseEntity.ok(uploaded);
    }

    /**
     * Upload spécifique d'une image de couverture
     */
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PostMapping("/upload/cover")
    public ResponseEntity<MediaDTO> uploadCoverImage(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("contentId") String contentId,
                                                     @RequestParam("title") String title,
                                                     @RequestParam("description") String description) throws IOException {
        MediaDTO uploaded = mediaService.uploadCoverImage(contentId, file, title, description);
        return ResponseEntity.ok(uploaded);
    }

    /**
     * Upload spécifique d'une photo d'album
     */
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PostMapping("/upload/photo")
    public ResponseEntity<MediaDTO> uploadAlbumPhoto(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("contentId") String contentId,
                                                     @RequestParam("title") String title,
                                                     @RequestParam("description") String description,
                                                     @RequestParam(value = "displayOrder", required = false) Integer displayOrder) throws IOException {
        MediaDTO uploaded = mediaService.uploadAlbumPhoto(contentId, file, title, description, displayOrder);
        return ResponseEntity.ok(uploaded);
    }

    /**
     * Upload spécifique d'une vidéo
     */
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PostMapping("/upload/video")
    public ResponseEntity<MediaDTO> uploadVideo(@RequestParam("file") MultipartFile file,
                                                @RequestParam("contentId") String contentId,
                                                @RequestParam("title") String title,
                                                @RequestParam("description") String description,
                                                @RequestParam(value = "displayOrder", required = false) Integer displayOrder) throws IOException {
        MediaDTO uploaded = mediaService.uploadVideo(contentId, file, title, description, displayOrder);
        return ResponseEntity.ok(uploaded);
    }

    /**
     * Récupérer l'image de couverture d'un contenu
     */
    @GetMapping("/cover/{contentId}")
    public ResponseEntity<MediaDTO> getCoverByContentId(@PathVariable String contentId) {
        MediaDTO cover = mediaService.getCoverByContentId(contentId);
        return ResponseEntity.ok(cover);
    }

    /**
     * Récupérer toutes les photos d'un contenu
     */
    @GetMapping("/photos/{contentId}")
    public ResponseEntity<List<MediaDTO>> getPhotosByContentId(@PathVariable String contentId) {
        List<MediaDTO> photos = mediaService.getPhotosByContentId(contentId);
        return ResponseEntity.ok(photos);
    }

    /**
     * Récupérer toutes les vidéos d'un contenu
     */
    @GetMapping("/videos/{contentId}")
    public ResponseEntity<List<MediaDTO>> getVideosByContentId(@PathVariable String contentId) {
        List<MediaDTO> videos = mediaService.getVideosByContentId(contentId);
        return ResponseEntity.ok(videos);
    }

    /**
     * Récupérer tous les médias associés à un contenu
     */
    @GetMapping("/content/{contentId}")
    public ResponseEntity<List<MediaDTO>> getMediaByContentId(@PathVariable String contentId) {
        return ResponseEntity.ok(mediaService.getMediaByContentId(contentId));
    }

    /**
     * Récupérer un média par son ID
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaDTO> getMediaById(@PathVariable String mediaId) {
        return ResponseEntity.ok(mediaService.getMediaById(mediaId));
    }

    /**
     * Téléchargement d'un fichier média par son ID
     * Cette méthode est plus pratique pour le frontend car elle utilise directement l'ID du média
     */
    @GetMapping("/file/{mediaId}")
    public ResponseEntity<Resource> getFileByMediaId(@PathVariable String mediaId, HttpServletRequest request) {
        MediaDTO media = mediaService.getMediaById(mediaId);
        Resource resource = mediaService.loadFileAsResourceByContentId(media.getContentId(), media.getFileName());

        String contentType = media.getFileType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Téléchargement d'un fichier média par contentId et fileName
     * Cette méthode permet d'accéder aux fichiers organisés par dossier contentId
     */
    @GetMapping("/files/{contentId}/{fileName:.+}")
    public ResponseEntity<Resource> getFileByContentId(@PathVariable String contentId,
                                                       @PathVariable String fileName,
                                                       HttpServletRequest request) {
        Resource resource = mediaService.loadFileAsResourceByContentId(contentId, fileName);

        // Déterminer le type de contenu
        String contentType;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            contentType = "application/octet-stream";
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Ancienne méthode - gardée pour compatibilité
     * À éviter car ne gère pas correctement les dossiers par contentId
     */
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = mediaService.loadFileAsResource(fileName);

        // Déterminer le type de contenu
        String contentType;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            contentType = "application/octet-stream";
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Récupérer les médias d'un type spécifique pour un contenu
     */
    @GetMapping("/content/{contentId}/type/{mediaType}")
    public ResponseEntity<List<MediaDTO>> getMediaByContentIdAndType(@PathVariable String contentId,
                                                                     @PathVariable String mediaType) {
        return ResponseEntity.ok(mediaService.getMediaByContentIdAndType(contentId, mediaType));
    }

    /**
     * Mise à jour des infos (titre, description, mediaType, displayOrder)
     */
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PutMapping("/{mediaId}")
    public ResponseEntity<MediaDTO> updateMediaInfo(@PathVariable String mediaId,
                                                    @RequestParam("title") String title,
                                                    @RequestParam("description") String description,
                                                    @RequestParam(value = "mediaType", required = false) String mediaType,
                                                    @RequestParam(value = "displayOrder", required = false) Integer displayOrder) {
        return ResponseEntity.ok(mediaService.updateMediaInfo(mediaId, title, description, mediaType, displayOrder));
    }

    /**
     * Suppression (propriétaire ou ADMIN / MASTERADMIN)
     */
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(@PathVariable String mediaId, HttpServletRequest request) throws IOException {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }
}