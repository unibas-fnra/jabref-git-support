package org.jabref.logic.git;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

/**
 * This class is responsible for getting the status of a git repository. Its methods do not change the state
 * of the repository in any way.
 */
class GitStatus {

    private final Git git;
    private final Path repository;

    GitStatus(Git git) {
        this.git = git;
        this.repository = git.getRepository().getDirectory().getParentFile().toPath();
    }

    List<String> getBranchNames() throws GitException {
        List<Ref> localBranches;
        try {
            localBranches = this.git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        } catch (GitAPIException e) {
            throw new GitException("An error occurred while fetching branch names", e);
        }
        List<String> branchNames = new ArrayList<>();
        for (Ref branch : localBranches) {
            branchNames.add(branch.getName());
        }
        return branchNames;
    }

    boolean hasUntrackedFiles() throws GitException {
        return !getUntrackedFiles().isEmpty();
    }

    /**
     *
     * @return a set of paths to the modified and newly added files in the repository.
     */
    Set<Path> getUntrackedFiles() throws GitException {
        Status status;
        try {
            status = this.git.status().call();
        } catch (GitAPIException e) {
            throw new GitException("Failed to get git status", e);
        }
        Set<String> untrackedFiles = new HashSet<>(status.getUntracked());
        untrackedFiles.addAll(status.getModified());
        Set<Path> untrackedFilesPaths = new HashSet<>();
        for (String untrackedFile : untrackedFiles) {
            untrackedFilesPaths.add(repository.resolve(untrackedFile));
        }
        return untrackedFilesPaths;
    }

    boolean hasTrackedFiles() throws GitException {
        return !getTrackedFiles().isEmpty();
    }

    /**
     *
     * @return a set of paths to the added and changed files in the repository.
     */
    Set<Path> getTrackedFiles() throws GitException {
        Status status;
        try {
            status = this.git.status().call();
        } catch (GitAPIException e) {
            throw new GitException("Failed to get git status", e);
        }
        Set<String> trackedFiles = new HashSet<>(status.getAdded());
        trackedFiles.addAll(status.getChanged());
        Set<Path> trackedFilesPaths = new HashSet<>();
        for (String trackedFile : trackedFiles) {
            trackedFilesPaths.add(repository.resolve(trackedFile));
        }
        return trackedFilesPaths;
    }

    boolean hasUntrackedFolders() throws GitException {
        return !getUntrackedFolders().isEmpty();
    }

    /**
     *
     * @return a set of paths to the untracked folders in the repository. If multiple folders are nested,
     * only the top-level folder is returned.
     */
    Set<Path> getUntrackedFolders() throws GitException {
        Status status;
        try {
            status = this.git.status().call();
        } catch (GitAPIException e) {
            throw new GitException("Failed to get git status", e);
        }
        Set<String> untrackedFolders = new HashSet<>(status.getUntrackedFolders());
        Set<Path> untrackedFoldersPaths = new HashSet<>();
        for (String untrackedFolder : untrackedFolders) {
            untrackedFoldersPaths.add(repository.resolve(untrackedFolder));
        }
        return untrackedFoldersPaths;
    }
}
