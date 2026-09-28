package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.Empregado.CartaoPonto;
import br.ufal.ic.p2.wepayu.models.Empregado.TaxaServico;
import br.ufal.ic.p2.wepayu.models.Empregado.Venda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Faz os cálculos da folha e monta o arquivo de saída. */
class FolhaPagamento {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String NL = "\r\n";
    private static final String SEPARADOR = "=".repeat(127);
    private static final LocalDate PRIMEIRO_PAGAMENTO_COMISSIONADO = LocalDate.of(2005, 1, 14);

    static String total(SistemaFolha sistema, LocalDate data) {
        BigDecimal total = ZERO;
        for (Empregado empregado : sistema.empregados.values()) {
            if (deveReceber(empregado, data)) total = total.add(calcular(empregado, data).bruto);
        }
        return dinheiro(total);
    }

    static String relatorio(SistemaFolha sistema, LocalDate data) {
        List<Pagamento> horistas = pagamentos(sistema, data, Empregado.Tipo.HORISTA);
        List<Pagamento> assalariados = pagamentos(sistema, data, Empregado.Tipo.ASSALARIADO);
        List<Pagamento> comissionados = pagamentos(sistema, data, Empregado.Tipo.COMISSIONADO);

        StringBuilder out = new StringBuilder();
        out.append("FOLHA DE PAGAMENTO DO DIA ").append(data).append(NL);
        out.append("====================================").append(NL).append(NL);
        secaoHoristas(out, horistas);
        secaoAssalariados(out, assalariados);
        secaoComissionados(out, comissionados);

        BigDecimal total = somarBruto(horistas).add(somarBruto(assalariados)).add(somarBruto(comissionados));
        out.append("TOTAL FOLHA: ").append(dinheiro(total)).append(NL);
        return out.toString();
    }

    private static List<Pagamento> pagamentos(SistemaFolha sistema, LocalDate data, Empregado.Tipo tipo) {
        List<Pagamento> resultado = new ArrayList<>();
        for (Empregado empregado : sistema.empregados.values()) {
            if (empregado.getTipo() == tipo && deveReceber(empregado, data)) {
                resultado.add(calcular(empregado, data));
            }
        }
        resultado.sort(Comparator.comparing(p -> p.empregado.getNome()));
        return resultado;
    }

    private static boolean deveReceber(Empregado empregado, LocalDate data) {
        return switch (empregado.getTipo()) {
            case HORISTA -> data.getDayOfWeek() == DayOfWeek.FRIDAY;
            case ASSALARIADO -> data.equals(ultimoDiaUtil(data));
            case COMISSIONADO -> data.getDayOfWeek() == DayOfWeek.FRIDAY
                    && !data.isBefore(PRIMEIRO_PAGAMENTO_COMISSIONADO)
                    && ChronoUnit.WEEKS.between(PRIMEIRO_PAGAMENTO_COMISSIONADO, data) % 2 == 0;
        };
    }

    private static LocalDate ultimoDiaUtil(LocalDate data) {
        LocalDate ultimo = data.withDayOfMonth(data.lengthOfMonth());
        while (ultimo.getDayOfWeek() == DayOfWeek.SATURDAY || ultimo.getDayOfWeek() == DayOfWeek.SUNDAY) {
            ultimo = ultimo.minusDays(1);
        }
        return ultimo;
    }

