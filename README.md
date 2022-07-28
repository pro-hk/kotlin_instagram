# 코틀린 - 인스타그램 클론 코딩
### 개인 토이 프로젝트로 코틀린과 Firebase로 동작
### 하울스타그램 유튜브 참조
## 개발 환경
Windows<br>
Android Studio Chipmunk | 2021.2.1 Patch 1<br>
Java 1.8.0_321
## 개발 base
Android 11<br>
Kotlin<br>
Firebase
<br><br>
## 문제 & 해결
### 1. 이메일 로그인 진행 시, 'The email address is already in use by another account.' 오류
#### [해결] 로그인 후 trim으로 
### 2. 구글 로그인 진행 시, Auth.GoogleSignInApi.getSignInResultFromIntent(data).isSuccess = false 오류
#### firebase에 SHA 미인증으로 문제 발생
#### -> Gradle(우측)/Tasks/android/signingReport 을 통해 SHA1 값 확인 
#### -> firebase 일반 설정에 등록
