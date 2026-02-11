package com.monteweb.forms;

import java.util.List;

public record FormDetailInfo(
        FormInfo form,
        List<QuestionInfo> questions
) {
}