    private static Pagamento calcular(Empregado empregado, LocalDate pagamento) {
        LocalDate inicio;
        if (empregado.getTipo() == Empregado.Tipo.HORISTA) inicio = pagamento.minusDays(6);
        else if (empregado.getTipo() == Empregado.Tipo.COMISSIONADO) inicio = pagamento.minusDays(13);
        else inicio = pagamento.withDayOfMonth(1);
        LocalDate fimExclusivo = pagamento.plusDays(1);

        Pagamento p = new Pagamento(empregado);
        if (empregado.getTipo() == Empregado.Tipo.HORISTA) {
            for (CartaoPonto cartao : empregado.getCartoes()) {
                if (noPeriodo(cartao.data, inicio, fimExclusivo)) {
                    p.horasNormais = p.horasNormais.add(cartao.horas.min(new BigDecimal("8")));
                    p.horasExtras = p.horasExtras.add(cartao.horas.subtract(new BigDecimal("8")).max(BigDecimal.ZERO));
                }
            }
            BigDecimal normais = empregado.getSalario().multiply(p.horasNormais);
            BigDecimal extras = empregado.getSalario().multiply(new BigDecimal("1.5")).multiply(p.horasExtras);
            p.bruto = centavos(normais.add(extras));
        } else if (empregado.getTipo() == Empregado.Tipo.ASSALARIADO) {
            p.bruto = centavos(empregado.getSalario());
        } else {
            p.fixo = centavos(empregado.getSalario().multiply(new BigDecimal("12"))
                    .divide(new BigDecimal("26"), 10, RoundingMode.HALF_UP));
            for (Venda venda : empregado.getVendas()) {
                if (noPeriodo(venda.data, inicio, fimExclusivo)) p.vendas = p.vendas.add(venda.valor);
            }
            p.vendas = centavos(p.vendas);
            p.comissao = centavos(p.vendas.multiply(empregado.getComissao()));
            p.bruto = centavos(p.fixo.add(p.comissao));
        }

        if (empregado.isSindicalizado() && p.bruto.compareTo(BigDecimal.ZERO) > 0) {
            LocalDate inicioDescontos = empregado.getTipo() == Empregado.Tipo.HORISTA
                    ? inicioDescontosHorista(empregado, pagamento) : inicio;
            long dias = ChronoUnit.DAYS.between(inicioDescontos, fimExclusivo);
            p.descontos = empregado.getTaxaSindical().multiply(BigDecimal.valueOf(dias));
            for (TaxaServico taxa : empregado.getTaxasServico()) {
                if (noPeriodo(taxa.data, inicioDescontos, fimExclusivo)) p.descontos = p.descontos.add(taxa.valor);
            }
            p.descontos = centavos(p.descontos);
        }
        p.liquido = centavos(p.bruto.subtract(p.descontos).max(BigDecimal.ZERO));
        return p;
    }

    private static boolean noPeriodo(LocalDate data, LocalDate inicio, LocalDate fimExclusivo) {
        return !data.isBefore(inicio) && data.isBefore(fimExclusivo);
    }

    private static LocalDate inicioDescontosHorista(Empregado empregado, LocalDate pagamento) {
        LocalDate primeiroCartao = empregado.getCartoes().stream().map(c -> c.data).min(LocalDate::compareTo).orElse(pagamento);
        for (LocalDate anterior = pagamento.minusWeeks(1); !anterior.isBefore(primeiroCartao); anterior = anterior.minusWeeks(1)) {
            if (brutoHorista(empregado, anterior).compareTo(BigDecimal.ZERO) > 0) return anterior.plusDays(1);
        }
        return primeiroCartao;
    }

    private static BigDecimal brutoHorista(Empregado empregado, LocalDate pagamento) {
        LocalDate inicio = pagamento.minusDays(6);
        LocalDate fim = pagamento.plusDays(1);
        BigDecimal normais = BigDecimal.ZERO;
        BigDecimal extras = BigDecimal.ZERO;
        for (CartaoPonto cartao : empregado.getCartoes()) {
            if (noPeriodo(cartao.data, inicio, fim)) {
                normais = normais.add(cartao.horas.min(new BigDecimal("8")));
                extras = extras.add(cartao.horas.subtract(new BigDecimal("8")).max(BigDecimal.ZERO));
            }
        }
        return centavos(empregado.getSalario().multiply(normais)
                .add(empregado.getSalario().multiply(new BigDecimal("1.5")).multiply(extras)));
    }

    private static void secaoHoristas(StringBuilder out, List<Pagamento> pagamentos) {
        out.append(SEPARADOR).append(NL)
                .append("===================== HORISTAS ================================================================================================").append(NL)
                .append(SEPARADOR).append(NL)
                .append("Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo").append(NL)
                .append("==================================== ===== ===== ============= ========= =============== ======================================").append(NL);
        for (Pagamento p : pagamentos) {
            out.append(String.format(Locale.ROOT, "%-36s %5s %5s %13s %9s %15s %s",
                    p.empregado.getNome(), numero(p.horasNormais), numero(p.horasExtras), dinheiro(p.bruto),
                    dinheiro(p.descontos), dinheiro(p.liquido), metodo(p.empregado))).append(NL);
        }
        out.append(NL);
        BigDecimal horas = BigDecimal.ZERO, extras = BigDecimal.ZERO, bruto = ZERO, descontos = ZERO, liquido = ZERO;
        for (Pagamento p : pagamentos) {
            horas = horas.add(p.horasNormais); extras = extras.add(p.horasExtras); bruto = bruto.add(p.bruto);
            descontos = descontos.add(p.descontos); liquido = liquido.add(p.liquido);
        }
        out.append(String.format(Locale.ROOT, "%-36s %5s %5s %13s %9s %15s", "TOTAL HORISTAS",
                numero(horas), numero(extras), dinheiro(bruto), dinheiro(descontos), dinheiro(liquido))).append(NL).append(NL);
    }

