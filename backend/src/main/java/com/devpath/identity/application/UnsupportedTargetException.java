package com.devpath.identity.application;

public class UnsupportedTargetException extends RuntimeException {
    public UnsupportedTargetException(String type) { super("Unsupported " + type + " target"); }
}
