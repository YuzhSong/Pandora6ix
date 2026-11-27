package com.pandora6ix.backend.crm;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.config.Dev2Isolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.UUID;

@Component
@Profile("dev2")
class CrmAttachments {
    private static final Logger LOG = LoggerFactory.getLogger(CrmAttachments.class);
    private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private final Path root;

    record Attachment(String id, String name) {}

    CrmAttachments(Environment env) {
        String url = env.getProperty("spring.datasource.url", "");
        this.root = Dev2Isolation.dataRoot().resolve(url.startsWith("jdbc:h2:mem:") ? "test-attachments" : "attachments");
    }

    Attachment replace(MultipartFile file, String previous) {
        if (file.getSize() < PDF_HEADER.length || file.getSize() > MAX_BYTES) {
            throw BusinessException.invalid("Select a PDF up to 10 MiB.");
        }
        String id = UUID.randomUUID().toString();
        Path target = target(id);
        try (var input = file.getInputStream()) {
            if (!Arrays.equals(input.readNBytes(PDF_HEADER.length), PDF_HEADER)) {
                throw BusinessException.invalid("Select a valid PDF.");
            }
            Files.createDirectories(root);
            try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                output.write(PDF_HEADER);
                input.transferTo(output);
            }
        } catch (IOException ex) {
            discard(id);
            throw storageError(ex);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    discard(previous);
                } else if (status == STATUS_ROLLED_BACK) {
                    discard(id);
                }
            }
        });
        String name = file.getOriginalFilename();
        name = name == null ? "" : name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        return new Attachment(id, name.isEmpty() || name.length() > 180 ? "contract.pdf" : name);
    }

    byte[] read(String id) {
        if (id == null) throw BusinessException.missing();
        try (var input = Files.newInputStream(target(id), LinkOption.NOFOLLOW_LINKS)) {
            byte[] content = input.readNBytes((int) MAX_BYTES + 1);
            if (content.length > MAX_BYTES) throw new IOException("Attachment exceeds configured limit.");
            return content;
        } catch (NoSuchFileException ex) {
            throw BusinessException.missing();
        } catch (IOException ex) {
            throw storageError(ex);
        }
    }

    private Path target(String id) {
        Dev2Isolation.verifyLocalPath(Dev2Isolation.dataRoot());
        Dev2Isolation.verifyLocalPath(root);
        return root.resolve(UUID.fromString(id) + ".pdf");
    }

    private void discard(String id) {
        if (id == null) return;
        try {
            Files.deleteIfExists(target(id));
        } catch (IOException | IllegalStateException ex) {
            LOG.warn("CRM attachment cleanup failed for {}.", id);
        }
    }

    private IllegalStateException storageError(IOException ex) {
        return new IllegalStateException("CRM attachment storage failed.", ex);
    }
}
