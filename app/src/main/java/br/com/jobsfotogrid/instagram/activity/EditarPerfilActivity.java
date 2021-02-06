package br.com.jobsfotogrid.instagram.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import br.com.jobsfotogrid.instagram.R;
import br.com.jobsfotogrid.instagram.helper.ConfiguracaoFirebase;
import br.com.jobsfotogrid.instagram.helper.Permissao;
import br.com.jobsfotogrid.instagram.helper.UsuarioFirebase;
import br.com.jobsfotogrid.instagram.model.Usuario;
import de.hdodenhof.circleimageview.CircleImageView;

public class EditarPerfilActivity extends AppCompatActivity {

    private CircleImageView imageEditarPerfil;
    private TextView alterarFotoPerfil;
    private TextInputEditText nomePerfil, emailPerfil;
    private Button salvarAlteracoes;
    private Usuario usuarioLogado;
    private static final int SELECAO_GALERIA = 200;
    private StorageReference reference;
    private String identificadorUsuario;

    private String[] permissoesNecessarias = new String[]{
        Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_editar_perfil);
        Toolbar toolbar = findViewById(R.id.toolbarPrincipal);
        toolbar.setTitle("Editar Perfil");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_voltar);

        //Validar permissões
        Permissao.validarPermissoes(permissoesNecessarias, this, 1);

        inicializarComponentes();

        //Metódo responsável por recuperar os dados do usuário
        FirebaseUser usuarioPerfil = UsuarioFirebase.getUsuarioAtual();
        nomePerfil.setText(usuarioPerfil.getDisplayName().toUpperCase());
        emailPerfil.setText(usuarioPerfil.getEmail());
        Uri url = usuarioPerfil.getPhotoUrl();
        if(url != null){
            Glide.with(EditarPerfilActivity.this)
                .load(url)
            .into(imageEditarPerfil);
        } else {
            imageEditarPerfil.setImageResource(R.drawable.avatar);
        }

        salvarAlteracoes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nomeAtualizado = nomePerfil.getText().toString();
                //Atualizar nome do perfil
                UsuarioFirebase.atualizarNomeUsuario(nomeAtualizado);
                //Atualizar o nome no banco de dados
                usuarioLogado.setNome(nomeAtualizado);
                usuarioLogado.atualizar();

                Toast.makeText(EditarPerfilActivity.this,
                        "Dados alterados com sucesso!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        //Método responsável por alterar a foto do usuário
        alterarFotoPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if(i.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(i, SELECAO_GALERIA);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( resultCode == RESULT_OK ){
            Bitmap imagem = null;

            try {

                //Selecao apenas da galeria
                switch ( requestCode ){
                    case SELECAO_GALERIA:
                        Uri localImagemSelecionada = data.getData();
                        imagem = MediaStore.Images.Media.getBitmap(getContentResolver(), localImagemSelecionada );
                        break;
                }

                //Caso tenha sido escolhido uma imagem
                if ( imagem != null ){

                    //Configura imagem na tela
                    imageEditarPerfil.setImageBitmap( imagem );

                    //Recuperar dados da imagem para o firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    //Salvar imagem no firebase
                    final StorageReference imagemRef = reference
                            .child("imagens")
                            .child("perfil")
                            .child(identificadorUsuario + ".jpeg");
                    UploadTask uploadTask = imagemRef.putBytes( dadosImagem );
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(EditarPerfilActivity.this,
                                    "Erro ao fazer upload da imagem",
                            Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            imagemRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    Uri uri = task.getResult();
                                    atualizarFotoUsuario(uri);

                                    Toast.makeText(EditarPerfilActivity.this,
                                            "Sucesso ao fazer upload da imagem",
                                    Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void atualizarFotoUsuario(Uri url) {
        //Atualizar foto do perfil
        UsuarioFirebase.atualizarFotoUsuario(url);


        //Atualizar foto do firebase
        usuarioLogado.setCaminhoFoto(url.toString());
        usuarioLogado.atualizar();

        Toast.makeText(EditarPerfilActivity.this,
                "Sua foto foi alterada com sucesso!",
                Toast.LENGTH_SHORT).show();
    }

    public void inicializarComponentes(){
        alterarFotoPerfil    = findViewById(R.id.textAlterarFoto);
        emailPerfil          = findViewById(R.id.editEmailPerfil);
        imageEditarPerfil    = findViewById(R.id.circleImageViewEditarPerfil);
        nomePerfil           = findViewById(R.id.editNomePerfil);
        salvarAlteracoes     = findViewById(R.id.buttonSalvarAlteracoes);
        reference            = ConfiguracaoFirebase.getFirebaseStorage();
        identificadorUsuario = UsuarioFirebase.getIdentificadorUsuario();
        emailPerfil.setFocusable(false);
        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}