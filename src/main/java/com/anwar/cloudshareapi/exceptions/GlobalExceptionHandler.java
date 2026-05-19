package com.anwar.cloudshareapi.exceptions;


import com.mongodb.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateEmailException(DuplicateKeyException ex){
        Map<String,Object> data=new HashMap<>();
        data.put("status", HttpStatus.CONFLICT);
        data.put("message",ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(data);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex){
        Map<String,Object> data=new HashMap<>();
        String message = ex.getMessage() == null ? "Unexpected error" : ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (lowerMessage.contains("private") || lowerMessage.contains("does not belong")) {
            status = HttpStatus.FORBIDDEN;
        }

        data.put("status", status);
        data.put("message", message);
        return ResponseEntity.status(status).body(data);
    }
}
