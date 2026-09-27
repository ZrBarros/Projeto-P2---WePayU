package br.ufal.ic.p2.wepayu;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formata números usando vírgula, como esperado nos testes. */
final class Formatador {
    private Formatador() { }

    static String dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.DOWN).toPlainString().replace('.', ',');
    }

    static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
