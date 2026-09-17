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
 *  along with AIDE Next.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aidenext.lsp.kotlin

import com.aidenext.lsp.api.ILanguageClient
import com.aidenext.lsp.api.ILanguageServer
import com.aidenext.lsp.api.IServerSettings
import com.aidenext.lsp.models.CodeFormatResult
import com.aidenext.lsp.models.CompletionParams
import com.aidenext.lsp.models.CompletionResult
import com.aidenext.lsp.models.DefinitionParams
import com.aidenext.lsp.models.DefinitionResult
import com.aidenext.lsp.models.DiagnosticResult
import com.aidenext.lsp.models.ExpandSelectionParams
import com.aidenext.lsp.models.FormatCodeParams
import com.aidenext.lsp.models.LSPFailure
import com.aidenext.lsp.models.ReferenceParams
import com.aidenext.lsp.models.ReferenceResult
import com.aidenext.lsp.models.SignatureHelp
import com.aidenext.lsp.models.SignatureHelpParams
import com.aidenext.models.Range
import com.aidenext.projects.IWorkspace
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * On-device Kotlin syntactic & snippet language provider implementing [ILanguageServer].
 *
 * Provides:
 * - Syntactic and template completion (Kotlin keywords, Compose primitives, Android APIs, local
 *   document symbols)
 * - Syntax-level diagnostics (bracket balance, unclosed string literals)
 * - Document formatting and indentation
 *
 * Note: Full semantic type resolution, project-wide symbol indexing, and type-aware refactoring
 * require a dedicated compiler analysis daemon (such as Kotlin Analysis API or external LSP),
 * which is planned as a future heavyweight extension.
 *
 * @author AIDE Next
 */
class KotlinLanguageServer : ILanguageServer {

  companion object {
    const val SERVER_ID = "ide.lsp.kotlin"
    private val log = LoggerFactory.getLogger(KotlinLanguageServer::class.java)
  }

  override val serverId: String = SERVER_ID

  override var client: ILanguageClient? = null
    private set

  private var settings: IServerSettings? = null

  private var workspace: IWorkspace? = null

  override fun shutdown() {
    log.info("Shutting down {}", SERVER_ID)
    client = null
    workspace = null
    settings = null
  }

  override fun connectClient(client: ILanguageClient?) {
    this.client = client
  }

  override fun applySettings(settings: IServerSettings?) {
    this.settings = settings
  }

  override fun setupWorkspace(workspace: IWorkspace) {
    this.workspace = workspace
    log.info("KotlinLanguageServer initialized for workspace: {}", workspace.getProjectDir())
  }

  override fun complete(params: CompletionParams?): CompletionResult {
    if (params == null) {
      return CompletionResult.EMPTY
    }

    return runCatching { KotlinCompletionProvider.complete(params) }
      .onFailure { log.error("Failed to compute completions", it) }
      .getOrDefault(CompletionResult.EMPTY)
  }

  override suspend fun findReferences(params: ReferenceParams): ReferenceResult {
    // Full project-wide reference search requires the symbol index, which is not available yet.
    return ReferenceResult(emptyList())
  }

  override suspend fun findDefinition(params: DefinitionParams): DefinitionResult {
    // Basic symbol search inside the file is not implemented yet.
    return DefinitionResult(emptyList())
  }

  override suspend fun expandSelection(params: ExpandSelectionParams): Range {
    return params.selection
  }

  override suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp {
    return SignatureHelp(emptyList(), -1, -1)
  }

  override suspend fun analyze(file: Path): DiagnosticResult {
    return runCatching { KotlinDiagnosticProvider.analyze(file) }
      .onFailure { log.error("Failed to analyze file: {}", file, it) }
      .getOrDefault(DiagnosticResult(file, emptyList()))
  }

  override fun formatCode(params: FormatCodeParams?): CodeFormatResult {
    return runCatching { KotlinFormatProvider.format(params) }
      .onFailure { log.error("Failed to format Kotlin source", it) }
      .getOrDefault(CodeFormatResult.NONE)
  }

  override fun handleFailure(failure: LSPFailure?): Boolean {
    log.error("LSP failure: {} ({})", failure?.type, failure?.error?.message)
    return false
  }
}
