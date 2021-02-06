package br.com.jobsfotogrid.instagram.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import br.com.jobsfotogrid.instagram.R;
import br.com.jobsfotogrid.instagram.helper.ConfiguracaoFirebase;
import br.com.jobsfotogrid.instagram.helper.UsuarioFirebase;
import br.com.jobsfotogrid.instagram.model.Usuario;

public class CadastroActivity extends AppCompatActivity {

    private EditText campoNome, campoEmail, campoSenha;
    private Button botaoCadastrar;
    private ProgressBar progressBarCadastro;
    private Usuario usuario;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_cadastro);
        inicializarComponentes();

        //Cadastrar usuario
        progressBarCadastro.setVisibility(View.GONE);
        botaoCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String textoNome  = campoNome.getText().toString();
                String textoEmail = campoEmail.getText().toString();
                String textoSenha = campoSenha.getText().toString();
                    if(!textoNome.isEmpty()){
                        if(!textoEmail.isEmpty()){
                            if(!textoSenha.isEmpty()){

                                usuario = new Usuario();
                                usuario.setNome(textoNome);
                                usuario.setEmail(textoEmail);
                                usuario.setSenha(textoSenha);
                                cadastrarUsuario(usuario);

                            } else {
                                Toast.makeText(CadastroActivity.this,
                                        "Preencha a senha!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(CadastroActivity.this,
                                    "Preencha o email!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CadastroActivity.this,
                                "Preencha o nome!",
                                Toast.LENGTH_SHORT).show();
                    }
            }
        });
    }

    public void cadastrarUsuario(final Usuario usuario){
        progressBarCadastro.setVisibility(View.VISIBLE);
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(
                this,
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if( task.isSuccessful() ){

                            try {
                                progressBarCadastro.setVisibility(View.VISIBLE);

                                //Metódo responsável por salvar os dados dos usuários
                                String idUsuario = task.getResult().getUser().getUid();
                                usuario.setId(idUsuario);
                                usuario.salvar();

                                //Salvar dados do profile do firebase
                                UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());


                                Toast.makeText(CadastroActivity.this,
                                        "Cadastro com sucesso",
                                        Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                finish();
                            } catch (Exception e){
                                e.printStackTrace();
                            }

                        } else {
                            progressBarCadastro.setVisibility(View.GONE);
                            String erroExcecao = "";
                            try {
                                throw task.getException();
                            }catch (FirebaseAuthWeakPasswordException e){
                                erroExcecao = "Digite uma senha mais forte!";
                            }catch (FirebaseAuthInvalidCredentialsException e){
                                erroExcecao = "Por favor, digite um e-mail válido!";
                            }catch (FirebaseAuthUserCollisionException e){
                                erroExcecao = "Esta conta já foi cadastrada!";
                            }catch (Exception e){
                                erroExcecao = "Erro ao cadastrar usuário!" + e.getMessage();
                                e.printStackTrace();
                            }
                            Toast.makeText(CadastroActivity.this,
                                        erroExcecao,
                                    Toast.LENGTH_SHORT).show();
                        }
                    };
                }
        );
    }

    private void inicializarComponentes() {
        campoNome           = findViewById(R.id.editCadastroNome);
        campoEmail          = findViewById(R.id.editCadastroEmail);
        campoSenha          = findViewById(R.id.editCadastroSenha);
        botaoCadastrar      = findViewById(R.id.buttonCadastrar);
        progressBarCadastro = findViewById(R.id.progressCadastro);
    }
}