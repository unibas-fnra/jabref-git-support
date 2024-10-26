package org.jabref.gui.gitsupport;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class GitSupportPreferences {
    private BooleanProperty gitSupportEnabledProperty = new SimpleBooleanProperty();
    private ObjectProperty<AuthenticationViewMode> authenticationMethod = new SimpleObjectProperty<>();

    public GitSupportPreferences(boolean gitSupportEnabledProperty, AuthenticationViewMode authenticationMethod) {
        this.gitSupportEnabledProperty = new SimpleBooleanProperty(gitSupportEnabledProperty);
        this.authenticationMethod = new SimpleObjectProperty<AuthenticationViewMode>(authenticationMethod);
    }

    public AuthenticationViewMode getAuthenticationMethod() {
        return authenticationMethod.getValue();
    }

    public ObjectProperty<AuthenticationViewMode> getAuthenticationProperty() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(AuthenticationViewMode authenticationMethod) {
        this.authenticationMethod.set(authenticationMethod);
    }

    public boolean isGitEnabled() {
        return gitSupportEnabledProperty.getValue();
    }

    public BooleanProperty getGitSupportEnabledProperty() {
        return gitSupportEnabledProperty;
    }

    public void setGitSupportEnabledProperty(boolean gitSupportEnabledProperty) {
        this.gitSupportEnabledProperty.set(gitSupportEnabledProperty);
    }
}
