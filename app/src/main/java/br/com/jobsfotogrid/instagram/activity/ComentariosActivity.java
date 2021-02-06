package br.com.jobsfotogrid.instagram.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import br.com.jobsfotogrid.instagram.R;
import br.com.jobsfotogrid.instagram.adapter.AdapterComentario;
import br.com.jobsfotogrid.instagram.helper.ConfiguracaoFirebase;
import br.com.jobsfotogrid.instagram.helper.UsuarioFirebase;
import br.com.jobsfotogrid.instagram.model.Comentario;
import br.com.jobsfotogrid.instagram.model.Usuario;

public class ComentariosActivity extends AppCompatActivity {

    private EditText editComentario;
    private RecyclerView recyclerComentarios;
    private String idPostagem;
    private Usuario usuario;
    private AdapterComentario adapterComentario;
    private List<Comentario> listaComentarios = new ArrayList<>();
    private DatabaseReference firebaserRef;
    private DatabaseReference comentariosRef;
    private ValueEventListener valueEventListenerComentarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_comentarios);
        Toolbar toolbar = findViewById(R.id.toolbarPrincipal);
        toolbar.setTitle("Comentários");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_voltar);

        inicializarComponentes();

        //Configura recyclerview
        adapterComentario = new AdapterComentario(listaComentarios, getApplicationContext() );
        recyclerComentarios.setHasFixedSize( true );
        recyclerComentarios.setLayoutManager(new LinearLayoutManager(this));
        recyclerComentarios.setAdapter( adapterComentario );

        //Recupera id da postagem
        Bundle bundle = getIntent().getExtras();
        if( bundle != null ){
            idPostagem = bundle.getString("idPostagem");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    recuperarComentarios();
    }

    private void recuperarComentarios(){

        comentariosRef = firebaserRef.child("comentarios")
                .child( idPostagem );
        valueEventListenerComentarios = comentariosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaComentarios.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    listaComentarios.add( ds.getValue(Comentario.class) );
                }
                adapterComentario.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    public  void salvarComentario(View view){

        String textoComentario = editComentario.getText().toString();
        if( textoComentario != null && !textoComentario.equals("") ){

            Comentario comentario = new Comentario();
            comentario.setIdPostagem( idPostagem );
            comentario.setIdUsuario( usuario.getId() );
            comentario.setNomeUsuario( usuario.getNome() );
            comentario.setCaminhoFoto( usuario.getCaminhoFoto() );
            comentario.setComentario( textoComentario );
            if(comentario.salvar()){
                Toast.makeText(this,
                        "Comentário salvo com sucesso!",
                Toast.LENGTH_SHORT).show();
            }

        }else {
        Toast.makeText(this,
                "Insira o comentário antes de salvar!",
            Toast.LENGTH_SHORT).show();
        }
        //Limpa comentário digitado
        editComentario.setText("");
    }


    private void inicializarComponentes() {
        editComentario = findViewById(R.id.editComentario);
        usuario        = UsuarioFirebase.getDadosUsuarioLogado();
        recyclerComentarios = findViewById(R.id.recyclerComentarios);
        firebaserRef = ConfiguracaoFirebase.getFirebase();
    }

    @Override
    protected void onStop() {
        super.onStop();
    comentariosRef.removeEventListener( valueEventListenerComentarios );
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}