package org.jabref.gui.preferences.gitsupport;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class GitSupportTab extends AbstractPreferenceTabView<GitSupportViewModel> implements PreferencesTab {
    @FXML private CheckBox enableGitSupport;
    @FXML private CheckBox frequencyLabel;
    @FXML private ComboBox<String> setPushFrequency;
    @FXML private ComboBox<String> authentificationMethod;
    @FXML private Button synchronizeButton;
    @FXML  private Label authentificationLabel;


    public GitSupportTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }
    @Override
    public String getTabName() {
        return Localization.lang("Git Support");
    }

    @Override
    public Node getStyleableNode() {
        return super.getStyleableNode();
    }

    public void initialize() {
        this.viewModel = new GitSupportViewModel(preferencesService.getGitSupportPreferences());
        frequencyLabel.disableProperty().bind(enableGitSupport.selectedProperty().not());
        setPushFrequency.disableProperty().bind(enableGitSupport.selectedProperty().not());
        authentificationMethod.disableProperty().bind(enableGitSupport.selectedProperty().not());
        synchronizeButton.disableProperty().bind(enableGitSupport.selectedProperty().not());
        authentificationLabel.disableProperty().bind(enableGitSupport.selectedProperty().not());
    }
}
