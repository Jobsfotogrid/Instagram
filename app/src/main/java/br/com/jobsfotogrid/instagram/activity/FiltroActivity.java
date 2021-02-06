package br.com.jobsfotogrid.instagram.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.zomato.photofilters.FilterPack;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.utils.ThumbnailItem;
import com.zomato.photofilters.utils.ThumbnailsManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import br.com.jobsfotogrid.instagram.R;
import br.com.jobsfotogrid.instagram.adapter.AdapterMiniaturas;
import br.com.jobsfotogrid.instagram.helper.ConfiguracaoFirebase;
import br.com.jobsfotogrid.instagram.helper.RecyclerItemClickListener;
import br.com.jobsfotogrid.instagram.helper.UsuarioFirebase;
import br.com.jobsfotogrid.instagram.model.Postagem;
import br.com.jobsfotogrid.instagram.model.Usuario;

public class FiltroActivity extends AppCompatActivity {

    static {
        System.loadLibrary("NativeImageProcessor");
    }

    private ImageView imageFotoEscolhida;
    private Bitmap imagem;
    private Bitmap imageFiltro;
    private List<ThumbnailItem> listaFiltros;
    private RecyclerView recyclerFiltros;
    private AdapterMiniaturas adapterMiniaturas;
    private String idUsuarioLogado;
    private TextInputEditText descricaoFiltro;
    private DatabaseReference usuarioLogadoRef;
    private DatabaseReference usuariosRef;
    private DatabaseReference firebaseRef;
    private DataSnapshot seguidoresSnapshot;
    private Usuario usuarioLogado;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_filtro);
        Toolbar toolbar = findViewById(R.id.toolbarPrincipal);
        toolbar.setTitle("Filtros");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_voltar);

        //Configurações iniciais
        listaFiltros = new ArrayList<>();
        usuariosRef = ConfiguracaoFirebase.getFirebase().child("usuarios");
        firebaseRef = ConfiguracaoFirebase.getFirebase();


        inicializarComponentes();

        //Recuperar dados do usuário logado
        recuperarDadosPostagem();

        //Recupera a imagem escolhida pelo usuário
        Bundle bundle = getIntent().getExtras();
        if( bundle != null ){
            byte[] dadosImagem = bundle.getByteArray("fotoEscolhida");
            imagem = BitmapFactory.decodeByteArray(dadosImagem, 0, dadosImagem.length );
            imageFotoEscolhida.setImageBitmap( imagem );
            imageFiltro = imagem.copy(imagem.getConfig(), true );

            //Configura recyclerView de filtros
            adapterMiniaturas = new AdapterMiniaturas(listaFiltros, getApplicationContext());
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            recyclerFiltros.setLayoutManager(layoutManager);
            recyclerFiltros.setAdapter(adapterMiniaturas);

            //Adiciona evento de clique no recyclerview
            recyclerFiltros.addOnItemTouchListener(new RecyclerItemClickListener(
                getApplicationContext(),
                recyclerFiltros,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        ThumbnailItem item = listaFiltros.get(position);
                        imageFiltro = imagem.copy(imagem.getConfig(), true );
                        Filter filtro = item.filter;
                        imageFotoEscolhida.setImageBitmap( filtro.processFilter(imageFiltro) );
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    }
                }
            ));

            recuperarFiltros();

        }
    }

    private void abrirDialogCarregando(String titulo){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(titulo);
        alert.setCancelable(false);
        alert.setView(R.layout.carregamento);

        dialog = alert.create();
        dialog.show();
    }

    private void recuperarDadosPostagem(){
        abrirDialogCarregando("Carregando suas informações!");
        usuarioLogadoRef = usuariosRef.child(idUsuarioLogado);
        usuarioLogadoRef.addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //Recupera dados do usuário logado
                    usuarioLogado = snapshot.getValue(Usuario.class);

                    //Recuperar seguidores
                    DatabaseReference seguidoresRef = firebaseRef
                            .child("seguidores")
                            .child(idUsuarioLogado);
                    seguidoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            seguidoresSnapshot = snapshot;
                            dialog.cancel();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            }
        );
    }

    private void recuperarFiltros() {
        //Limpar itens
        ThumbnailsManager.clearThumbs();
        listaFiltros.clear();
        //Configurar filtro normal
        ThumbnailItem item = new ThumbnailItem();
        item.image = imagem;
        item.filterName = "Normal";
        ThumbnailsManager.addThumb(item);

        //Lista de todos os filtros
        List< Filter > filtros = FilterPack.getFilterPack( getApplicationContext() );
        for ( Filter filtro: filtros ){
            ThumbnailItem itemFiltro = new ThumbnailItem();
            itemFiltro.image = imagem;
            itemFiltro.filter = filtro;
            itemFiltro.filterName = filtro.getName();
            ThumbnailsManager.addThumb(itemFiltro);
        }
        listaFiltros.addAll(ThumbnailsManager.processThumbs(getApplicationContext()));
        adapterMiniaturas.notifyDataSetChanged();
    }

    private void publicarPostagem() {
        abrirDialogCarregando("Criando sua postagem!");
        final Postagem postagem = new Postagem();
        postagem.setIdUsuario(idUsuarioLogado);
        postagem.setDescricao(descricaoFiltro.getText().toString());

        //Recuperar dados da imagem para o firabase
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageFiltro.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] dadosImagem = baos.toByteArray();

        //Salvar imagem no Firebase Storege
        StorageReference storageRef = ConfiguracaoFirebase.getFirebaseStorage();
        final StorageReference imagemRef = storageRef
            .child("imagens")
            .child("postagens")
            .child(postagem.getId() + "jpeg");

        UploadTask uploadTask = imagemRef.putBytes( dadosImagem );
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            Toast.makeText(FiltroActivity.this,
                    "Erro ao fazer upload da imagem",
            Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
            imagemRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                Uri url = task.getResult();
                postagem.setCaminhoFoto(url.toString());

                    //Atualizar qtd de postagens
                    int qtdPostagem = usuarioLogado.getPostagens() + 1;
                    usuarioLogado.setPostagens(qtdPostagem);
                    usuarioLogado.atualizarQtdPostagem();

                //Salvar postagem
                if(postagem.salvar(seguidoresSnapshot)){
                    Toast.makeText(FiltroActivity.this,
                            "Sucesso ao criar postagem!",
                            Toast.LENGTH_SHORT).show();
                    dialog.cancel();
                    finish();
                }
                }
            });
            }
        });
    }

    private void inicializarComponentes(){
        imageFotoEscolhida = findViewById(R.id.imageFotoEscolhida);
        recyclerFiltros    = findViewById(R.id.recyclerFiltros);
        descricaoFiltro    = findViewById(R.id.textDescricaoFiltro);
        idUsuarioLogado    = UsuarioFirebase.getIdentificadorUsuario();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_filtro, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch ( item.getItemId() ){
            case R.id.ic_salvar_postagem :
                publicarPostagem();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}