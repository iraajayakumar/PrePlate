"""Model bundle metadata shared by training and inference."""
from importlib.metadata import version

FORMAT_VERSION = 1
MODEL_VERSION = "preplate-ml-v1"
DEFAULT_BUNDLE = "forecast.joblib"

def model_dependencies():
    versions = {name: version(name) for name in
                ("numpy", "scipy", "pandas", "scikit-learn", "joblib")}
    import xgboost
    versions["xgboost"] = xgboost.__version__
    return versions
