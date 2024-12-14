package org.jabref.logic.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(GitManager.class);
    private final static String DEFAULT_COMMIT_MESSAGE = "Automatic update via JabRef";
    private static boolean sshAuthenticationVerified = false;
    private static boolean httpAuthenticationVerified = false;
    private final Path path;
    private final Git git;
    private final GitPreferences preferences;
    private final GitActionExecutor gitActionExecutor;
    private final GitStatus gitStatus;
    private int sychronizeCount;

    private GitProtocol gitProtocol = GitProtocol.UNKNOWN;

    public GitManager(Git git, GitPreferences preferences) {
        this.path = git.getRepository().getDirectory().getParentFile().toPath();
        this.git = git;
        this.gitStatus = new GitStatus(this.git);
        this.gitActionExecutor = new GitActionExecutor(this.git, new GitAuthenticator(preferences), this.gitStatus);
        this.preferences = preferences;
        determineGitProtocol();
    }

    /**
     * this methode uses user's preferences to determine whether to synchronize the associated
     * bib file. It also keeps count of how many times it was called. This counter is reset
     * once this methode has returned true.
     *
     * @return false if git is not enabled or git is enabled but the number of times this method was called is
     *         less than the frequency specified by the user. <br>
     *         true otherwise
     */
    public boolean shouldSynchronize() {
        if (!preferences.isGitEnabled()) {
            return false;
        }
        if (!preferences.isPushFrequencyEnabled()) {
            return true;
        }

        int pushFrequency = preferences.getPushFrequency().map(Integer::parseInt).orElse(1);

        sychronizeCount++;
        LOGGER.debug("push frequency: {}", pushFrequency);
        LOGGER.debug("Current save count: {}", sychronizeCount);
        if (sychronizeCount < pushFrequency) {
            return false;
        }

        if (pushFrequency <= 0) {
            LOGGER.warn("Invalid push frequency: {}. Push frequency must be greater than 0.", pushFrequency);
            return false;
        }

        sychronizeCount = 0;
        return true;
    }

    public void synchronize(Path filePath) throws GitException {
        if (!hasUncommittedChanges(filePath)) {
            LOGGER.debug("No changes detected in {}. Skipping git operations.", path);
            throw new GitException("No changes detected in bib file. Skipping git operations.",
                    Localization.lang("No changes detected in bib file. Skipping git operations."));
        }
        if (gitStatus.hasTrackedFiles()) {
            Set<Path> trackedFiles = gitStatus.getTrackedFiles();
            gitActionExecutor.unstage(new ArrayList<>(trackedFiles));
        }
        gitActionExecutor.add(filePath);
        LOGGER.debug("file was added to staging area successfully");
        gitActionExecutor.commit(DEFAULT_COMMIT_MESSAGE, false);
        LOGGER.info("Committed changes for {}", filePath);
        update();
        gitActionExecutor.push();
        LOGGER.debug("{} was pushed successfully", filePath);
        updateAuthenticationStatus();
    }

    public void update() throws GitException {
        try {
            gitActionExecutor.pull(true);
            LOGGER.debug("Git pull with rebase was successful.");
            updateAuthenticationStatus();
            return;
        } catch (GitConflictException e) {
            LOGGER.debug("Pull with rebase failed. Attempting to undo changes done by the pull operation...");
            gitActionExecutor.undoPull();
        }
        updateAuthenticationStatus();
        try {
            LOGGER.debug("Attempting pull with merge strategy...");
            gitActionExecutor.pull(false);
            LOGGER.debug("Git pull with merge strategy was successful.");
        } catch (GitConflictException e) {
            LOGGER.debug("Pull with merge strategy failed. Please resolve conflicts manually.");
            gitActionExecutor.undoPull();
            throw new GitConflictException("Git pull resulted in conflicts. Please resolve manually.",
                    Localization.lang("Git pull resulted in conflicts. Please resolve manually."));
        }
    }

    public boolean hasUncommittedChanges(Path filePath) throws GitException {
        Set<Path> untrackedFiles = gitStatus.getUntrackedFiles();
        return untrackedFiles.contains(filePath);
    }

    /**
     *
     * @return Returns true if the given repository path to the GitManager object to a directory that is a git repository (contains a .git folder)
     */
    public static boolean isGitRepository(Path path) {
        return findGitRepository(path).isPresent();
    }

    public static GitManager openGitRepository(Path path, GitPreferences gitPreferences) throws GitException {
        Optional<Path> optionalPath = findGitRepository(path);
        if (optionalPath.isEmpty()) {
            throw new GitException(path.getFileName() + " is not in a git repository.");
        }
        try {
            return new GitManager(Git.open(optionalPath.get().toFile()), gitPreferences);
        } catch (IOException e) {
            throw new GitException("Failed to open git repository", e);
        }
    }

    /**
     * Initiates git repository at given path.
     */
    public static GitManager initGitRepository(Path path, GitPreferences gitPreferences)
            throws GitException {
        try {
            if (isGitRepository(path)) {
                throw new GitException(path.getFileName() + " is already a git repository.");
            }
            Git git = Git.init()
                         .setDirectory(path.toFile())
                         .setInitialBranch("main")
                         .call();
            LOGGER.info("Git repository initialized successfully.");
            return new GitManager(git, gitPreferences);
        } catch (GitAPIException e) {
            throw new GitException("Initialization of git repository failed", e);
        }
    }

    void close() {
        git.close();
    }

    /**
     * traverse up the directory tree until a .git directory is found or the root is reached.
     *
     * @return to git repository if found or an empty optional otherwise.
     */
    static Optional<Path> findGitRepository(Path path) {
        Path currentPath = path;

        while (currentPath != null) {
            if (Files.isDirectory(currentPath.resolve(".git"))) {
                return Optional.of(currentPath);
            }
            currentPath = currentPath.getParent();
        }
        return Optional.empty();
    }

    GitActionExecutor getGitActionExecutor() {
        return this.gitActionExecutor;
    }

    GitStatus getGitStatus() {
        return this.gitStatus;
    }

    public Path getPath() {
        return path;
    }

    /**
     * determines the protocol used to communicate with origin.
     */
    private void determineGitProtocol() {
        try {
            Transport transport = Transport.open(git.getRepository(), "origin");
            if (transport instanceof SshTransport) {
                LOGGER.debug("SSH protocol detected");
                gitProtocol = GitProtocol.SSH;
            } else if (transport instanceof HttpTransport) {
                LOGGER.debug("Http protocol detected");
                gitProtocol = GitProtocol.HTTPS;
            } else {
                LOGGER.debug("unknown protocol detected");
                gitProtocol = GitProtocol.UNKNOWN;
            }
        } catch (NotSupportedException | URISyntaxException | TransportException e) {
            LOGGER.warn("Failed to determine git protocol");
        }
    }

    private void updateAuthenticationStatus() {
        sshAuthenticationVerified = sshAuthenticationVerified || gitProtocol == GitProtocol.SSH;
        httpAuthenticationVerified = httpAuthenticationVerified || gitProtocol == GitProtocol.HTTPS;
    }

    /**
     * Prompts the user for the passphrase of the SSH key or the password encryption key based on the git protocol.
     * The prompt is skipped if the passphrase or password encryption key is already provided and a connection was
     * successfully established using it. It is also skipped if the user did not encrypt the SSH key or the password.
     *
     * @param dialogService the dialog service used to prompt the user
     */
    public void promptForPassphraseIfNeeded(DialogService dialogService) {
        switch (this.gitProtocol) {
            case SSH:
                if (preferences.isSshKeyEncrypted() && !sshAuthenticationVerified) {
                    GitPreferences.setSshPassphrase(dialogService.showPasswordDialogAndWait(
                            "SSH passphrase",
                            "Enter passphrase for your specified SSH key",
                            "SSH passphrase"
                    ).orElse(null));
                }
                return;
            case HTTPS:
                if (preferences.isPasswordEncrypted() && !httpAuthenticationVerified) {
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
