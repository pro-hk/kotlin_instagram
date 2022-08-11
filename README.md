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
#### 미해결
### 2. 구글 로그인 진행 시, Auth.GoogleSignInApi.getSignInResultFromIntent(data).isSuccess = false 오류
#### firebase에 SHA 미인증으로 문제 발생
#### -> Gradle(우측)/Tasks/android/signingReport 을 통해 SHA1 값 확인 
#### -> firebase 일반 설정에 등록
### 3. DetailViewFragment & RecyclerView - binding  문제
#### onCreateView class에 binding = FragmentBinding.inflate(inflate, container, false)
#### DetailViewRecyclerAdapter(): RecyclerView.Adapter<CustomViewHolder>()
#### onCreateViewHolder class에 view = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
#### onBindViewHolder class에 viewHolder = holder.binding
### 4. 프로필 사진 클릭 -> user page 전환 시 mainActivity 변경 안됨
#### mainBinding = ActivityMainBinding.inflate(inflate, container, false) -> mainBinding = (activity as MainActivity)
#### mainBinding.binding.위젯~~ 가능
### 5. firebase 내림차순 변경
#### orderBy("X", Query.Direction.DESCENDING) -- X : 정렬 기준
### 6. 댓글창 진입 및 댓글 등록시 refresh 안됨
#### RecyclerViewAdapter - init 의 addSnapshotListener 에 notifyDataSetchanged() 적용
### 7. FirebaseInstanceID 가 Deprecated되어 사용 불가
#### FirebaseInstanceId.getInstance().instanceId -> FirebaseMessaging.getInstance().token
