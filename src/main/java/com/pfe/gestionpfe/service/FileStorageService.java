package com.pfe.gestionpfe.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final long MAX_PDF_SIZE = 5L * 1024L * 1024L;
    private final Path uploadRoot = Paths.get("uploads");

    public String storePdf(MultipartFile file, String prefix) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier PDF est vide.");
        }
        if (file.getSize() > MAX_PDF_SIZE) {
            throw new IllegalArgumentException("Le fichier PDF dépasse la taille maximale de 5 Mo.");
        }

        String originalName = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        if (!originalName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Seuls les fichiers PDF sont autorisés.");
        }

        if (!Files.exists(uploadRoot)) {
            Files.createDirectories(uploadRoot);
        }

        String safePrefix = prefix == null ? "document" : prefix.replaceAll("[^A-Za-z0-9_-]", "_");
        String safeName = UUID.randomUUID() + "_" + safePrefix + ".pdf";
        Path normalizedRoot = uploadRoot.toAbsolutePath().normalize();
        Path target = normalizedRoot.resolve(safeName).normalize();
        if (!target.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Nom de fichier invalide.");
        }

        try (var inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(5);
            if (!"%PDF-".equals(new String(header, StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("Le contenu du fichier n'est pas un PDF valide.");
            }
            try (var completeStream = new SequenceInputStream(new ByteArrayInputStream(header), inputStream)) {
                Files.copy(completeStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return safeName;
    }
}
