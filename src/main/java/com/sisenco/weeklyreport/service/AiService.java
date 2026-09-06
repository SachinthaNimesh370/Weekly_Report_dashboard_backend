package com.sisenco.weeklyreport.service;

import com.sisenco.weeklyreport.dto.request.ChatRequest;
import com.sisenco.weeklyreport.dto.response.ChatResponse;

public interface AiService {
    ChatResponse processChat(ChatRequest request, String currentUserEmail);
}
