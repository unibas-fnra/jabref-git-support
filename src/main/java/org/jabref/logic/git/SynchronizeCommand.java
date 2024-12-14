package org.jabref.logic.git;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.binding.BooleanExpression;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.collab.DatabaseChangeMonitor;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.tobiasdiez.easybind.PreboundBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizeCommand extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizeCommand.class);
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;

    public SynchronizeCommand(Supplier<LibraryTab> tabSupplier,
                              DialogService dialogService,
                              StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.executable.bind(ActionHelper.needsDatabase(stateManager)
                                         .and(SynchronizeCommand.existsInGitRepository(stateManager)));
    }

    /**
     * Synchronizes the library with the remote repository. It pulls changes from the remote repository.
     * If there are uncommitted changes to current library, they are committed before that and pushed afterward.
     *
     * @implNote The  database change monitor is unregistered before the synchronization and registered afterward.
     */
    public void execute() {
        LibraryTab libraryTab = tabSupplier.get();

        if (libraryTab == null) {
            LOGGER.warn("LibraryTab is null.");
            return;
        }

        Optional<DatabaseChangeMonitor> optionalChangeMonitor = libraryTab.getChangeMonitor();
        Optional<GitManager> optionalGitManager = libraryTab.getGitManager();
        BibDatabaseContext databaseContext = libraryTab.getBibDatabaseContext();
        Optional<Path> optionalFilePath = databaseContext.getDatabasePath();

        if (optionalGitManager.isEmpty() || optionalFilePath.isEmpty() || !databaseContext.isInGitRepository()) {
            LOGGER.warn("path to file could not be found or the file is not in a git repository");
            return;
        }

        optionalGitManager.get().promptForPassphraseIfNeeded(dialogService);
        try {
            optionalChangeMonitor.ifPresent(DatabaseChangeMonitor::unregister);

            if (optionalGitManager.get().hasUncommittedChanges(optionalFilePath.get())) {
                optionalGitManager.get().synchronize(optionalFilePath.get());
            } else {
                optionalGitManager.get().update();
            }

            optionalChangeMonitor.ifPresent(DatabaseChangeMonitor::acceptChanges);
            dialogService.notify(Localization.lang("Library saved and pushed to remote."));
        } catch (GitException e) {
            LOGGER.warn("Git error during save operation.", e);
            dialogService.notify(e.getLocalizedMessage());
        } finally {
            optionalChangeMonitor.ifPresent(DatabaseChangeMonitor::register);
        }
    }

    /**
     * Returns a BooleanExpression that is true if the active database is in a git repository. It is used
     * to enable the synchronize command.
     */
    private static BooleanExpression existsInGitRepository(StateManager stateManager) {
        OptionalObjectProperty<BibDatabaseContext> optionalBibDatabaseContext = stateManager.activeDatabaseProperty();
        return BooleanExpression.booleanExpression(new PreboundBinding<>(optionalBibDatabaseContext) {
            @Override
            protected Boolean computeValue() {
                if (!optionalBibDatabaseContext.isPresent().get() || optionalBibDatabaseContext.get().isEmpty()) {
                    return false;
                }
                return optionalBibDatabaseContext.get().get().isInGitRepository();
            }
        });
    }
}

