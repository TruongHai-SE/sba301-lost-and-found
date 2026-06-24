@echo off
echo ============================================
echo   SBA301 Lost and Found - CLIP Service
echo ============================================

:: 1. Start PostgreSQL (if not running)
echo [1/4] Checking PostgreSQL...
docker compose -f "%~dp0..\..\docker-compose.yml" up -d postgres
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker failed! Make sure Docker Desktop is running.
    pause
    exit /b 1
)

:: Wait for DB to be ready
echo [1/4] Waiting for PostgreSQL to be ready...
ping 127.0.0.1 -n 6 > nul

:: 2. Check local model files
echo [2/4] Checking local model files...
if not exist "%~dp0..\..\clip-service\.local\models\clip_onnx_large_model\model_quantized.onnx" (
    echo [ERROR] Missing clip-service\.local\models\clip_onnx_large_model\model_quantized.onnx
    echo         Run: cd clip-service && .venv\Scripts\python.exe scripts\export_and_prepare_models.py
    pause
    exit /b 1
)
if not exist "%~dp0..\..\clip-service\.local\models\opus_mt_vi_en_onnx\pytorch_model.bin" (
    echo [ERROR] Missing clip-service\.local\models\opus_mt_vi_en_onnx\pytorch_model.bin
    echo         Run: cd clip-service && .venv\Scripts\python.exe scripts\export_and_prepare_models.py
    pause
    exit /b 1
)
if not exist "%~dp0..\..\clip-service\.local\models\yolov8n.pt" (
    echo [ERROR] Missing clip-service\.local\models\yolov8n.pt
    echo         Run: cd clip-service && .venv\Scripts\python.exe scripts\export_and_prepare_models.py
    pause
    exit /b 1
)

:: 3. Select Python executable
echo [3/4] Setting up Python environment...
if exist "%~dp0..\..\clip-service\.venv\Scripts\python.exe" (
    set "PYTHON_EXE=%~dp0..\..\clip-service\.venv\Scripts\python.exe"
    echo       Using virtual environment
) else if exist "%LocalAppData%\Python\bin\python.exe" (
    set "PYTHON_EXE=%LocalAppData%\Python\bin\python.exe"
    echo       Using Python Install Manager
) else (
    set "PYTHON_EXE=python"
    echo       Using Python from PATH
)

:: 4. Start CLIP Service
echo [4/4] Starting CLIP Service on port 8000...
echo       Swagger UI: http://localhost:8000/docs
echo ============================================
cd /d "%~dp0..\..\clip-service"
"%PYTHON_EXE%" -B -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
