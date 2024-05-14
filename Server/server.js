const express = require('express');
const multer = require('multer');
const bodyParser = require('body-parser');
const path = require('path');
const fs = require('fs');
const { spawn } = require('child_process');

const app = express();
const port = 3000;

// bodyParser 설정
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

// 파일 저장을 위한 multer 설정
const storage = multer.diskStorage({
    destination: function(req, file, cb) {
        cb(null, 'uploads/'); // 업로드된 파일을 저장할 디렉토리
    },
    filename: function(req, file, cb) {
        // 파일명 설정
        cb(null, file.fieldname + '-' + Date.now() + path.extname(file.originalname));
    }
});

// multer 인스턴스 생성
const upload = multer({ storage: storage });

// Python 스크립트 실행 함수
function runPythonScript(imagePath) {
  return new Promise((resolve, reject) => {
    console.log(`Python 스크립트 실행: ${imagePath}`);
    const process = spawn('python', ['server_detectFace_resizer.py', imagePath]);

    let scriptOutput = "";
    process.stdout.on('data', (data) => {
      console.log(`stdout: ${data}`);
      scriptOutput += data.toString();
    });

    process.stderr.on('data', (data) => {
      console.error(`stderr: ${data.toString()}`);
    });

    process.on('close', (code) => {
      console.log(`Python 스크립트 종료 코드: ${code}`);
      if (code === 0) {
        resolve(scriptOutput);
      } else {
        reject("Python script ended with error");
      }
    });
  });
}

// 업로드 API 라우트
app.post('/upload', upload.single('image'), async (req, res) => {
    const textData = req.body.text; // 클라이언트로부터 받은 텍스트 데이터
    const fileData = req.file; // 업로드된 파일 데이터

    if (!fileData) {
      return res.status(400).send({
        message: '파일이 업로드되지 않았습니다.',
      });
    }

    try {
      // Python 스크립트 실행 및 결과 받기
      const pythonResult = await runPythonScript(fileData.path);

      // Python 스크립트의 결과와 초기 텍스트 데이터를 클라이언트에 응답으로 보냄
      res.send({
        message: '파일 업로드 및 처리 성공!',
        textData: textData, // 처음 받은 텍스트 데이터
        pythonResult: pythonResult.trim(), // Python 스크립트의 출력 결과
      });
    } catch (error) {
      console.error(error);
      res.status(500).send({
        message: '서버에서 파일을 처리하는 중 오류가 발생했습니다.',
      });
    }
});

app.get('/', (req, res) => {
  res.send('Server running...'); // 사용자에게 "안녕" 메시지 보내기
});

app.listen(port, () => {
    console.log(`서버가 http://localhost:${port} 에서 실행 중입니다.`);
});





