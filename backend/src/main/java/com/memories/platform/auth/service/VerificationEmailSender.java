package com.memories.platform.auth.service;

import com.memories.platform.auth.dto.VerificationEmail;

public interface VerificationEmailSender {

    void send(VerificationEmail email);
}
