package com.prohk.kotlin_instagram

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import com.prohk.kotlin_instagram.databinding.ActivityLoginBinding
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {

    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    // Authentication 라이브러리 생성
    var auth: FirebaseAuth? = null

    // google - SHA 인증서 해야됨
    var googleSignInClient: GoogleSignInClient? = null
    val GOOGLE_LOGIN_CODE = 9001

    // facebook - callbackmanager
    var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // singup or singin by email 버튼 클릭 이벤트
        binding.emailLoginButton.setOnClickListener {
            signinAndSignup()
        }

        // google 옵션
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googleSignInButton.setOnClickListener {
            // First step
            googleLogin()
        }

        // 페이스북 해시태그
        //printHashKey()

        // 페이스북
        callbackManager = CallbackManager.Factory.create()

        binding.facebookLoginButton.setOnClickListener {
            // First step
            facebookLogin()
        }
    }

    // 자동 로그인 기능
    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }

    // 페이스북 해시키
    fun printHashKey() {
        try {
            val info: PackageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey: String = String(Base64.encode(md.digest(), 0))
                Log.i("TAG", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("TAG", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("TAG", "printHashKey()", e)
        }
    }

    // 페이스북 로그인
    fun facebookLogin() {
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onCancel() {

                }

                override fun onError(error: FacebookException) {

                }

                override fun onSuccess(result: LoginResult) {
                    // Second step
                    handleFacebookAccessToken(result?.accessToken)
                }
            })
    }

    fun handleFacebookAccessToken(token: AccessToken?) {
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    // Third step
                    // 아이디와 패스워드가 맞았을 때
                    moveMainPage(task.result.user) // 메인페이지 이동
                } else {
                    // 아이디와 패스워드가 틀렸을 때
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show() // 에러메시지 호출
                }
            }
    }

    // 구글 로그인
    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    // 액티비티 결과 값 수신
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 페이스북 콜백
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        // 구글 로그인 결과
        if(requestCode == GOOGLE_LOGIN_CODE) {
            var result = data?.let { Auth.GoogleSignInApi.getSignInResultFromIntent(it) }
            if(result!!.isSuccess) {
                var account = result.signInAccount
                // Second step
                firebaseAuthWithGoogle(account)
            }
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        var credentail = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credentail)
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    // 아이디와 패스워드가 맞았을 때
                    moveMainPage(task.result.user) // 메인페이지 이동
                } else {
                    // 아이디와 패스워드가 틀렸을 때
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show() // 에러메시지 호출
                }
            }
    }

    // 아이디 생성
    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(
            binding.emailEdittext.text.toString(),
            binding.passwordEdittext.text.toString()
        )?.addOnCompleteListener { task ->
            if(task.isSuccessful) {
                // 아이디가 생성되었을 때 필요한 코드를 입력
                moveMainPage(task.result.user) // 메인페이지 이동
            } else if(!task.exception?.message.isNullOrEmpty()) {
                // 로그인 에러가 나타났을 때 에러 메시지 출력
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show() // 에러메시지 호출
            } else {
                // 회원가입, 에러메시지 아닌 경우 로그인하는 부분으로 빠짐
                singinEmail() // 로그인
            }
        }
    }

    // 로그인
    fun singinEmail() {
        auth?.signInWithEmailAndPassword(
            binding.emailEdittext.text.toString(),
            binding.passwordEdittext.text.toString()
        )?.addOnCompleteListener { task ->
            if(task.isSuccessful) {
                // 아이디와 패스워드가 맞았을 때
                moveMainPage(task.result.user) // 메인페이지 이동
            } else {
                Toast.makeText(this,"로그인",Toast.LENGTH_LONG).show()
                // 아이디와 패스워드가 틀렸을 때
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show() // 에러메시지 호출
            }
        }
    }
    
    // 로그인 성공 후 페이지 이동
    fun moveMainPage(user:FirebaseUser?) {
        if(user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}