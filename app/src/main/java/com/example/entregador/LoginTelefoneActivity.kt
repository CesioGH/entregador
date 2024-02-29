package com.example.entregador

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.entregador.databinding.ActivityLoginTelefoneBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginTelefoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginTelefoneBinding
    private lateinit var verificacaoId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginTelefoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.receberSmsButton.setOnClickListener {
            val telefone = binding.telefoneEditText.text.toString().trim()
            if (telefone.isNotEmpty()) {
                enviarCodigo(telefone)
            } else {
                Toast.makeText(this, "Insira um número de telefone.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.verificarCodigoButton.setOnClickListener {
            val codigo = binding.codigoSmsEditText.text.toString().trim()
            if (codigo.isNotEmpty()) {
                verificarCodigoComFirebase(codigo)
            } else {
                Toast.makeText(this, "Insira o código do SMS.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarCodigo(telefone: String) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(telefone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(applicationContext, "Falha na verificação: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(verificationId, token)
            verificacaoId = verificationId
            binding.telefoneEditText.isEnabled = false
            binding.codigoSmsEditText.isEnabled = true
            binding.verificarCodigoButton.isEnabled = true
        }
    }

    private fun verificarCodigoComFirebase(codigo: String) {
        val credential = PhoneAuthProvider.getCredential(verificacaoId, codigo)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(applicationContext, "Login bem-sucedido.", Toast.LENGTH_LONG).show()
                    // Redirecionar para MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Encerra esta atividade para evitar retornar a ela ao pressionar voltar
                } else {
                    Toast.makeText(applicationContext, "Falha no login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

}
