package com.example.entregador

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.entregador.databinding.ActivityCadastroEntregadorBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class CadastroEntregadorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroEntregadorBinding
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var uriCarteiraHabilitacao: Uri? = null
    private var uriFotoPerfil: Uri? = null
    private lateinit var tipoImagem: TipoImagem

    enum class TipoImagem {
        CARTEIRA_HABILITACAO, FOTO_PERFIL
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            when (tipoImagem) {
                TipoImagem.CARTEIRA_HABILITACAO -> {
                    uriCarteiraHabilitacao = it
                    binding.fotoCNHImageView.setImageURI(it)
                }
                TipoImagem.FOTO_PERFIL -> {
                    uriFotoPerfil = it
                    binding.fotoPerfilImageView.setImageURI(it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroEntregadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dataNascimentoEditText.setOnClickListener {
            abrirCalendario()
        }

        binding.fotoCNHImageView.setOnClickListener {
            tipoImagem = TipoImagem.CARTEIRA_HABILITACAO
            verificarPermissaoESelecionarImagem()
        }

        binding.fotoPerfilImageView.setOnClickListener {
            tipoImagem = TipoImagem.FOTO_PERFIL
            verificarPermissaoESelecionarImagem()
        }

        binding.cadastrarButton.setOnClickListener {
            uploadImagem(TipoImagem.CARTEIRA_HABILITACAO) { urlCarteira ->
                uploadImagem(TipoImagem.FOTO_PERFIL) { urlPerfil ->
                    salvarNoFirestore(urlCarteira, urlPerfil)
                }
            }
        }
    }

    private fun abrirCalendario() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(this, { _, ano, mes, dia ->
            val dataSelecionada = Calendar.getInstance()
            dataSelecionada.set(ano, mes, dia)
            val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.dataNascimentoEditText.setText(formatoData.format(dataSelecionada.time))
        }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun verificarPermissaoESelecionarImagem() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        } else {
            selecionarImagem()
        }
    }

    private fun selecionarImagem() {
        getContent.launch("image/*")
    }

    private fun uploadImagem(tipo: TipoImagem, callback: (String) -> Unit) {
        val uriImagem = when (tipo) {
            TipoImagem.CARTEIRA_HABILITACAO -> uriCarteiraHabilitacao
            TipoImagem.FOTO_PERFIL -> uriFotoPerfil
        }
        uriImagem?.let {
            val ref = storage.reference.child("images/${UUID.randomUUID()}")
            ref.putFile(it).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                ref.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    callback(downloadUri.toString())
                } else {
                    // Falha no upload
                }
            }
        }
    }

    private fun salvarNoFirestore(urlCarteira: String, urlPerfil: String) {
        val nome = binding.nomeEditText.text.toString()
        val dataNascimento = binding.dataNascimentoEditText.text.toString()
        val cpf = binding.cpfEditText.text.toString()
        val telefone = binding.telefoneEditText.text.toString()
        val endereco = binding.enderecoEditText.text.toString()

        val entregador = hashMapOf(
            "nome" to nome,
            "dataNascimento" to dataNascimento,
            "cpf" to cpf,
            "telefone" to telefone,
            "endereco" to endereco,
            "urlCarteiraHabilitacao" to urlCarteira,
            "urlFotoPerfil" to urlPerfil
        )

        db.collection("cadastroEntregador")
            .add(entregador)
            .addOnSuccessListener {
                Toast.makeText(this, "Entregador cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao cadastrar entregador.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selecionarImagem()
        }
    }
}
