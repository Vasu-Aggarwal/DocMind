package com.docmind.backend.exception;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(){
        super("Resource not found !");
    }

    public ResourceNotFoundException(String message){
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable ex){
        super(message, ex);
    }
}
