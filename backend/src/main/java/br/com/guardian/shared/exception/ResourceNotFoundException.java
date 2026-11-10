package br.com.guardian.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = "RESOURCE";
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s com identificador '%s' não foi encontrado.", resourceName, identifier));
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }
}
