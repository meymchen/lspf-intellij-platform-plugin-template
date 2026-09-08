//! A small, production-shaped language server served over stdio.

mod log_format;

use std::sync::Arc;

use lspf::types::{
    CompletionItem, CompletionItemKind, CompletionParams, CompletionResponse, Contents, Diagnostic,
    DiagnosticSeverity, DidOpenTextDocumentNotification as DidOpenTextDocument,
    DidOpenTextDocumentParams, Hover, HoverParams, MarkupContent, MarkupKind, Position,
    PublishDiagnosticsNotification as PublishDiagnostics, PublishDiagnosticsParams, Range,
};
use lspf::{CancellationToken, LspError, Server, ServerContext};
use tracing::warn;

/// Application-owned state shared by all handlers.
///
/// Put parsed syntax trees, indexes, or configuration here. Documents and the
/// client connection remain available through `ServerContext`.
struct State;

async fn did_open(_state: Arc<State>, ctx: ServerContext, params: DidOpenTextDocumentParams) {
    let uri = params.text_document.uri;
    let Some(document) = ctx.documents().get(&uri) else {
        return;
    };

    let diagnostics = PublishDiagnosticsParams {
        uri,
        version: document.version(),
        diagnostics: vec![Diagnostic {
            range: Range {
                start: Position {
                    line: 0,
                    character: 0,
                },
                end: Position {
                    line: 0,
                    character: 0,
                },
            },
            severity: Some(DiagnosticSeverity::Information),
            source: Some(env!("CARGO_PKG_NAME").into()),
            message: "Your lspf language server is running".into(),
            ..Diagnostic::default()
        }],
    };

    if let Err(error) = ctx.client().notify::<PublishDiagnostics>(diagnostics) {
        warn!(%error, "failed to publish diagnostics");
    }
}

async fn hover(
    _state: Arc<State>,
    ctx: ServerContext,
    params: HoverParams,
    _cancellation: CancellationToken,
) -> Result<Option<Hover>, LspError> {
    let uri = &params.text_document_position_params.text_document.uri;
    let Some(document) = ctx.documents().get(uri) else {
        return Ok(None);
    };

    Ok(Some(Hover {
        contents: Contents::MarkupContent(MarkupContent {
            kind: MarkupKind::Markdown,
            value: format!(
                "**{}** is tracking this document as `{}`.",
                env!("CARGO_PKG_NAME"),
                document.language_id()
            ),
        }),
        range: None,
    }))
}

async fn completion(
    _state: Arc<State>,
    _ctx: ServerContext,
    _params: CompletionParams,
    _cancellation: CancellationToken,
) -> Result<Option<CompletionResponse>, LspError> {
    Ok(Some(CompletionResponse::CompletionItemList(vec![
        CompletionItem {
            label: "hello".into(),
            kind: Some(CompletionItemKind::Keyword),
            detail: Some("Example completion from lspf".into()),
            ..CompletionItem::default()
        },
    ])))
}

#[tokio::main]
async fn main() -> lspf::Result<()> {
    // stdout belongs exclusively to the LSP wire protocol.
    log_format::init();

    let server = Server::builder(State)
        .feature(lspf::features::hover(), hover)
        .feature(lspf::features::completion(Default::default()), completion)
        .notification::<DidOpenTextDocument, _, _>(did_open)
        .build()
        .expect("the static registrations are valid");

    let outcome = lspf::stdio(server).serve().await?;
    std::process::exit(outcome.code());
}
