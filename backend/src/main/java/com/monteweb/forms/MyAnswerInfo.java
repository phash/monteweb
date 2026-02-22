package com.monteweb.forms;

import java.util.List;
import java.util.UUID;

public record MyAnswerInfo(
    UUID questionId,
    String text,
    List<String> selectedOptions,
    Integer rating
) {}
