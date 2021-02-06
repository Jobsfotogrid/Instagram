package br.com.jobsfotogrid.instagram.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
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
import java.util.List;

import br.com.jobsfotogrid.instagram.R;
import br.com.jobsfotogrid.instagram.activity.EditarPerfilActivity;
import br.com.jobsfotogrid.instagram.activity.PerfilAmigoActivity;
import br.com.jobsfotogrid.instagram.adapter.AdapterGrid;
import br.com.jobsfotogrid.instagram.helper.ConfiguracaoFirebase;
import br.com.jobsfotogrid.instagram.helper.UsuarioFirebase;
import br.com.jobsfotogrid.instagram.model.Postagem;
import br.com.jobsfotogrid.instagram.model.Usuario;
import de.hdodenhof.circleimageview.CircleImageView;

public class PerfilFragment extends Fragment {

    private ProgressBar progressBar;
    private CircleImageView imagePerfil;
    private GridView gridViewPerfil;
    private Button editarPerfil;
    private Usuario usuarioLogado;
    private ValueEventListener valueEventListenerPerfil;
    private DatabaseReference firebaseRef;
    private DatabaseReference usuariosRef;
    private DatabaseReference usuarioLogadoRef;
    private DatabaseReference postagemUsuarioRef;
    private TextView textpublicacoes, textSeguidores, textSeguindo;
    private AdapterGrid adapterGrid;

    public PerfilFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        //Configurações iniciais
        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        firebaseRef     = ConfiguracaoFirebase.getFirebase();
        usuariosRef     = firebaseRef.child("usuarios");

        //Configura referência de postagens do usuário
        postagemUsuarioRef = ConfiguracaoFirebase.getFirebase()
                .child("postagens")
        .child(usuarioLogado.getId());


        inicializarComponentes(view);

        editarPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), EditarPerfilActivity.class);
                startActivity(i);
            }
        });

        inicializarImageLoder();

        carregarFotosPostagem();

        return  view;
    }

    public void inicializarImageLoder(){
        ImageLoaderConfiguration config = new ImageLoaderConfiguration
                .Builder(getActivity())
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
                    urlFotos.add( postagem.getCaminhoFoto() );
                }
                //Configurar adapter
                adapterGrid = new AdapterGrid(getActivity(), R.layout.grid_postagem, urlFotos);
                gridViewPerfil.setAdapter(adapterGrid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void inicializarComponentes(View view){
        //Configurações dos componentes
        progressBar     = view.findViewById(R.id.progressBarPerfil);
        imagePerfil     = view.findViewById(R.id.imageEditarPerfil);
        gridViewPerfil  = view.findViewById(R.id.gridViewPerfil);
        textpublicacoes = view.findViewById(R.id.textPublicacoes);
        textSeguidores  = view.findViewById(R.id.textSeguidores);
        textSeguindo    = view.findViewById(R.id.textSeguindo);
        editarPerfil    = view.findViewById(R.id.buttonAcaoPerfil);
    }

    private void recuperarFotoUsuario(){

        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        //Recuperar foto do usuário
        String caminhoFoto = usuarioLogado.getCaminhoFoto();
        if( caminhoFoto != null ){
            Uri url = Uri.parse( caminhoFoto );
            Glide.with(getActivity())
                    .load( url )
            .into( imagePerfil );
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        recuperarDadosUsuarioLogado();
        recuperarFotoUsuario();
    }

    @Override
    public void onStop() {
        super.onStop();
        usuariosRef.removeEventListener(valueEventListenerPerfil);
    }

    private void recuperarDadosUsuarioLogado() {
        usuarioLogadoRef = usuariosRef.child(usuarioLogado.getId());
        valueEventListenerPerfil = usuarioLogadoRef.addValueEventListener(
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
}