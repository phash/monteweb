package com.monteweb.forms;

import java.util.List;
import java.util.UUID;

public record MyResponseInfo(
    UUID responseId,
    List<MyAnswerInfo> answers
) {}
