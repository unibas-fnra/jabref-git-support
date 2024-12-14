package org.jabref.gui.git;

import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitManager;
import org.jabref.logic.git.GitPreferences;
import org.jabref.logic.git.GitProtocol;

public class PassphrasePrompter {

    /**
     * Prompts the user for the passphrase of the SSH key or the password encryption key based on the git protocol.
     * The prompt is skipped if the passphrase or password encryption key is already provided and a connection was
     * successfully established using it. It is also skipped if the user did not encrypt the SSH key or the password.
     *
     */
    public static void promptIfNeeded(GitManager gitManager, GitPreferences preferences, DialogService dialogService) {
        GitProtocol gitProtocol = gitManager.getGitProtocol();
        switch (gitProtocol) {
            case SSH:
                if (preferences.isSshKeyEncrypted() && !GitManager.isSshAuthenticationVerified()) {
                    GitPreferences.setSshPassphrase(dialogService.showPasswordDialogAndWait(
                            "SSH passphrase",
                            "Enter passphrase for your specified SSH key",
                            "SSH passphrase"
                    ).orElse(null));
                }
                return;
            case HTTPS:
                if (preferences.isPasswordEncrypted() && !GitManager.isHttpAuthenticationVerified()) {
                    GitPreferences.setPasswordEncryptionKey(dialogService.showPasswordDialogAndWait(
                            "password encryption key",
                            "Enter password encryption key",
                            ""
                    ).orElse(null));
                }
                return;
            default:
                break;
        }
    }
}
