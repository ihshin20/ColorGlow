import cv2
import dlib
import numpy as np
import os
from pathlib import Path

def resize_image_keep_ratio(img, output_size=(224, 224), fill_color=(0, 0, 0)):
    h, w = img.shape[:2]

    if h == 0 or w == 0:
        # 이미지의 높이나 너비가 0인 경우, 처리를 건너뛰기
        print("Image width or height is 0, skipping resize.")
        return img

    scale = min(output_size[0] / h, output_size[1] / w)
    new_w = int(w * scale)
    new_h = int(h * scale)

    # new_w 또는 new_h가 0이 되지 않도록 검증
    new_w = max(1, new_w)
    new_h = max(1, new_h)

    resized_img = cv2.resize(img, (new_w, new_h))

    new_img = np.full((output_size[0], output_size[1], 3), fill_color, dtype=np.uint8)
    left = (output_size[1] - new_w) // 2
    top = (output_size[0] - new_h) // 2
    new_img[top:top+new_h, left:left+new_w] = resized_img
    return new_img

# 랜드마크 모델 및 폴더 경로 설정
# https://github.com/italojs/facial-landmarks-recognition/blob/master/shape_predictor_68_face_landmarks.dat 에서 download
predictor_path = "C:\\Crawling\\shape_predictor_68_face_landmarks.dat" # predictor dir
folder_path = "C:\\Crawling\\preprocessing\\test\\meme" # 처리해야 하는 이미지의 dir
save_path = "C:\\Crawling\\preprocessing\\test\\crop" # 처리 후 저장할 dir

# dlib의 얼굴 감지기 및 랜드마크 감지기 초기화
detector = dlib.get_frontal_face_detector()
predictor = dlib.shape_predictor(predictor_path)

# 결과 폴더 생성 (존재하지 않는 경우)
if not os.path.exists(save_path):
    os.makedirs(save_path)

# 폴더 내 모든 jpg 이미지에 대해 처리
for image_path in Path(folder_path).glob('*.jpg'):
    img = cv2.imread(str(image_path))

    if img is None:
        print(f"Failed to load image: {image_path}, skipping.")
        continue

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    faces = detector(gray)

    # 한 사람의 얼굴만 처리
    if len(faces) != 1:
        print(f"Expected 1 face, found {len(faces)} in: {image_path}, skipping.")
        continue

    for k, d in enumerate(faces):
        shape = predictor(gray, d)
        points = np.array([[p.x, p.y] for p in shape.parts()])
        convexhull = cv2.convexHull(points)
        
        mask = np.zeros(gray.shape[:2], dtype=np.uint8)
        cv2.drawContours(mask, [convexhull], -1, (255), cv2.FILLED)
        
        face_img = cv2.bitwise_and(img, img, mask=mask)
        
        # 얼굴 영역만 추출하여 저장
        (x, y, w, h) = cv2.boundingRect(convexhull)
        if w == 0 or h == 0:
            print(f"Invalid face dimensions in: {image_path}, skipping.")
            continue
        cropped_face = face_img[y:y+h, x:x+w]

        # 추가 검사
        if cropped_face.size == 0:
            print(f"No data in cropped face for: {image_path}, skipping.")
            continue

        # 크기 조절
        resized_face = resize_image_keep_ratio(cropped_face)

        # 조절된 크기의 이미지 저장
        cv2.imwrite(os.path.join(save_path, image_path.name), resized_face)

print("Processing completed.")