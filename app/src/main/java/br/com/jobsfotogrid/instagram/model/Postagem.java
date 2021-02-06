package br.com.jobsfotogrid.instagram.model;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import br.com.jobsfotogrid.instagram.helper.ConfiguracaoFirebase;
import br.com.jobsfotogrid.instagram.helper.UsuarioFirebase;

public class Postagem implements Serializable {
    private String id;
    private String idUsuario;
    private String descricao;
    private String caminhoFoto;

    public Postagem() {
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebase();
        DatabaseReference postagemRef = firebaseRef.child("postagens");
        String idPostagem = postagemRef.push().getKey();
        setId(idPostagem);
    }

    public boolean salvar(DataSnapshot seguidoresSnapshop){

        Map objeto = new HashMap();
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebase();

        //Referência das postagem
        String combinacaoId = "/" + getIdUsuario() + "/" + getId();
        objeto.put("/postagens/" + combinacaoId, this);

        //Referência para o feed
        for ( DataSnapshot seguidores : seguidoresSnapshop.getChildren() ){

            String idSeguidor = seguidores.getKey();

            HashMap<String, Object> dadosSeguindor = new HashMap<>();
            dadosSeguindor.put("fotoPostagem", getCaminhoFoto());
            dadosSeguindor.put("descricao", getDescricao());
            dadosSeguindor.put("id", getId());
            dadosSeguindor.put("nomeUsuario", usuarioLogado.getNome());
            dadosSeguindor.put("fotoUsuario", usuarioLogado.getCaminhoFoto());

            String idAtualizacao =   "/" + idSeguidor + "/" + getId();
            objeto.put("/feed" + idAtualizacao, dadosSeguindor);
        }
        firebaseRef.updateChildren(objeto);
        return true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCaminhoFoto() {
        return caminhoFoto;
    }

    public void setCaminhoFoto(String caminhoFoto) {
        this.caminhoFoto = caminhoFoto;
    }
}
