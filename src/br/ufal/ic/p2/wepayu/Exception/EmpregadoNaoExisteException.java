package br.ufal.ic.p2.wepayu.Exception;

/**Exception para empregados inexistentes*/
public class EmpregadoNaoExisteException extends Exception{
    private static final long serialVersionUID = 1L;

    public EmpregadoNaoExisteException(){
        super("Empregado nao existe.");
    }
}
