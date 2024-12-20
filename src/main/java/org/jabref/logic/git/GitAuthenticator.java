package org.jabref.logic.git;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Optional;

import org.jabref.logic.shared.security.Password;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.IdentityPasswordProvider;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GitAuthenticator {
    private final static Logger LOGGER = LoggerFactory.getLogger(GitAuthenticator.class);

    private static final Path HOME_DIRECTORY = Path.of(Optional.of(System.getProperty("user.home")).orElse(""));
    private final GitPreferences preferences;

    /**
     * This class is responsible for authenticating git commands with the credentials provided in the preferences.
     * <br>
     *
     * Usage:
     * <pre>
     *     GitAuthenticator authenticator = new GitAuthenticator(preferences);
     *     PullCommand pullCommand = git.pull();
     *     authenticator.authenticate(pullCommand);
     *     pullCommand.call();
     * </pre>
     */
    GitAuthenticator(GitPreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Authenticates the given transport command with the credentials provided in the preferences.
     * Both HTTPS and SSH configurations are added to the command. JGit will automatically choose the correct
     * configuration based on the repository URL.
     */
    <Command extends TransportCommand<Command, ?>> void authenticate(Command transportCommand) {
        transportCommand.setCredentialsProvider(getCredentialsProvider());
        transportCommand.setTransportConfigCallback(this::transportConfigCallback);
    }

    /**
     * fetches the credentials from the preferences. If the password is encrypted, the encryption key
     * must be set in the preferences dynamically in run time before calling this method.
     */
    private CredentialsProvider getCredentialsProvider() {
        String password = preferences.getPassword().orElse("");
        try {
            password = new Password(
                    preferences.getPassword().orElse("").toCharArray(),
                    GitPreferences.getPasswordEncryptionKey().orElse(preferences.getUsername().orElse(""))
            ).decrypt();
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LOGGER.debug("Error while decrypting git password");
        }
        return new UsernamePasswordCredentialsProvider(preferences.getUsername().orElse(""), password);
    }

    /**
     * Configures the SSH transport with the necessary settings based on the information provided in the preferences.
     * If the SSH key is encrypted, the passphrase must be set in the preferences dynamically in run time
     * before calling this method.
     */
    private void transportConfigCallback(Transport transport) {
        if (!(transport instanceof SshTransport sshTransport)) {
            LOGGER.debug("git repository does not use a SSH protocol");
            return;
        }
        SshdSessionFactoryBuilder sshdSessionFactoryBuilder = new SshdSessionFactoryBuilder()
                .setPreferredAuthentications("publickey")
                .setHomeDirectory(HOME_DIRECTORY.toFile())
                .setSshDirectory(Path.of(preferences.getSshDirPath().orElse("")).toFile())
                .setKeyPasswordProvider(cp -> new IdentityPasswordProvider(cp) {
                    @Override
                    protected char[] getPassword(URIish uri, String message) {
                        return GitPreferences.getSshPassphrase().orElse("").toCharArray();
                    }
                });
        sshTransport.setSshSessionFactory(sshdSessionFactoryBuilder.build(null));
    }
}
