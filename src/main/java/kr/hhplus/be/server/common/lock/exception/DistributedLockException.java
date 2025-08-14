package kr.hhplus.be.server.common.lock.exception;

public class DistributedLockException extends RuntimeException{
    public DistributedLockException(String message) {
        super(message);
    }

    public DistributedLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