    private static void secaoAssalariados(StringBuilder out, List<Pagamento> pagamentos) {
        out.append(SEPARADOR).append(NL)
                .append("===================== ASSALARIADOS ============================================================================================").append(NL)
                .append(SEPARADOR).append(NL)
                .append("Nome                                             Salario Bruto Descontos Salario Liquido Metodo").append(NL)
                .append("================================================ ============= ========= =============== ======================================").append(NL);
        for (Pagamento p : pagamentos) {
            out.append(String.format(Locale.ROOT, "%-48s %13s %9s %15s %s", p.empregado.getNome(), dinheiro(p.bruto),
                    dinheiro(p.descontos), dinheiro(p.liquido), metodo(p.empregado))).append(NL);
        }
        out.append(NL);
        out.append(String.format(Locale.ROOT, "%-48s %13s %9s %15s", "TOTAL ASSALARIADOS", dinheiro(somarBruto(pagamentos)),
                dinheiro(somarDescontos(pagamentos)), dinheiro(somarLiquido(pagamentos)))).append(NL).append(NL);
    }

    private static void secaoComissionados(StringBuilder out, List<Pagamento> pagamentos) {
        out.append(SEPARADOR).append(NL)
                .append("===================== COMISSIONADOS ===========================================================================================").append(NL)
                .append(SEPARADOR).append(NL)
                .append("Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo").append(NL)
                .append("===================== ======== ======== ======== ============= ========= =============== ======================================").append(NL);
        for (Pagamento p : pagamentos) {
            out.append(String.format(Locale.ROOT, "%-21s %8s %8s %8s %13s %9s %15s %s", p.empregado.getNome(), dinheiro(p.fixo),
                    dinheiro(p.vendas), dinheiro(p.comissao), dinheiro(p.bruto), dinheiro(p.descontos),
                    dinheiro(p.liquido), metodo(p.empregado))).append(NL);
        }
        out.append(NL);
        BigDecimal fixo = ZERO, vendas = ZERO, comissao = ZERO;
        for (Pagamento p : pagamentos) { fixo = fixo.add(p.fixo); vendas = vendas.add(p.vendas); comissao = comissao.add(p.comissao); }
        out.append(String.format(Locale.ROOT, "%-21s %8s %8s %8s %13s %9s %15s", "TOTAL COMISSIONADOS", dinheiro(fixo),
                dinheiro(vendas), dinheiro(comissao), dinheiro(somarBruto(pagamentos)), dinheiro(somarDescontos(pagamentos)),
                dinheiro(somarLiquido(pagamentos)))).append(NL).append(NL);
    }

    private static String metodo(Empregado e) {
        return switch (e.getFormaPagamento()) {
            case EM_MAOS -> "Em maos";
            case CORREIOS -> "Correios, " + e.getEndereco();
            case BANCO -> e.getBanco() + ", Ag. " + e.getAgencia() + " CC " + e.getContaCorrente();
        };
    }

    private static BigDecimal somarBruto(List<Pagamento> ps) { return ps.stream().map(p -> p.bruto).reduce(ZERO, BigDecimal::add); }
    private static BigDecimal somarDescontos(List<Pagamento> ps) { return ps.stream().map(p -> p.descontos).reduce(ZERO, BigDecimal::add); }
    private static BigDecimal somarLiquido(List<Pagamento> ps) { return ps.stream().map(p -> p.liquido).reduce(ZERO, BigDecimal::add); }
    private static BigDecimal centavos(BigDecimal valor) { return valor.setScale(2, RoundingMode.DOWN); }
    static String dinheiro(BigDecimal valor) { return centavos(valor).toPlainString().replace('.', ','); }
    static String numero(BigDecimal valor) { return valor.stripTrailingZeros().toPlainString().replace('.', ','); }

    private static class Pagamento {
        final Empregado empregado;
        BigDecimal horasNormais = BigDecimal.ZERO;
        BigDecimal horasExtras = BigDecimal.ZERO;
        BigDecimal fixo = ZERO;
        BigDecimal vendas = ZERO;
        BigDecimal comissao = ZERO;
        BigDecimal bruto = ZERO;
        BigDecimal descontos = ZERO;
        BigDecimal liquido = ZERO;
        Pagamento(Empregado empregado) { this.empregado = empregado; }
    }
}
