"""Loopback-only CPU inference for the owner's local TF-IDF bundle. No remote model IO."""
import hashlib
import json
import os
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
import secrets
import signal
import warnings

LABELS = {"positive", "neutral", "negative", "mixed"}


def load_bundle():
    # Only the explicitly configured, owner-created package is deserialized.
    import joblib
    import sklearn
    from sklearn.exceptions import InconsistentVersionWarning
    root = Path(os.environ["SENTIMENT_MODEL_DIR"]).resolve(strict=True)
    matches = list(root.rglob("tfidf_logreg.joblib"))
    if len(matches) != 1:
        raise ValueError("Expected exactly one tfidf_logreg.joblib in SENTIMENT_MODEL_DIR; extract ZIP first")
    model_path = matches[0]
    metadata = json.loads(model_path.with_name("metrics.json").read_text(encoding="utf-8-sig"))
    if metadata.get("sklearn") != sklearn.__version__:
        raise ValueError("scikit-learn version must match metrics.json")
    if set(metadata.get("labels", [])) != LABELS:
        raise ValueError("Unexpected bundle labels")
    demo = metadata.get("evaluation_mode") == "train_only" or set(metadata.get("training_sources", {})) == {"synthetic"}
    if demo and os.environ.get("SENTIMENT_ALLOW_DEMO") != "true":
        raise ValueError("This bundle requires SENTIMENT_ALLOW_DEMO=true; no independent test quality claim")
    with warnings.catch_warnings():
        warnings.simplefilter("error", InconsistentVersionWarning)
        model = joblib.load(model_path)
    if not hasattr(model, "predict") or set(str(x) for x in model.classes_) != LABELS:
        raise ValueError("Expected a fitted text pipeline with the four named labels")
    version = "tfidf-logreg-" + hashlib.sha256(model_path.read_bytes()).hexdigest()
    return model, version, metadata


def main():
    def stop(_signum, _frame):
        raise SystemExit(0)

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    model, version, metadata = load_bundle()
    token = os.environ.get("SENTIMENT_INTERNAL_TOKEN", "")
    if len(token) < 32:
        raise ValueError("SENTIMENT_INTERNAL_TOKEN must contain at least 32 characters")

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass  # Never log review text or credentials.

        def respond(self, status, payload):
            raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(raw)))
            self.end_headers()
            self.wfile.write(raw)

        def do_POST(self):
            self.connection.settimeout(5)
            if self.path != "/predict" or not secrets.compare_digest(self.headers.get("Authorization", ""), "Bearer " + token):
                return self.respond(403, {"error": "FORBIDDEN"})
            try:
                length = int(self.headers.get("Content-Length", "0"))
                if not 0 < length <= 16000:
                    return self.respond(413, {"error": "INVALID_LENGTH"})
                body = json.loads(self.rfile.read(length).decode("utf-8"))
                if set(body) != {"text"} or not isinstance(body["text"], str) or not 1 <= len(body["text"].strip()) <= 2000:
                    return self.respond(400, {"error": "INVALID_INPUT"})
                label = str(model.predict([body["text"]])[0])
                if label not in LABELS:
                    raise ValueError("Invalid prediction")
                self.respond(200, {"label": label, "modelVersion": version,
                                   "evaluationMode": metadata["evaluation_mode"],
                                   "demoOnly": metadata.get("independent_test_provided") is not True})
            except (ValueError, TypeError, UnicodeError):
                self.respond(400, {"error": "INVALID_INPUT_OR_OUTPUT"})
            except Exception:
                self.respond(503, {"error": "INFERENCE_UNAVAILABLE"})

    print(json.dumps({"modelVersion": version, "evaluationMode": metadata["evaluation_mode"], "device": "cpu"}), flush=True)
    host = os.environ.get("SENTIMENT_BIND_HOST", "127.0.0.1")
    if host not in {"127.0.0.1", "0.0.0.0"}:
        raise ValueError("Unsupported bind host")
    with HTTPServer((host, int(os.environ.get("SENTIMENT_PORT", "8091"))), Handler) as server:
        server.serve_forever()


if __name__ == "__main__":
    main()
