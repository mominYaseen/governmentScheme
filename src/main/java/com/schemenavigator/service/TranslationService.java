package com.schemenavigator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TranslationService {

    public String translateToLanguage(String text, String targetLanguage) {
        log.debug("Translation stub called for language: {}", targetLanguage);
        return text;
    }
}
