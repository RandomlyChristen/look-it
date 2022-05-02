# AR Notepad, Look-it

이 레포지토리는 "기계설비 기반 SW 융합인적자원 생태계 조성사업"의 지원을 받아 진행한 연구과제임을 알립니다.

<img src="https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=Android&logoColor=white"/> <img src="https://img.shields.io/badge/OpenCV-5C3EE8?style=flat-square&logo=OpenCV&logoColor=white"/> <img src="https://img.shields.io/badge/SQLite-003B57?style=flat-square&logo=SQLite&logoColor=white"/>

### 메모 부착과정

```mermaid
sequenceDiagram
    participant HomeActivity
    participant CaptureActivity
    participant JNI
    participant DBManager
    
    HomeActivity ->> CaptureActivity: ActivityResultLauncher.lucnch()
    
    loop camera
        CaptureActivity ->> JNI: drawAndGetInterest(inputFrame)
        JNI ->> CaptureActivity: native Mat
    end
    
    alt RESULT_OK
        CaptureActivity ->> HomeActivity: Mat
        HomeActivity ->> DBManager: newMemo(), updateMemo(), ...
        Note right of DBManager: on SimpleBackground
    else !RESULT_OK
        CaptureActivity ->> HomeActivity: failure
        
    end
```


### 메모 열람과정
```mermaid
sequenceDiagram
    participant LookActivity
    participant Matcher
    participant JNI
    
    loop camera (on ui thread)
        LookActivity ->> Matcher: setTrainGray(inputFrame)
        opt on matcher thread
            Matcher ->> JNI: matchQuery(trainGray)
            JNI ->> Matcher: getResult()
        end
        Matcher ->> LookActivity: getResult()
    end
```

<a href="https://drive.google.com/file/d/1ZgK2fZy1aORm6UpwSPUKcb1N2XV0sV1N/view" title="Link Title"><img src="./img/lookit.png" alt="데모영상" /></a>