package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;

import java.io.*;
import java.util.LinkedHashMap;

/** Mantém os empregados cadastrados e o próximo identificador disponível. */
class SistemaFolha implements Serializable {
    private static final long serialVersionUID = 1L;

    final LinkedHashMap<String, Empregado> empregados = new LinkedHashMap<>();
    int proximoId = 1;

    String novoId() { return Integer.toString(proximoId++); }

    SistemaFolha copia() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(this); }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (SistemaFolha) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Falha ao copiar o estado do sistema", e);
        }
    }
}
