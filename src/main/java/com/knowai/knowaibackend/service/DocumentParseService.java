package com.knowai.knowaibackend.service;

public interface DocumentParseService {
    void parseDocument(Long documentId);
    boolean deleteDocument(Long documentId);
}
