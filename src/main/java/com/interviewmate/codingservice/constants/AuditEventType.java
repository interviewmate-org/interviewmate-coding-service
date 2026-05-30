package com.interviewmate.codingservice.constants;

public enum AuditEventType {
    UNAUTHORIZED,       // missing/invalid header — stopped at filter
    ACCESS_DENIED,      // valid user but wrong role
    ACCESS_GRANTED,     // passed all checks, entering controller
    REQUEST_SUCCESS,    // controller completed successfully
    REQUEST_ERROR       // unexpected error in controller
}