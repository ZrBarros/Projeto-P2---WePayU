package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.Empregado.CartaoPonto;
import br.ufal.ic.p2.wepayu.models.Empregado.TaxaServico;
import br.ufal.ic.p2.wepayu.models.Empregado.Venda;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayDeque;
import java.util.Deque;

/** Fachada usada pelo EasyAccept para acessar as funções do sistema. */
public class Facade {
    private static final Path ARQUIVO_ESTADO = Path.of("wepayu-state.bin");
    private static final DateTimeFormatter DATA = new DateTimeFormatterBuilder()
            .appendPattern("d/M/uuuu").toFormatter().withResolverStyle(ResolverStyle.STRICT);

    private SistemaFolha sistema;
    private final Deque<SistemaFolha> desfazer = new ArrayDeque<>();
    private final Deque<SistemaFolha> refazer = new ArrayDeque<>();
    private boolean encerrado;

    public Facade() {
        sistema = carregar();
    }

    public void zerarSistema() throws Exception {
        verificarAberto();
        registrarAlteracao();
        sistema = new SistemaFolha();
    }

    public void encerrarSistema() throws Exception {
        verificarAberto();
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(ARQUIVO_ESTADO))) {
            out.writeObject(sistema);
        } catch (IOException e) {
            throw new Exception("Nao foi possivel salvar o sistema.");
        }
        encerrado = true;
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws Exception {
        verificarAberto();
        validarTexto(nome, "Nome nao pode ser nulo.");
        validarTexto(endereco, "Endereco nao pode ser nulo.");
        Empregado.Tipo tipoConvertido = tipo(tipo);
        if (tipoConvertido == Empregado.Tipo.COMISSIONADO) throw new Exception("Tipo nao aplicavel.");
        BigDecimal salarioConvertido = valorNaoNegativo(salario, "Salario");
        registrarAlteracao();
        String id = sistema.novoId();
        sistema.empregados.put(id, new Empregado(id, nome, endereco, tipoConvertido, salarioConvertido, null));
        return id;
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao) throws Exception {
        verificarAberto();
        validarTexto(nome, "Nome nao pode ser nulo.");
        validarTexto(endereco, "Endereco nao pode ser nulo.");
        Empregado.Tipo tipoConvertido = tipo(tipo);
        if (tipoConvertido != Empregado.Tipo.COMISSIONADO) throw new Exception("Tipo nao aplicavel.");
        BigDecimal salarioConvertido = valorNaoNegativo(salario, "Salario");
        BigDecimal comissaoConvertida = valorNaoNegativo(comissao, "Comissao");
        registrarAlteracao();
        String id = sistema.novoId();
        sistema.empregados.put(id, new Empregado(id, nome, endereco, tipoConvertido, salarioConvertido, comissaoConvertida));
        return id;
    }

    public void removerEmpregado(String emp) throws Exception {
        verificarAberto();
        Empregado empregado = empregado(emp);
        registrarAlteracao();
        sistema.empregados.remove(empregado.getId());
    }

    public int getNumeroDeEmpregados() throws Exception {
        verificarAberto();
        return sistema.empregados.size();
    }

    public String getEmpregadoPorNome(String nome, String indice) throws Exception {
        verificarAberto();
        int procurado;
        try { procurado = Integer.parseInt(indice); }
        catch (NumberFormatException e) { throw new Exception("Nao ha empregado com esse nome."); }
        int atual = 0;
        for (Empregado empregado : sistema.empregados.values()) {
            if (empregado.getNome().contains(nome) && ++atual == procurado) return empregado.getId();
        }
        throw new Exception("Nao ha empregado com esse nome.");
    }

    public String getAtributoEmpregado(String emp, String atributo) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        return switch (atributo) {
            case "nome" -> e.getNome();
            case "endereco" -> e.getEndereco();
            case "tipo" -> nomeTipo(e.getTipo());
            case "salario" -> Formatador.dinheiro(e.getSalario());
            case "comissao" -> {
                if (e.getTipo() != Empregado.Tipo.COMISSIONADO) throw new Exception("Empregado nao eh comissionado.");
                yield Formatador.dinheiro(e.getComissao());
            }
            case "metodoPagamento" -> switch (e.getFormaPagamento()) {
                case EM_MAOS -> "emMaos";
                case CORREIOS -> "correios";
                case BANCO -> "banco";
            };
            case "banco" -> dadoBancario(e, e.getBanco());
            case "agencia" -> dadoBancario(e, e.getAgencia());
            case "contaCorrente" -> dadoBancario(e, e.getContaCorrente());
            case "sindicalizado" -> Boolean.toString(e.isSindicalizado());
            case "idSindicato" -> dadoSindical(e, e.getIdSindicato());
            case "taxaSindical" -> taxaSindical(e);
            default -> throw new Exception("Atributo nao existe.");
        };
    }

    public void alteraEmpregado(String emp, String atributo, String valor) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        switch (atributo) {
            case "nome" -> {
                validarTexto(valor, "Nome nao pode ser nulo."); registrarAlteracao(); e.setNome(valor);
            }
            case "endereco" -> {
                validarTexto(valor, "Endereco nao pode ser nulo."); registrarAlteracao(); e.setEndereco(valor);
            }
            case "tipo" -> {
                Empregado.Tipo novoTipo = tipo(valor);
                if (novoTipo == Empregado.Tipo.COMISSIONADO) throw new Exception("Comissao nao pode ser nula.");
                registrarAlteracao(); e.setTipo(novoTipo); e.setComissao(null);
            }
            case "salario" -> {
                BigDecimal novoSalario = valorNaoNegativo(valor, "Salario"); registrarAlteracao(); e.setSalario(novoSalario);
            }
            case "comissao" -> {
                if (e.getTipo() != Empregado.Tipo.COMISSIONADO) throw new Exception("Empregado nao eh comissionado.");
                BigDecimal novaComissao = valorNaoNegativo(valor, "Comissao"); registrarAlteracao(); e.setComissao(novaComissao);
            }
            case "metodoPagamento" -> {
                if (!valor.equals("emMaos") && !valor.equals("correios")) throw new Exception("Metodo de pagamento invalido.");
                registrarAlteracao();
                if (valor.equals("emMaos")) e.pagarEmMaos(); else e.pagarPelosCorreios();
            }
            case "sindicalizado" -> {
                if (!valor.equals("true") && !valor.equals("false")) throw new Exception("Valor deve ser true ou false.");
                if (valor.equals("true")) throw new Exception("Identificacao do sindicato nao pode ser nula.");
                registrarAlteracao(); e.dessindicalizar();
            }
            default -> throw new Exception("Atributo nao existe.");
        }
    }

    /** Altera o tipo do empregado e o valor relacionado ao novo tipo. */
    public void alteraEmpregado(String emp, String atributo, String valor, String adicional) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (!atributo.equals("tipo")) throw new Exception("Atributo nao existe.");
        Empregado.Tipo novoTipo = tipo(valor);
        BigDecimal numero = novoTipo == Empregado.Tipo.COMISSIONADO
                ? valorNaoNegativo(adicional, "Comissao") : valorNaoNegativo(adicional, "Salario");
        registrarAlteracao();
        e.setTipo(novoTipo);
        if (novoTipo == Empregado.Tipo.COMISSIONADO) e.setComissao(numero);
        else { e.setSalario(numero); e.setComissao(null); }
    }

    /** Atualiza os dados do empregado no sindicato. */
    public void alteraEmpregado(String emp, String atributo, String valor,
                                String idSindicato, String taxaSindical) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (!atributo.equals("sindicalizado")) throw new Exception("Atributo nao existe.");
        if (!valor.equals("true") && !valor.equals("false")) throw new Exception("Valor deve ser true ou false.");
        if (valor.equals("false")) { registrarAlteracao(); e.dessindicalizar(); return; }
        validarTexto(idSindicato, "Identificacao do sindicato nao pode ser nula.");
        BigDecimal taxa = valorNaoNegativoFeminino(taxaSindical, "Taxa sindical");
        for (Empregado outro : sistema.empregados.values()) {
            if (outro != e && outro.isSindicalizado() && idSindicato.equals(outro.getIdSindicato()))
                throw new Exception("Ha outro empregado com esta identificacao de sindicato");
        }
        registrarAlteracao();
        e.sindicalizar(idSindicato, taxa);
    }

    /** Configura o pagamento por depósito bancário. */
    public void alteraEmpregado(String emp, String atributo, String valor,
                                String banco, String agencia, String contaCorrente) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (!atributo.equals("metodoPagamento") || !valor.equals("banco"))
            throw new Exception("Metodo de pagamento invalido.");
        validarTexto(banco, "Banco nao pode ser nulo.");
        validarTexto(agencia, "Agencia nao pode ser nulo.");
        validarTexto(contaCorrente, "Conta corrente nao pode ser nulo.");
        registrarAlteracao();
        e.pagarNoBanco(banco, agencia, contaCorrente);
    }

    public void lancaCartao(String emp, String data, String horas) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (e.getTipo() != Empregado.Tipo.HORISTA) throw new Exception("Empregado nao eh horista.");
        LocalDate dia = data(data, "Data invalida.");
        BigDecimal quantidade = decimal(horas, "Horas devem ser positivas.");
        if (quantidade.compareTo(BigDecimal.ZERO) <= 0) throw new Exception("Horas devem ser positivas.");
        registrarAlteracao();
        e.getCartoes().add(new CartaoPonto(dia, quantidade));
    }

    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (e.getTipo() != Empregado.Tipo.HORISTA) throw new Exception("Empregado nao eh horista.");
        LocalDate[] periodo = periodo(dataInicial, dataFinal);
        BigDecimal total = BigDecimal.ZERO;
        for (CartaoPonto c : e.getCartoes()) if (noPeriodo(c.data, periodo)) total = total.add(c.horas.min(new BigDecimal("8")));
        return Formatador.numero(total);
    }

    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (e.getTipo() != Empregado.Tipo.HORISTA) throw new Exception("Empregado nao eh horista.");
        LocalDate[] periodo = periodo(dataInicial, dataFinal);
        BigDecimal total = BigDecimal.ZERO;
        for (CartaoPonto c : e.getCartoes()) if (noPeriodo(c.data, periodo))
            total = total.add(c.horas.subtract(new BigDecimal("8")).max(BigDecimal.ZERO));
        return Formatador.numero(total);
    }

    public void lancaVenda(String emp, String data, String valor) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (e.getTipo() != Empregado.Tipo.COMISSIONADO) throw new Exception("Empregado nao eh comissionado.");
        LocalDate dia = data(data, "Data invalida.");
        BigDecimal montante = decimal(valor, "Valor deve ser positivo.");
        if (montante.compareTo(BigDecimal.ZERO) <= 0) throw new Exception("Valor deve ser positivo.");
        registrarAlteracao();
        e.getVendas().add(new Venda(dia, montante));
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (e.getTipo() != Empregado.Tipo.COMISSIONADO) throw new Exception("Empregado nao eh comissionado.");
        LocalDate[] periodo = periodo(dataInicial, dataFinal);
        BigDecimal total = BigDecimal.ZERO;
        for (Venda v : e.getVendas()) if (noPeriodo(v.data, periodo)) total = total.add(v.valor);
        return Formatador.dinheiro(total);
    }

    public void lancaTaxaServico(String membro, String data, String valor) throws Exception {
        verificarAberto();
        validarTexto(membro, "Identificacao do membro nao pode ser nula.");
        Empregado e = null;
        for (Empregado candidato : sistema.empregados.values())
            if (candidato.isSindicalizado() && membro.equals(candidato.getIdSindicato())) { e = candidato; break; }
        if (e == null) throw new Exception("Membro nao existe.");
        LocalDate dia = data(data, "Data invalida.");
        BigDecimal montante = decimal(valor, "Valor deve ser positivo.");
        if (montante.compareTo(BigDecimal.ZERO) <= 0) throw new Exception("Valor deve ser positivo.");
        registrarAlteracao();
        e.getTaxasServico().add(new TaxaServico(dia, montante));
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) throws Exception {
        verificarAberto();
        Empregado e = empregado(emp);
        if (!e.isSindicalizado()) throw new Exception("Empregado nao eh sindicalizado.");
        LocalDate[] periodo = periodo(dataInicial, dataFinal);
        BigDecimal total = BigDecimal.ZERO;
        for (TaxaServico t : e.getTaxasServico()) if (noPeriodo(t.data, periodo)) total = total.add(t.valor);
        return Formatador.dinheiro(total);
    }

    public String totalFolha(String data) throws Exception {
        verificarAberto();
        return FolhaPagamento.total(sistema, data(data, "Data invalida."));
    }

    public void rodaFolha(String data, String saida) throws Exception {
        verificarAberto();
        LocalDate dia = data(data, "Data invalida.");
        String conteudo = FolhaPagamento.relatorio(sistema, dia);
        try { Files.writeString(Path.of(saida), conteudo, StandardCharsets.US_ASCII); }
        catch (IOException e) { throw new Exception("Nao foi possivel escrever a folha."); }
        registrarAlteracao();
    }

    public void undo() throws Exception {
        verificarAberto();
        if (desfazer.isEmpty()) throw new Exception("Nao ha comando a desfazer.");
        refazer.push(sistema.copia());
        sistema = desfazer.pop();
    }

    public void redo() throws Exception {
        verificarAberto();
        if (refazer.isEmpty()) throw new Exception("Nao ha comando a refazer.");
        desfazer.push(sistema.copia());
        sistema = refazer.pop();
    }

    private void registrarAlteracao() {
        desfazer.push(sistema.copia());
        refazer.clear();
    }

    private void verificarAberto() throws Exception {
        if (encerrado) throw new Exception("Nao pode dar comandos depois de encerrarSistema.");
    }

    private Empregado empregado(String id) throws Exception {
        validarTexto(id, "Identificacao do empregado nao pode ser nula.");
        Empregado empregado = sistema.empregados.get(id);
        if (empregado == null) throw new Exception("Empregado nao existe.");
        return empregado;
    }

    private static Empregado.Tipo tipo(String tipo) throws Exception {
        return switch (tipo) {
            case "horista" -> Empregado.Tipo.HORISTA;
            case "assalariado" -> Empregado.Tipo.ASSALARIADO;
            case "comissionado" -> Empregado.Tipo.COMISSIONADO;
            default -> throw new Exception("Tipo invalido.");
        };
    }

    private static String nomeTipo(Empregado.Tipo tipo) {
        return switch (tipo) {
            case HORISTA -> "horista";
            case ASSALARIADO -> "assalariado";
            case COMISSIONADO -> "comissionado";
        };
    }

    private static String dadoBancario(Empregado e, String valor) throws Exception {
        if (e.getFormaPagamento() != Empregado.FormaPagamento.BANCO) throw new Exception("Empregado nao recebe em banco.");
        return valor;
    }

    private static String dadoSindical(Empregado e, String valor) throws Exception {
        if (!e.isSindicalizado()) throw new Exception("Empregado nao eh sindicalizado.");
        return valor;
    }

    private static String taxaSindical(Empregado e) throws Exception {
        if (!e.isSindicalizado()) throw new Exception("Empregado nao eh sindicalizado.");
        return Formatador.dinheiro(e.getTaxaSindical());
    }

    private static void validarTexto(String valor, String mensagem) throws Exception {
        if (valor == null || valor.isEmpty()) throw new Exception(mensagem);
    }

    private static BigDecimal valorNaoNegativo(String texto, String nome) throws Exception {
        boolean feminino = nome.equals("Comissao");
        validarTexto(texto, nome + " nao pode ser nul" + (feminino ? "a." : "o."));
        BigDecimal valor;
        try { valor = new BigDecimal(texto.replace(',', '.')); }
        catch (NumberFormatException e) { throw new Exception(nome + " deve ser numeric" + (feminino ? "a." : "o.")); }
        if (valor.compareTo(BigDecimal.ZERO) < 0) throw new Exception(nome + " deve ser nao-negativ" + (feminino ? "a." : "o."));
        return valor;
    }

    private static BigDecimal valorNaoNegativoFeminino(String texto, String nome) throws Exception {
        validarTexto(texto, nome + " nao pode ser nula.");
        BigDecimal valor;
        try { valor = new BigDecimal(texto.replace(',', '.')); }
        catch (NumberFormatException e) { throw new Exception(nome + " deve ser numerica."); }
        if (valor.compareTo(BigDecimal.ZERO) < 0) throw new Exception(nome + " deve ser nao-negativa.");
        return valor;
    }

    private static BigDecimal decimal(String texto, String mensagem) throws Exception {
        try { return new BigDecimal(texto.replace(',', '.')); }
        catch (RuntimeException e) { throw new Exception(mensagem); }
    }

    private static LocalDate data(String texto, String mensagem) throws Exception {
        try { return LocalDate.parse(texto, DATA); }
        catch (DateTimeParseException e) { throw new Exception(mensagem); }
    }

    private static LocalDate[] periodo(String inicial, String fim) throws Exception {
        LocalDate i = data(inicial, "Data inicial invalida.");
        LocalDate f = data(fim, "Data final invalida.");
        if (i.isAfter(f)) throw new Exception("Data inicial nao pode ser posterior aa data final.");
        return new LocalDate[]{i, f};
    }

    private static boolean noPeriodo(LocalDate data, LocalDate[] periodo) {
        return !data.isBefore(periodo[0]) && data.isBefore(periodo[1]);
    }

    private static SistemaFolha carregar() {
        if (!Files.exists(ARQUIVO_ESTADO)) return new SistemaFolha();
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(ARQUIVO_ESTADO))) {
            return (SistemaFolha) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return new SistemaFolha();
        }
    }
}
