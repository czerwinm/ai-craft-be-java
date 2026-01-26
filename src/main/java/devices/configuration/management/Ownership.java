package devices.configuration.management;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Ownership(String operator, String provider) {

    public Ownership {
        boolean bothNull = operator == null && provider == null;
        boolean bothNotNull = operator != null && provider != null;
        if (!bothNull && !bothNotNull) {
            throw new IllegalArgumentException("Both operator and provider must be either null or non-null");
        }
    }

    public static Ownership unowned() {
        return new Ownership(null, null);
    }

    public static Ownership of(String operator, String provider) {
        return new Ownership(operator, provider);
    }

    @JsonIgnore
    public boolean isUnowned() {
        return operator == null && provider == null;
    }

    @JsonIgnore
    public boolean isOwned() {
        return operator != null && provider != null;
    }
}
