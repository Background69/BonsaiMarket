"""Run only the supplied baseline, record predictions without claiming accuracy."""
import json
import platform
import joblib
import sklearn
import numpy
import scipy
from server import load_bundle

model, version, metadata = load_bundle()
texts = [
    "Cây đẹp, tán đều và khỏe mạnh. Tôi rất hài lòng.",
    "Chậu bị nứt, lá héo nhiều. Trải nghiệm rất thất vọng.",
    "Tôi nhận cây chiều nay, đang để ở ban công theo dõi thêm.",
    "Cây đẹp và đúng dáng nhưng hộp móp, giao trễ hai ngày.",
    "Đóng gói cẩn thận, rễ không bị tổn thương sau vận chuyển.",
    "Cây nhỏ hơn mô tả, tuy vậy người bán hướng dẫn chăm sóc khá tận tình.",
]
labels = [str(x) for x in model.predict(texts)]
assert set(labels) <= {"positive", "neutral", "negative", "mixed"}
print(json.dumps({
    "boundary": "REAL_MODEL_CPU_SMOKE_ONLY; train_only synthetic; no independent quality score",
    "modelVersion": version, "metadata": metadata,
    "runtime": {"python": platform.python_version(), "sklearn": sklearn.__version__, "joblib": joblib.__version__, "numpy": numpy.__version__, "scipy": scipy.__version__},
    "pipelineSteps": [name + ":" + type(step).__name__ for name, step in model.steps],
    "predictions": [{"text": text, "label": label} for text, label in zip(texts, labels)]
}, ensure_ascii=False, indent=2))
