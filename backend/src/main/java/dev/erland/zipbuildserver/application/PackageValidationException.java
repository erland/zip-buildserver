package dev.erland.zipbuildserver.application;

public class PackageValidationException extends RuntimeException {
    public PackageValidationException(String message) {
        super(message);
    }
}
