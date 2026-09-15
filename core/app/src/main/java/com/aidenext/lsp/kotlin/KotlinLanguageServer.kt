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

package com.aidenext.lsp.kotlin

import com.aidenext.lsp.api.ICompletionProvider
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
import java.nio.file.Files
import java.nio.file.Path

/**
 * On-device Kotlin Language Server providing completion, diagnostics, formatting, and definitions.
 */
class KotlinLanguageServer : ILanguageServer {

  companion object {
    const val SERVER_ID = "ide.lsp.kotlin"
    private val log = LoggerFactory.getLogger(KotlinLanguageServer::class.java)
  }

  override val serverId: String = SERVER_ID
  override val completionProvider: ICompletionProvider? = null

  private var client: ILanguageClient? = null
  private var workspace: IWorkspace? = null

  override suspend fun initialize(workspace: IWorkspace) {
    this.workspace = workspace
    log.info("KotlinLanguageServer initialized for workspace: {}", workspace.rootDirectory)
  }

  override fun connectClient(client: ILanguageClient) {
    this.client = client
  }

  override fun disconnectClient() {
    this.client = null
  }

  override suspend fun destroy() {
    this.client = null
    this.workspace = null
  }

  override suspend fun didOpen(file: Path) {
    log.debug("KotlinLanguageServer didOpen: {}", file)
    val diag = analyze(file)
    client?.publishDiagnostics(file, diag)
  }

  override suspend fun didChange(file: Path) {
    val diag = analyze(file)
    client?.publishDiagnostics(file, diag)
  }

  override suspend fun didClose(file: Path) {
    log.debug("KotlinLanguageServer didClose: {}", file)
  }

  override suspend fun didSave(file: Path) {
    val diag = analyze(file)
    client?.publishDiagnostics(file, diag)
  }

  override suspend fun complete(params: CompletionParams): CompletionResult {
    return KotlinCompletionProvider.complete(params)
  }

  override suspend fun analyze(file: Path): DiagnosticResult {
    return KotlinDiagnosticProvider.analyze(file)
  }

  override fun formatCode(params: FormatCodeParams?): CodeFormatResult {
    return KotlinFormatProvider.format(params)
  }

  override suspend fun findDefinition(params: DefinitionParams): DefinitionResult {
    // Basic symbol search in file
    return DefinitionResult(emptyList())
  }

  override suspend fun findReferences(params: ReferenceParams): ReferenceResult {
    return ReferenceResult(emptyList())
  }

  override suspend fun expandSelection(params: ExpandSelectionParams): Range {
    return Range(params.position, params.position)
  }

  override suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp {
    return SignatureHelp(emptyList(), 0, 0)
  }

  override fun handleFailure(failure: LSPFailure?): Boolean {
    return false
  }
}
