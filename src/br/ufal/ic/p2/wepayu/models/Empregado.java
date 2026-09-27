package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Guarda os dados do empregado e os lançamentos ligados a ele. */
public class Empregado implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo { HORISTA, ASSALARIADO, COMISSIONADO }
    public enum FormaPagamento { EM_MAOS, CORREIOS, BANCO }

    public static class CartaoPonto implements Serializable {
        private static final long serialVersionUID = 1L;
        public final LocalDate data;
        public final BigDecimal horas;
        public CartaoPonto(LocalDate data, BigDecimal horas) { this.data = data; this.horas = horas; }
    }

    public static class Venda implements Serializable {
        private static final long serialVersionUID = 1L;
        public final LocalDate data;
        public final BigDecimal valor;
        public Venda(LocalDate data, BigDecimal valor) { this.data = data; this.valor = valor; }
    }

    public static class TaxaServico implements Serializable {
        private static final long serialVersionUID = 1L;
        public final LocalDate data;
        public final BigDecimal valor;
        public TaxaServico(LocalDate data, BigDecimal valor) { this.data = data; this.valor = valor; }
    }

    private final String id;
    private String nome;
    private String endereco;
    private Tipo tipo;
    private BigDecimal salario;
    private BigDecimal comissao;

    private FormaPagamento formaPagamento = FormaPagamento.EM_MAOS;
    private String banco;
    private String agencia;
    private String contaCorrente;

    private boolean sindicalizado;
    private String idSindicato;
    private BigDecimal taxaSindical;

    private final ArrayList<CartaoPonto> cartoes = new ArrayList<>();
    private final ArrayList<Venda> vendas = new ArrayList<>();
    private final ArrayList<TaxaServico> taxasServico = new ArrayList<>();

    public Empregado(String id, String nome, String endereco, Tipo tipo,
                     BigDecimal salario, BigDecimal comissao) {
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
        this.tipo = tipo;
        this.salario = salario;
        this.comissao = comissao;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public BigDecimal getSalario() { return salario; }
    public void setSalario(BigDecimal salario) { this.salario = salario; }
    public BigDecimal getComissao() { return comissao; }
    public void setComissao(BigDecimal comissao) { this.comissao = comissao; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public String getBanco() { return banco; }
    public String getAgencia() { return agencia; }
    public String getContaCorrente() { return contaCorrente; }
    public boolean isSindicalizado() { return sindicalizado; }
    public String getIdSindicato() { return idSindicato; }
    public BigDecimal getTaxaSindical() { return taxaSindical; }
    public List<CartaoPonto> getCartoes() { return cartoes; }
    public List<Venda> getVendas() { return vendas; }
    public List<TaxaServico> getTaxasServico() { return taxasServico; }

    public void pagarEmMaos() {
        formaPagamento = FormaPagamento.EM_MAOS;
        banco = agencia = contaCorrente = null;
    }

    public void pagarPelosCorreios() {
        formaPagamento = FormaPagamento.CORREIOS;
        banco = agencia = contaCorrente = null;
    }

    public void pagarNoBanco(String banco, String agencia, String contaCorrente) {
        formaPagamento = FormaPagamento.BANCO;
        this.banco = banco;
        this.agencia = agencia;
        this.contaCorrente = contaCorrente;
    }

    public void sindicalizar(String idSindicato, BigDecimal taxaSindical) {
        sindicalizado = true;
        this.idSindicato = idSindicato;
        this.taxaSindical = taxaSindical;
    }

    public void dessindicalizar() {
        sindicalizado = false;
        idSindicato = null;
        taxaSindical = null;
        taxasServico.clear();
    }
}
