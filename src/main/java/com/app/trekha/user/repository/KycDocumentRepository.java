package com.app.trekha.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.trekha.user.model.KycDocument;
import com.app.trekha.user.model.User;

import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByUser(User user);
}
