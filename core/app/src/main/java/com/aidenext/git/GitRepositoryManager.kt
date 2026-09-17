/*
 *  This file is part of AIDE Next.
 *
 *  AIDE Next is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AIDE Next is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AIDE Next.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aidenext.git

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File

data class GitCommitInfo(
  val hash: String,
  val shortHash: String,
  val author: String,
  val message: String,
  val timestamp: Long
)

data class GitRepoStatus(
  val currentBranch: String,
  val added: Set<String>,
  val modified: Set<String>,
  val uncommittedChanges: Set<String>,
  val untracked: Set<String>,
  val missing: Set<String>,
  val hasCleanWorkingTree: Boolean
)

object GitRepositoryManager {

  private val log = LoggerFactory.getLogger(GitRepositoryManager::class.java)

  fun isGitRepository(dir: File): Boolean {
    val gitDir = File(dir, ".git")
    return gitDir.exists() && gitDir.isDirectory
  }

  fun openRepository(projectDir: File): Git? {
    return try {
      val repo = FileRepositoryBuilder()
        .setWorkTree(projectDir)
        .findGitDir(projectDir)
        .build()
      Git(repo)
    } catch (e: Exception) {
      log.warn("Failed to open git repository at {}", projectDir, e)
      null
    }
  }

  fun getStatus(projectDir: File): GitRepoStatus? {
    val git = openRepository(projectDir) ?: return null
    return try {
      val status = git.status().call()
      val branch = git.repository.branch ?: "HEAD"
      GitRepoStatus(
        currentBranch = branch,
        added = status.added,
        modified = status.modified,
        uncommittedChanges = status.uncommittedChanges,
        untracked = status.untracked,
        missing = status.missing,
        hasCleanWorkingTree = status.isClean
      )
    } catch (e: Exception) {
      log.error("Failed to query git status", e)
      null
    } finally {
      git.close()
    }
  }

  fun commit(projectDir: File, message: String, addAll: Boolean = true): Boolean {
    val git = openRepository(projectDir) ?: return false
    return try {
      if (addAll) {
        git.add().addFilepattern(".").call()
      }
      git.commit().setMessage(message).call()
      log.info("Git commit successful: {}", message)
      true
    } catch (e: Exception) {
      log.error("Git commit failed", e)
      false
    } finally {
      git.close()
    }
  }

  fun getCommitHistory(projectDir: File, limit: Int = 30): List<GitCommitInfo> {
    val git = openRepository(projectDir) ?: return emptyList()
    val list = mutableListOf<GitCommitInfo>()
    return try {
      val commits = git.log().setMaxCount(limit).call()
      for (commit in commits) {
        list.add(
          GitCommitInfo(
            hash = commit.name,
            shortHash = commit.name.take(7),
            author = commit.authorIdent.name,
            message = commit.shortMessage,
            timestamp = commit.commitTime.toLong() * 1000
          )
        )
      }
      list
    } catch (e: Exception) {
      log.error("Failed to retrieve git log", e)
      emptyList()
    } finally {
      git.close()
    }
  }

  fun listBranches(projectDir: File): Pair<String, List<String>> {
    val git = openRepository(projectDir) ?: return "unknown" to emptyList()
    return try {
      val current = git.repository.branch ?: "main"
      val branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map {
        it.name.removePrefix("refs/heads/").removePrefix("refs/remotes/")
      }.distinct()
      current to branches
    } catch (e: Exception) {
      "error" to emptyList()
    } finally {
      git.close()
    }
  }

  fun checkout(projectDir: File, branchName: String, createNew: Boolean = false): Boolean {
    val git = openRepository(projectDir) ?: return false
    return try {
      val cmd = git.checkout().setName(branchName)
      if (createNew) {
        cmd.setCreateBranch(true)
      }
      cmd.call()
      log.info("Checked out branch {}", branchName)
      true
    } catch (e: Exception) {
      log.error("Failed to checkout branch {}", branchName, e)
      false
    } finally {
      git.close()
    }
  }

  fun pull(projectDir: File, username: String? = null, tokenOrPass: String? = null): Pair<Boolean, String> {
    val git = openRepository(projectDir) ?: return false to "Not a git repository"
    return try {
      val cmd = git.pull()
      if (!username.isNullOrBlank() && !tokenOrPass.isNullOrBlank()) {
        cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, tokenOrPass))
      }
      val pullResult = cmd.call()
      pullResult.isSuccessful to "Pull successful"
    } catch (e: Exception) {
      false to (e.message ?: "Git pull error")
    } finally {
      git.close()
    }
  }

  fun push(projectDir: File, remote: String = "origin", branch: String? = null, username: String? = null, tokenOrPass: String? = null): Pair<Boolean, String> {
    val git = openRepository(projectDir) ?: return false to "Not a git repository"
    return try {
      val cmd = git.push().setRemote(remote)
      if (!username.isNullOrBlank() && !tokenOrPass.isNullOrBlank()) {
        cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, tokenOrPass))
      }
      val pushResults = cmd.call()
      true to "Push completed"
    } catch (e: Exception) {
      false to (e.message ?: "Git push error")
    } finally {
      git.close()
    }
  }

  fun diff(projectDir: File): String {
    val git = openRepository(projectDir) ?: return ""
    return try {
      val out = ByteArrayOutputStream()
      val formatter = DiffFormatter(out)
      formatter.setRepository(git.repository)
      val diffs = git.diff().setOutputStream(out).call()
      out.toString()
    } catch (e: Exception) {
      ""
    } finally {
      git.close()
    }
  }
}
