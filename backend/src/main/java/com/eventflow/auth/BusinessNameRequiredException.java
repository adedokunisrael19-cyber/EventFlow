package com.eventflow.auth;

public class BusinessNameRequiredException extends RuntimeException {
    public BusinessNameRequiredException() { super("Enter your business name to create an EventFlow account."); }
}
