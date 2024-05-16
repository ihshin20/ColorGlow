import sys
import cv2
import dlib
import numpy as np
import os
import shutil
from pathlib import Path
from tensorflow.keras.preprocessing import image
from tensorflow.keras.models import load_model


errorHandler = 0

def resize_image_keep_ratio(img, output_size=(224, 224), fill_color=(0, 0, 0)):
    h, w = img.shape[:2]
    scale = min(output_size[0] / h, output_size[1] / w)
    new_w = int(w * scale)
    new_h = int(h * scale)
    resized_img = cv2.resize(img, (new_w, new_h))

    new_img = np.full((output_size[0], output_size[1], 3), fill_color, dtype=np.uint8)
    left = (output_size[1] - new_w) // 2
    top = (output_size[0] - new_h) // 2
    new_img[top:top+new_h, left:left+new_w] = resized_img
    return new_img

def process_single_image(image_path, save_path):
    predictor_path = "shape_predictor_68_face_landmarks.dat"
    global errorHandler

    # dlib의 얼굴 감지기 및 랜드마크 감지기 초기화
    detector = dlib.get_frontal_face_detector()
    predictor = dlib.shape_predictor(predictor_path)

    img = cv2.imread(image_path)
    if img is None:
        print("error")
        errorHandler = 1
        return

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    faces = detector(gray)

    if len(faces) != 1:
        print("1face")
        errorHandler = 1
        return

    for k, d in enumerate(faces):
        shape = predictor(gray, d)
        points = np.array([[p.x, p.y] for p in shape.parts()])
        convexhull = cv2.convexHull(points)
        
        mask = np.zeros(gray.shape[:2], dtype=np.uint8)
        cv2.drawContours(mask, [convexhull], -1, (255), cv2.FILLED)
        
        face_img = cv2.bitwise_and(img, img, mask=mask)
        
        (x, y, w, h) = cv2.boundingRect(convexhull)
        cropped_face = face_img[y:y+h, x:x+w]

        resized_face = resize_image_keep_ratio(cropped_face)

        cv2.imwrite(os.path.join(save_path, os.path.basename(image_path)), resized_face)


def main(image_path):

    processing_path = 'processing' 
    processed_path = 'processed'

    process_single_image(image_path, processing_path)

    if(errorHandler == 0):

        processing_image_path = os.path.join(processing_path, os.path.basename(image_path))

        # 모델 로드
        model = load_model('RGB.h5') #모델 로드
        model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

        # 이미지 로드 및 전처리
        img = image.load_img(processing_image_path, target_size=(224, 224))  # 이미지 로드 및 크기 조정
        img_array = image.img_to_array(img)  # 이미지를 배열로 변환
        img_array = np.expand_dims(img_array, axis=0)  # 이미지 배열에 배치 차원 추가
        norImg = img_array / 255.0  # 정규화

        # 모델 예측
        predictions = model.predict(norImg, verbose=0)
        predicted_class_index = np.argmax(predictions[0])

        # 클래스 레이블 정의
        class_labels = [
            'Bright_Spring', 
            'Bright_Winter', 
            'Dark_Autumn', 
            'Dark_Winter', 
            'Light_Spring', 
            'Light_Summer', 
            'Soft_Autumn', 
            'Soft_Summer', 
            'True_Autumn', 
            'True_Spring', 
            'True_Summer', 
            'True_Winter'
        ]

        # 예측 결과 출력
        predicted_class_label = class_labels[predicted_class_index]
        print(predicted_class_label)
        shutil.move(processing_image_path, os.path.join(processed_path, os.path.basename(image_path)))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python server_detectFace_resizer.py <image_path>")
        sys.exit(1)

    image_path = sys.argv[1]
    main(image_path)
