FROM python:3.12-slim

# Tạo user không phải root với UID 1000 theo chuẩn Hugging Face
RUN useradd -m -u 1000 user
WORKDIR /app

# Cài đặt các thư viện hệ thống cần cho OpenCV, Pillow và YOLO
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements và cài đặt
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy toàn bộ mã nguồn ứng dụng và các file scripts
COPY . .

# Tạo thư mục lưu model
RUN mkdir -p .local/models

# Khai báo các biến môi trường để override cấu hình đường dẫn tương đối mặc định
ENV CLIP_MODEL_DIR=/app/.local/models/clip_onnx_large_model
ENV YOLO_MODEL_PATH=/app/.local/models/yolov8n.pt
ENV TRANSLATION_MODEL_DIR=/app/.local/models/opus_mt_vi_en_onnx

# Chạy script tải và quantize mô hình ngay lúc build
# để Space khởi động nhanh hơn, tránh timeout khi cold start
RUN python scripts/export_and_prepare_models.py

# Phân quyền sở hữu toàn bộ thư mục app cho user 1000
RUN chown -R user:user /app

USER user
ENV HOME=/home/user
ENV PATH=/home/user/.local/bin:$PATH

# Hugging Face yêu cầu chạy cổng 7860
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "7860"]
