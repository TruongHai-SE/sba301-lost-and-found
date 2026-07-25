# ============================================================
#  Copy tung cell ben duoi vao Google Colab, chay theo thu tu
# ============================================================

# === CELL 1: Config ===
TUNNEL_MODE = "named"  # @param ["named", "quick"]

CLOUDFLARE_TOKEN = "eyJhIjoiZmUyY2E0MGM1OWIwNWM4OTM0Y2MxYmFmMjQ4NjI1ZTEiLCJ0IjoiMDAyNjhlZDQtMDkyNi00ZDk0LWJmMzQtOTQxMWQyY2RkZDZlIiwicyI6Ik9UQmtOR0k1TXpndE9URXpNUzAwWXpBM0xUa3lNMlF0T0dabVptWTVZak16T1RSaiJ9"

MODEL_NAME = "qwen2.5vl:3b"  # @param ["qwen2.5vl:3b", "qwen2.5:1.5b", "qwen2.5:3b", "qwen2.5:7b", "qwen2.5:14b", "qwen2.5-coder:7b"]

OLLAMA_HOST = "0.0.0.0"
OLLAMA_PORT = 11434

print(f"Tunnel mode : {TUNNEL_MODE}")
print(f"Model       : {MODEL_NAME}")
print(f"Ollama host : {OLLAMA_HOST}:{OLLAMA_PORT}")


# === CELL 2: Install Ollama ===
!sudo apt-get update -qq && sudo apt-get install -y -qq curl zstd > /dev/null
!curl -fsSL https://ollama.com/install.sh | sh
!ollama --version


# === CELL 3: Start Ollama server ===
import subprocess, time, os

!pkill ollama 2>/dev/null || true

process = subprocess.Popen(
    ["ollama", "serve"],
    env={**os.environ, "OLLAMA_HOST": f"{OLLAMA_HOST}:{OLLAMA_PORT}"},
    stdout=open("/tmp/ollama.log", "w"),
    stderr=subprocess.STDOUT,
)

print("Waiting for Ollama server...")
for i in range(30):
    try:
        import urllib.request
        urllib.request.urlopen(f"http://localhost:{OLLAMA_PORT}")
        print("Ollama server is ready!")
        break
    except Exception:
        time.sleep(1)
else:
    print("Failed to start Ollama server")


# === CELL 4: Pull Qwen model ===
!ollama pull {MODEL_NAME}
print(f"Pulled model: {MODEL_NAME}")


# === CELL 5: Quick test ===
!ollama run {MODEL_NAME} "Hello, who are you?"


# === CELL 6: Install & run Cloudflare Tunnel ===
import subprocess, time, re

!curl -fsSL https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o /usr/local/bin/cloudflared
!chmod +x /usr/local/bin/cloudflared
!cloudflared --version

print(f"\nStarting Cloudflare Tunnel (mode: {TUNNEL_MODE})...")

tunnel_url = None

if TUNNEL_MODE == "named":
    print("Running: cloudflared tunnel run --token <TOKEN>")
    tunnel = subprocess.Popen(
        ["cloudflared", "tunnel", "run", "--token", CLOUDFLARE_TOKEN],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    start = time.time()
    while time.time() - start < 30:
        line = tunnel.stdout.readline()
        if line:
            print(line, end="")
        if "Registered tunnel connection" in line:
            print("\nTunnel connected! Access via your public hostname.")
            break

elif TUNNEL_MODE == "quick":
    print(f"Running: cloudflared tunnel --url localhost:{OLLAMA_PORT}")
    tunnel = subprocess.Popen(
        ["cloudflared", "tunnel", "--url", f"http://localhost:{OLLAMA_PORT}"],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    start = time.time()
    while time.time() - start < 60:
        line = tunnel.stdout.readline()
        if line:
            print(line, end="")
            match = re.search(r'https://[a-zA-Z0-9.-]+\.trycloudflare\.com', line)
            if match:
                tunnel_url = match.group(0)
                break

    if tunnel_url:
        print(f"\n{'='*60}")
        print("Tunnel is ready!")
        print(f"API URL: {tunnel_url}")
        print(f"{'='*60}")
        print("\nUsage examples:")
        print(f"  curl {tunnel_url}/api/tags")
    else:
        print("Failed to get tunnel URL")

else:
    print(f"Invalid TUNNEL_MODE: {TUNNEL_MODE}. Use 'named' or 'quick'.")


# === CELL 7: (Optional) Test API via tunnel ===
import requests

if TUNNEL_MODE == "quick" and tunnel_url:
    response = requests.post(
        f"{tunnel_url}/api/chat",
        json={
            "model": MODEL_NAME,
            "messages": [{"role": "user", "content": "Say hello in 3 words."}],
            "stream": False,
        },
    )
    print(response.json()["message"]["content"])
elif TUNNEL_MODE == "named":
    print("For named tunnel, call your public hostname directly.")
else:
    print("Tunnel not ready, re-run cell 6.")


# === CELL 8: Keep-alive ===
import time, itertools

print("Keep-alive loop running. Do not stop this cell.")
spinner = itertools.cycle(["-", "\\", "|", "/"])
try:
    while True:
        print(f"\r{next(spinner)} Active - {time.strftime('%H:%M:%S')}", end="")
        time.sleep(5)
except KeyboardInterrupt:
    print("\nDone.")
