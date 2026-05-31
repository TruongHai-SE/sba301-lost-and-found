from PIL import Image
from ultralytics import YOLO


class YOLODetector:
    """YOLOv8 Nano - crop the main object from an image."""

    def __init__(self, weights: str = "yolov8n.pt"):
        print(f"[YOLO] Loading {weights} ...")
        self.model = YOLO(weights)

    def crop_main_object(self, img: Image.Image, padding: int = 15) -> Image.Image:
        """Detect and crop the most prominent object, or return the original."""
        results = self.model(img, verbose=False)

        if not results or not results[0].boxes or len(results[0].boxes) == 0:
            return img

        boxes = results[0].boxes.xyxy.cpu().numpy()
        confidences = results[0].boxes.conf.cpu().numpy()

        img_cx, img_cy = img.width / 2, img.height / 2
        img_area = img.width * img.height
        max_dist = ((img.width / 2) ** 2 + (img.height / 2) ** 2) ** 0.5

        best_box, best_score = None, -float("inf")

        for box, conf in zip(boxes, confidences):
            if conf < 0.25:
                continue
            x1, y1, x2, y2 = box
            area_ratio = ((x2 - x1) * (y2 - y1)) / img_area
            dist_ratio = (
                ((x1 + x2) / 2 - img_cx) ** 2 + ((y1 + y2) / 2 - img_cy) ** 2
            ) ** 0.5 / max_dist

            score = area_ratio * 1.0 - dist_ratio * 2.0
            if score > best_score:
                best_score = score
                best_box = box

        if best_box is None:
            return img

        x1, y1, x2, y2 = map(int, best_box)
        x1, y1 = max(0, x1 - padding), max(0, y1 - padding)
        x2, y2 = min(img.width, x2 + padding), min(img.height, y2 + padding)
        return img.crop((x1, y1, x2, y2))
