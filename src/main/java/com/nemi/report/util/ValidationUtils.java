package com.nemi.report.util;

import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.exception.ValidationAlertCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationUtils {
    public static void validateTimeRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            log.error("Invalid date range: from={}, to={}", from, to);
            throw new ValidationException(AlertMessages.alert(ValidationAlertCode.DATA_INVALID));
        }
    }
}
