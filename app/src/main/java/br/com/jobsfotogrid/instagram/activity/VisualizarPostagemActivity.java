package br.com.jobsfotogrid.instagram.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import br.com.jobsfotogrid.instagram.R;
import br.com.jobsfotogrid.instagram.model.Postagem;
import br.com.jobsfotogrid.instagram.model.Usuario;
import de.hdodenhof.circleimageview.CircleImageView;

public class VisualizarPostagemActivity extends AppCompatActivity {

    private TextView textperfilPostagem, textqtdCurtidasPostagem, textDescricaoPostagem;
    private ImageView imagePostagemSelecionada;
    private CircleImageView imagePerfilPostagem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_visualizar_postagem);
        Toolbar toolbar = findViewById(R.id.toolbarPrincipal);
        toolbar.setTitle("Visualizar Postagens");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_voltar);

        inicializarComponentes();

        //Recuperar dados do perfil amigo
        Bundle bundle = getIntent().getExtras();
        if( bundle != null ){
            Postagem postagem = (Postagem) bundle.getSerializable("postagem");
            Usuario usuario = (Usuario) bundle.getSerializable("usuario");

            //Exibi dados de usuário
            Uri uri = Uri.parse( usuario.getCaminhoFoto() );
            Glide.with(VisualizarPostagemActivity.this)
                    .load(uri)
                    .into(imagePerfilPostagem);
            textperfilPostagem.setText(usuario.getNome());

            //Exibi os dados da postagem
            Uri uriPostagem = Uri.parse(postagem.getCaminhoFoto());
            Glide.with(VisualizarPostagemActivity.this)
                    .load(uriPostagem)
                    .into(imagePostagemSelecionada);
            textDescricaoPostagem.setText(postagem.getDescricao());
        }
    }

    private void inicializarComponentes() {
        imagePostagemSelecionada = findViewById(R.id.imagePostagemSelecionada);
        imagePerfilPostagem      = findViewById(R.id.imagePerfilPostagem);
        textperfilPostagem       = findViewById(R.id.textPerfilPostagem);
        textqtdCurtidasPostagem  = findViewById(R.id.textQtdCurtidasPostagem);
        textDescricaoPostagem    = findViewById(R.id.textDescricaoPostagem);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}