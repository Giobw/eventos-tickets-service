package cl.duoc.eventos.tickets.exception;

/** Error al interactuar con EFS (disco) o con AWS S3. */
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
