package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    Optional<StoredFile> findByBucketAndFilename(String bucket, String filename);
}
