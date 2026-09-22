package net.warp_scores.warpscores.service;

public class PyBb3ServiceException extends RuntimeException {
    private final int statusCode;

    public PyBb3ServiceException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
