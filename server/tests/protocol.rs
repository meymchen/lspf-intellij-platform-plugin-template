use std::path::PathBuf;
use std::process::Stdio;
use std::time::Duration;

use serde_json::{Value, json};
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader};
use tokio::process::{ChildStdin, ChildStdout, Command};

fn server_binary() -> PathBuf {
    PathBuf::from(env!("CARGO_BIN_EXE_lspf-hello"))
}

async fn write_message(stdin: &mut ChildStdin, message: Value) {
    let body = message.to_string();
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    stdin.write_all(header.as_bytes()).await.unwrap();
    stdin.write_all(body.as_bytes()).await.unwrap();
    stdin.flush().await.unwrap();
}

async fn read_message(stdout: &mut BufReader<ChildStdout>) -> Value {
    tokio::time::timeout(Duration::from_secs(10), async {
        let mut content_length = None;
        loop {
            let mut line = String::new();
            assert!(stdout.read_line(&mut line).await.unwrap() > 0);
            if line == "\r\n" {
                break;
            }
            if let Some(value) = line.strip_prefix("Content-Length: ") {
                content_length = Some(value.trim().parse::<usize>().unwrap());
            }
        }
        let mut body = vec![0; content_length.expect("Content-Length header")];
        stdout.read_exact(&mut body).await.unwrap();
        serde_json::from_slice(&body).unwrap()
    })
    .await
    .expect("server responded within 10 seconds")
}

#[tokio::test]
async fn editor_lifecycle_and_diagnostic_round_trip() {
    let mut child = Command::new(server_binary())
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .kill_on_drop(true)
        .spawn()
        .expect("spawn language server");
    let mut stdin = child.stdin.take().unwrap();
    let mut stdout = BufReader::new(child.stdout.take().unwrap());

    write_message(
        &mut stdin,
        json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "processId": null,
                "rootUri": null,
                "capabilities": {},
                "workspaceFolders": null
            }
        }),
    )
    .await;
    let initialized = read_message(&mut stdout).await;
    assert_eq!(initialized["id"], 1);
    assert_eq!(initialized["result"]["capabilities"]["textDocumentSync"], 2);

    write_message(
        &mut stdin,
        json!({"jsonrpc": "2.0", "method": "initialized", "params": {}}),
    )
    .await;
    write_message(
        &mut stdin,
        json!({
            "jsonrpc": "2.0",
            "method": "textDocument/didOpen",
            "params": {
                "textDocument": {
                    "uri": "file:///tmp/template.hello",
                    "languageId": "hello",
                    "version": 1,
                    "text": "hello\n"
                }
            }
        }),
    )
    .await;

    let diagnostic = read_message(&mut stdout).await;
    assert_eq!(diagnostic["method"], "textDocument/publishDiagnostics");
    assert_eq!(
        diagnostic["params"]["diagnostics"][0]["source"],
        env!("CARGO_PKG_NAME")
    );

    write_message(
        &mut stdin,
        json!({
            "jsonrpc": "2.0", "id": 3, "method": "textDocument/hover",
            "params": {
                "textDocument": {"uri": "file:///tmp/template.hello"},
                "position": {"line": 0, "character": 1}
            }
        }),
    )
    .await;
    let hover = read_message(&mut stdout).await;
    assert_eq!(hover["id"], 3);
    assert!(
        hover["result"]["contents"]["value"]
            .as_str()
            .unwrap()
            .contains(env!("CARGO_PKG_NAME"))
    );

    write_message(
        &mut stdin,
        json!({
            "jsonrpc": "2.0", "id": 4, "method": "textDocument/completion",
            "params": {
                "textDocument": {"uri": "file:///tmp/template.hello"},
                "position": {"line": 0, "character": 1}
            }
        }),
    )
    .await;
    let completion = read_message(&mut stdout).await;
    assert_eq!(completion["id"], 4);
    assert_eq!(completion["result"][0]["label"], "hello");

    write_message(
        &mut stdin,
        json!({"jsonrpc": "2.0", "id": 2, "method": "shutdown"}),
    )
    .await;
    assert_eq!(read_message(&mut stdout).await["id"], 2);
    write_message(&mut stdin, json!({"jsonrpc": "2.0", "method": "exit"})).await;
    drop(stdin);

    let status = tokio::time::timeout(Duration::from_secs(5), child.wait())
        .await
        .expect("server exited")
        .unwrap();
    assert_eq!(status.code(), Some(0));
}
