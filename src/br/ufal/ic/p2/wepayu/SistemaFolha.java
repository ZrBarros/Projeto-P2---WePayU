package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;

import java.io.Serializable;
import java.util.LinkedHashMap;

/** Mantém os empregados cadastrados e o próximo identificador disponível. */
class SistemaFolha implements Serializable {
    private static final long serialVersionUID = 1L;

    final LinkedHashMap<String, Empregado> empregados = new LinkedHashMap<>();
    int proximoId = 1;

    String novoId() { return Integer.toString(proximoId++); }
}
