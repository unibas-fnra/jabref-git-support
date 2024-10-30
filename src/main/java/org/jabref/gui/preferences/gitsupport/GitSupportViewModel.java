package org.jabref.gui.preferences.gitsupport;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.gitsupport.AuthenticationViewMode;
import org.jabref.gui.gitsupport.GitSupportPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;

public class GitSupportViewModel implements PreferenceTabViewModel {

    private final BooleanProperty gitSupportEnabledProperty = new SimpleBooleanProperty();
    private final ObjectProperty<AuthenticationViewMode> authenticationMethod = new SimpleObjectProperty<>();

    private final GitSupportPreferences gitSupportPreferences;

    public GitSupportViewModel(GitSupportPreferences gitSupportPreferences) {
        this.gitSupportPreferences = gitSupportPreferences;
    }

    @Override
    public void setValues() {
        switch (gitSupportPreferences.getAuthenticationMethod()) {
            case SSH ->
                    authenticationMethod.setValue(AuthenticationViewMode.SSH);
            case CREDENTIALS ->
                    authenticationMethod.setValue(AuthenticationViewMode.CREDENTIALS);
        }
        gitSupportEnabledProperty.setValue(gitSupportPreferences.isGitEnabled());
    }

    /**
     * Saves the current user preferences to the GroupsPreferences object.
     */
    @Override
    public void storeSettings() {
        gitSupportPreferences.setGitSupportEnabledProperty(gitSupportEnabledProperty.getValue());

        if (AuthenticationViewMode.SSH.equals(authenticationMethod.getValue())) {
            gitSupportPreferences.setAuthenticationMethod(AuthenticationViewMode.SSH);
        } else if (AuthenticationViewMode.CREDENTIALS.equals(authenticationMethod.getValue())) {
            gitSupportPreferences.setAuthenticationMethod(AuthenticationViewMode.CREDENTIALS);
        }
    }

    public BooleanProperty gitSupportEnabledProperty() {
        return gitSupportEnabledProperty;
    }

    public AuthenticationViewMode authenticationViewMode() {
        return authenticationMethod.getValue();
    }

    public ObjectProperty<AuthenticationViewMode> authenticationViewModeObjectProperty() {
        return authenticationMethod;
    }

    @Override
    public boolean validateSettings() {
        return PreferenceTabViewModel.super.validateSettings();
    }

    @Override
    public List<String> getRestartWarnings() {
        return PreferenceTabViewModel.super.getRestartWarnings();
    }
}
