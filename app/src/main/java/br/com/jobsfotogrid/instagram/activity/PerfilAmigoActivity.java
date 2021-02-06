package br.com.jobsfotogrid.instagram.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.com.jobsfotogrid.instagram.R;
import br.com.jobsfotogrid.instagram.adapter.AdapterGrid;
import br.com.jobsfotogrid.instagram.helper.ConfiguracaoFirebase;
import br.com.jobsfotogrid.instagram.helper.UsuarioFirebase;
import br.com.jobsfotogrid.instagram.model.Postagem;
import br.com.jobsfotogrid.instagram.model.Usuario;
import de.hdodenhof.circleimageview.CircleImageView;

public class PerfilAmigoActivity extends AppCompatActivity {

    private Usuario usuarioSelecionado;
    private Usuario usuarioLogado;
    private Button editarPerfil;
    private CircleImageView imagePerfil;
    private DatabaseReference usuariosRef;
    private DatabaseReference usuarioAmigoRef;
    private DatabaseReference usuarioLogadoRef;
    private DatabaseReference seguidoresRef;
    private DatabaseReference firebaseRef;
    private DatabaseReference postagemUsuarioRef;
    private ValueEventListener valueEventListenerPerfilAmigo;
    private TextView textpublicacoes, textSeguidores, textSeguindo;
    private String idUsuarioLogado;
    private AdapterGrid adapterGrid;
    private GridView gridViewPerfil;
    private List<Postagem> postagens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_perfil_amigo);

        //Metódo responsável por inicializar os componentes
        inicializarComponentes();

        //Configurações responsáveis pela toolbar
        Toolbar toolbar = findViewById(R.id.toolbarPrincipal);
        toolbar.setTitle("Perfil");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_voltar);

        //Recuperar usuario selecionado
        Bundle bundle = getIntent().getExtras();
        if( bundle != null){
            usuarioSelecionado = (Usuario) bundle.getSerializable("usuarioSelecionado");

            //Configura referência de postagens do usuário
            postagemUsuarioRef = ConfiguracaoFirebase.getFirebase()
                    .child("postagens")
                    .child(usuarioSelecionado.getId());

            //Método responsável por exibir o nome do usuário na toolbar
            getSupportActionBar().setTitle(usuarioSelecionado.getNome());

            //Metódo responsável por recuperar a foto do usuário
            String caminhoFoto = usuarioSelecionado.getCaminhoFoto();
            if(caminhoFoto != null){
                Uri url = Uri.parse(caminhoFoto);
                Glide.with(PerfilAmigoActivity.this)
                .load(url)
                .into(imagePerfil);
            }
        }

        inicializarImageLoder();
        carregarFotosPostagem();

        //Abrir a foto clicada
        gridViewPerfil.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Postagem postagem = postagens.get(position);
                Intent i = new Intent(getApplicationContext(), VisualizarPostagemActivity.class);
                i.putExtra("postagem", postagem);
                i.putExtra("usuario", usuarioSelecionado);
                startActivity(i);
            }
        });
    }

    public void inicializarImageLoder(){
        ImageLoaderConfiguration config = new ImageLoaderConfiguration
                .Builder(this)
                .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .diskCacheSize(50 * 1024 * 1024)
                .diskCacheFileCount(100)
                .diskCacheFileNameGenerator(new HashCodeFileNameGenerator())
                .build();
        ImageLoader.getInstance().init(config);
    }

    private void carregarFotosPostagem(){

        //Recupera as fotos postadas pelo usuário
        postagens = new ArrayList<>();
        postagemUsuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //Configurar o tamanho do grid
                int tamanhoGrid = getResources().getDisplayMetrics().widthPixels;
                int tamanhoImagem = tamanhoGrid / 3;
                gridViewPerfil.setColumnWidth( tamanhoImagem );

                List<String> urlFotos = new ArrayList<>();
                for( DataSnapshot ds : snapshot .getChildren() ){
                    Postagem postagem = ds.getValue(Postagem.class);
                    postagens.add( postagem );
                    urlFotos.add( postagem.getCaminhoFoto() );
                }

                //Configurar adapter
                adapterGrid = new AdapterGrid(getApplicationContext(), R.layout.grid_postagem, urlFotos);
                gridViewPerfil.setAdapter(adapterGrid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void recuperarDadosUsuarioLogado(){
        usuarioLogadoRef = usuariosRef.child(idUsuarioLogado);
        usuarioLogadoRef.addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //Verifica se o usuário já está seguindo
                    usuarioLogado = snapshot.getValue(Usuario.class);
                    /*Verifica se o usuário já está seguindo o
                    amigo selecionado
                     */
                    verificaSegueUsuarioAmigo();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            }
        );
    }

    private void verificaSegueUsuarioAmigo(){
        DatabaseReference seguidorRef = seguidoresRef
                .child( usuarioSelecionado.getId())
                .child( idUsuarioLogado );
        seguidorRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if( snapshot.exists()){
                            //O usuário já está sendo seguindo
                            habilitarBotaoSeguir(true);
                        } else {
                            //Ainda não está sendo seguindo
                            habilitarBotaoSeguir(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                }
        );
    }

    private void habilitarBotaoSeguir(boolean segueUsuario){
        if( segueUsuario ){
            editarPerfil.setText("Seguindo");
        } else {
            editarPerfil.setText("Seguir");
            //adiciona evento para seguir o usuário desejado
            editarPerfil.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Salvar seguidor
                    salvarSeguidor(usuarioLogado, usuarioSelecionado);
                }
            });
        }
    }

    private void salvarSeguidor(Usuario uLogado, Usuario uAmigo){

        /*
         * seguidores
         * id_usuario_selecionado (id amigo)
         *   id_usuario_logado (id usuario logado)
         *       dados logado
         * */
        HashMap<String, Object> dadosUsuarioLogado = new HashMap<>();
        dadosUsuarioLogado.put("nome", uLogado.getNome() );
        dadosUsuarioLogado.put("caminhoFoto", uLogado.getCaminhoFoto() );
        DatabaseReference seguidorRef = seguidoresRef
                .child( uAmigo.getId() )
                .child( uLogado.getId() );
        seguidorRef.setValue( dadosUsuarioLogado );

        //Alterar botao acao para seguindo
        editarPerfil.setText("Seguindo");
        editarPerfil.setOnClickListener(null);

        //Incrementar seguindo do usuário logado
        int seguindo = uLogado.getSeguindo() + 1;
        HashMap<String, Object> dadosSeguindo = new HashMap<>();
        dadosSeguindo.put("seguindo", seguindo );
        DatabaseReference usuarioSeguindo = usuariosRef
                .child( uLogado.getId() );
        usuarioSeguindo.updateChildren( dadosSeguindo );

        //Incrementar seguidores do amigo
        int seguidores = uAmigo.getSeguidores() + 1;
        HashMap<String, Object> dadosSeguidores = new HashMap<>();
        dadosSeguidores.put("seguidores", seguidores );
        DatabaseReference usuarioSeguidores = usuariosRef
                .child( uAmigo.getId() );
        usuarioSeguidores.updateChildren( dadosSeguidores );
    }


    @Override
    protected void onStart() {
        super.onStart();
        //Recuperar dados do amigo selecionado
        recuperarDadosPerfilAmigo();

        //Recuperar dados do usuário logado
        recuperarDadosUsuarioLogado();
    }

    @Override
    protected void onStop() {
        super.onStop();
        usuarioAmigoRef.removeEventListener(valueEventListenerPerfilAmigo);
    }

    //Metódo responsável por recuperar os dados do perfil amigo
    private void recuperarDadosPerfilAmigo(){
        usuarioAmigoRef = usuariosRef.child(usuarioSelecionado.getId());
        valueEventListenerPerfilAmigo = usuarioAmigoRef.addValueEventListener(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    String postagens  = String.valueOf(usuario.getPostagens());
                    String seguindo   = String.valueOf(usuario.getSeguindo());
                    String seguidores = String.valueOf(usuario.getSeguidores());

                    //Configurar valores recuperados
                    textpublicacoes.setText(postagens);
                    textSeguidores.setText( seguidores );
                    textSeguindo.setText( seguindo );
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            }
        );
    }

    //Configurações iniciais
    private void inicializarComponentes(){
        editarPerfil    = findViewById(R.id.buttonAcaoPerfil);
        editarPerfil.setText("Carregando");
        imagePerfil     = findViewById(R.id.imageEditarPerfil);
        firebaseRef     = ConfiguracaoFirebase.getFirebase();
        usuariosRef     = firebaseRef.child("usuarios");
        seguidoresRef   = firebaseRef.child("seguidores");
        textpublicacoes = findViewById(R.id.textPublicacoes);
        textSeguidores  = findViewById(R.id.textSeguidores);
        textSeguindo    = findViewById(R.id.textSeguindo);
        idUsuarioLogado = UsuarioFirebase.getIdentificadorUsuario();
        gridViewPerfil  = findViewById(R.id.gridViewPerfil);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}